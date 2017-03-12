/*
 * Copyright 2017 Malcolm Reid Jr.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.continuousassurance.swamp.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.continuousassurance.swamp.eclipse.exceptions.ResultsRetrievalException;
import org.continuousassurance.swamp.eclipse.exceptions.UserNotLoggedInException;
import org.eclipse.swt.widgets.Display;
import edu.uiuc.ncsa.swamp.api.AssessmentRecord;
import edu.wisc.cs.swamp.SwampApiWrapper;

/**
 * This class retrieves assessment statuses from the SWAMP, downloads results
 * as they become available, and updates the local assessment statuses
 * @author reid-jr
 *
 */
public class ResultsRetriever {
	
	/**
	 * Status to be displayed when plug-in is unable to contact SWAMP
	 * (presumably because user is not logged in) 
	 */
	private static final String USER_NOT_LOGGED_IN_STATUS = "User not logged in";
	/**
	 * String indicating that assessment is finished (there's no API in place 
	 * for figuring out whether an assessment has finished without testing 
	 * whether the assessment status equals this string)
	 */
	private static final String FINISHED = "Finished";
	
	/**
	 * String indicating that assessment has finished but with errors
	 */
	private static final String FINISHED_WITH_ERRORS = "Finished with Errors";
	
	/**
	 * Set of assessments to be "deleted/removed"
	 */
	private static final Set<String> assessmentsToDelete = new HashSet<>();

	/**
	 * Lock for assessmentsToDelete
	 */
	private static final Lock lock = new ReentrantLock();

	/**
	 * Retrieves results for assessments (unfinished and finished)
	 * @throws UserNotLoggedInException thrown if user is not logged in
	 * @throws ResultsRetrievalException thrown if there is some error in retrieving results
	 */
	public static synchronized void retrieveResults() throws ResultsRetrievalException {
		// (1) If user's not logged in, set SwampApiWrapper to be null
		SwampApiWrapper api = null;
		if (Activator.getLoggedIn()) {
			try {
				api = new SwampApiWrapper(SwampApiWrapper.HostType.CUSTOM, Activator.getLastHostname());
				if (!api.restoreSession()) {
					api = null;
				}
			} catch (Exception e) {
				api = null;
			}
		}

		List<String> statuses = new ArrayList<>();
		
		boolean refreshNeeded = addUnfinishedFileStatuses(statuses, api);
		refreshNeeded = (addFinishedFileStatuses(statuses) || refreshNeeded);
		if (refreshNeeded) {
			System.out.println("Refresh Needed!");
			refreshWS();
		}
		else {
			System.out.println("No refresh needed!");
		}
		
		System.out.println("Updating status view");
		updateStatusView(statuses);
		return;
	}
	
	/**
	 * Adds an assessment to remove (i.e. to stop checking status for and to 
	 * stop displaying in StatusView)
	 * @param assessUUID assessment UUID of the assessment to remove
	 */
	public static void addAssessmentToDelete(String assessUUID) {
		lock.lock();
		assessmentsToDelete.add(assessUUID);
		lock.unlock();
	}
	
	/**
	 * Checks and updates status of unfinished assessments
	 * @param statuses list of unfinished file statuses (new statuses are added
	 * to this)
	 * @param api SwampApiWrapper reference. Null if user is not logged in
	 * @return true if the workspace needs to be refreshed
	 * @throws ResultsRetrievalException thrown if there is an error in
	 * retrieving results
	 */
	public static boolean addUnfinishedFileStatuses(List<String> statuses, SwampApiWrapper api) throws ResultsRetrievalException {
		File oldFile = new File(Activator.getUnfinishedAssessmentsPath());
		if (!oldFile.exists()) {
			return false;
		}
		Scanner sc = null;
		try {
			sc = new Scanner(oldFile, Activator.ENCODING);
		} catch (FileNotFoundException e) {
			return false;
		}
		
		File tmp = new File(Activator.getUnfinishedAssessmentsPath() + ".tmp");
		if (tmp.exists()) {
			tmp.delete();
		}
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(tmp, true), Activator.ENCODING);
		} catch (IOException e) {
			e.printStackTrace();
			sc.close();
			throw new ResultsRetrievalException();
		}
		
		System.out.println("Now to parse stuff from unfinished file");
		boolean refreshNeeded = false;
		while (sc.hasNextLine()) {
			String assessmentDetails = sc.nextLine();
			System.out.println("New line: " + assessmentDetails);
			String newStatusStr = updateStatus(writer, api, assessmentDetails);
			System.out.println("New status string: " + newStatusStr);
			if (newStatusStr != null) { // null indicates this status is no longer in the unfinished file, don't want to double count, as we will go over finished file next.
				// also possible that null assessment was removed by the user
				statuses.add(newStatusStr);
			}
			else {
				refreshNeeded = true;
			}
		}
		sc.close();
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (api != null) {
			//oldFile.delete();
			//tmp.renameTo(oldFile);
			Path dst = oldFile.toPath();
			Path src = tmp.toPath();
			try {
				Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				// TODO: See if we can implement some kind of rollback here
				e.printStackTrace();
			}
		}

		return refreshNeeded;
	}
	
	/**
	 * Adds statuses of finished assessments to the list of statuses
	 * @param statuses list of assessment statuses
	 * @return true if workspace should be refreshed
	 * @throws ResultsRetrievalException thrown if there is some error retrieving results
	 */
	private static boolean addFinishedFileStatuses(List<String> statuses) throws ResultsRetrievalException {
		System.out.println("Now reading stuff from finished file");
		File f = new File(Activator.getFinishedAssessmentsPath());
		if (!f.exists()) {
			return false;
		}
		Scanner sc = null;
		try {
			sc = new Scanner(f);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			return false;
		}
		
		File tmp = new File(Activator.getFinishedAssessmentsPath() + ".tmp");
		if (tmp.exists()) {
			tmp.delete();
		}
		
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(tmp, true), Activator.ENCODING);
		} catch (IOException e) {
			e.printStackTrace();
			sc.close();
			throw new ResultsRetrievalException();
		}
		boolean refreshNeeded = false;
		
		while (sc.hasNext()) {
			String status = sc.nextLine();
			AssessmentDetails ad = new AssessmentDetails(status);
			String assessUUID = ad.getAssessUUID();
			lock.lock();
			if (!assessmentsToDelete.contains(assessUUID)) {
				statuses.add(status);
				try {
				writer.write(status + "\n");
				} catch (IOException e) {
				}
			}
			else {
				refreshNeeded = true;
				assessmentsToDelete.remove(assessUUID);
			}
			lock.unlock();
		}
		sc.close();
		try {
			writer.close();
		} catch (IOException e) {
		}
		//f.delete();
		//tmp.renameTo(f);
		Path dst = f.toPath();
		Path src = tmp.toPath();
		try {
			Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			// TODO: See if we can implement some kind of rollback here
			e.printStackTrace();
		}
		
		return refreshNeeded;
	}
	
	/**
	 * Updates status view with list of statuses
	 * @param statuses list of (unfinished and finished) assessment statuses
	 */
	private static void updateStatusView(List<String> statuses) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					System.out.println("Actually attempting to update status view");
					Controller.updateStatusView(statuses);
				}
			});
	}
	
	/**
	 * Refreshes workspace
	 */
	private static void refreshWS() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Actually attempting to refresh workspace");
				Controller.refreshWorkspace();
			}
		});
	}
	
	/**
	 * Utility method to append a single AssessmentDetail object to the
	 * file of finished assessments
	 * @param newDetailInfo serialized AssessmentDetail object
	 * @throws IOException
	 */
	private static void writeToFinishedFile(String newDetailInfo) throws IOException {
		File f = new File(Activator.getFinishedAssessmentsPath());
		if (!f.exists()) {
			f.createNewFile();
		}
		OutputStreamWriter finishedWriter = new OutputStreamWriter(new FileOutputStream(f, true), Activator.ENCODING);
		finishedWriter.write(newDetailInfo);
		finishedWriter.close();
	}

	/**
	 * This method updates the status of a single unfinished assessment
	 * @param writer OutputStreamWriter that appends to end of unfinished assessments file
	 * @param api SwampApiWrapper reference
	 * @param serializedAssessmentDetails information about this assessment
	 * @return new serialized AssessmentDetails object with updated status (if
	 * unfinished). null if finished
	 */
	private static String updateStatus(OutputStreamWriter writer, SwampApiWrapper api, String serializedAssessmentDetails) {
		AssessmentDetails ad = new AssessmentDetails(serializedAssessmentDetails);
		// TODO: Move this below assessmentsToDelete stuff
		
		String prjUUID = ad.getProjectUUID();
		String assessUUID = ad.getAssessUUID();
		lock.lock();
		if (assessmentsToDelete.contains(assessUUID)) {
			assessmentsToDelete.remove(assessUUID);
			lock.unlock();
			return null;
		}
		lock.unlock();
		if (api == null) {
			ad.updateStatus(USER_NOT_LOGGED_IN_STATUS);
			return ad.serialize();
		}
		
		AssessmentRecord rec = api.getAssessmentRecord(prjUUID, assessUUID);
		String status = rec.getStatus();
		System.out.println("Status: " + status);
		ad.updateStatus(status);
		String newDetailInfo = "";
		try {
			if (FINISHED.equals(status)) { // Note: This will break if the labels are changed, so MIR shouldn't do that
				System.out.println("Finished with no errors!");
				System.out.println("Bug count: " + rec.getWeaknessCount());
				ad.setBugCount(rec.getWeaknessCount());
				String filepath = ad.getFilepath();
				File f = new File(filepath);
				if (f.exists()) {
					f.delete();
				}
				newDetailInfo = ad.serialize();
				if (api.getAssessmentResults(prjUUID, rec.getAssessmentResultUUID(), filepath)) {
					System.out.println("Saved results to filepath: " + filepath);
					System.out.println("Here's the details I just wrote out: " + newDetailInfo);
					writeToFinishedFile(newDetailInfo);
					return null;
				} // TODO: Catch file not found exception
				writer.write(newDetailInfo);
			}
			else if (FINISHED_WITH_ERRORS.equals(status)) { // Note: This will break if the labels are changed, so MIR shouldn't do that
				System.out.println("Finished with errors!");
				newDetailInfo = ad.serialize();
				writeToFinishedFile(newDetailInfo);
				return null;
			}
			else {
				newDetailInfo = ad.serialize();
				writer.write(newDetailInfo);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return newDetailInfo;
	}
}