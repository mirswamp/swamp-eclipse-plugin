package org.continuousassurance.swamp.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.continuousassurance.swamp.eclipse.exceptions.ResultsRetrievalException;
import org.continuousassurance.swamp.eclipse.exceptions.UserNotLoggedInException;
import org.continuousassurance.swamp.eclipse.ui.StatusView;
import org.continuousassurance.swamp.eclipse.ui.SwampPerspective;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import edu.uiuc.ncsa.swamp.api.AssessmentRecord;
import edu.wisc.cs.swamp.SwampApiWrapper;

public class ResultsRetriever {
	
	private static final String USER_NOT_LOGGED_IN_STATUS = "User not logged in";
	
	private static final String FINISHED = "Finished";
	
	private static final String FINISHED_WITH_ERRORS = "Finished with Errors";
	
	private static final Set<String> assessmentsToDelete = new HashSet<>();
	
	private static final Lock lock = new ReentrantLock();

	public static synchronized void retrieveResults() throws UserNotLoggedInException, ResultsRetrievalException {
		// (1) If user's not logged in, quit out immediately
		if (!Activator.getLoggedIn()) {
			throw new UserNotLoggedInException();
		}
	
		SwampApiWrapper api = null;
		try {
			api = new SwampApiWrapper(SwampApiWrapper.HostType.CUSTOM, Activator.getLastHostname());
			if (!api.restoreSession()) {
				throw new UserNotLoggedInException();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserNotLoggedInException();
		}

		List<String> statuses = new ArrayList<>();
		
		addUnfinishedFileStatuses(statuses, api);
		addFinishedFileStatuses(statuses);
		
		System.out.println("Updating status view");
		updateStatusView(statuses);
		return;
	}
	
	public static void retrieveResultsNotLoggedIn() {
		
	}
	
	public static void addAssessmentToDelete(String assessUUID) {
		lock.lock();
		assessmentsToDelete.add(assessUUID);
		lock.unlock();
	}
	
	public static void addUnfinishedFileStatuses(List<String> statuses, SwampApiWrapper api) throws ResultsRetrievalException {
		File oldFile = new File(Activator.getUnfinishedAssessmentsPath());
		if (!oldFile.exists()) {
			return;
		}
		Scanner sc = null;
		try {
			sc = new Scanner(oldFile);
		} catch (FileNotFoundException e) {
			return;
		}
		
		File tmp = new File(Activator.getUnfinishedAssessmentsPath() + ".tmp");
		if (tmp.exists()) {
			tmp.delete();
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(tmp, true);
		} catch (IOException e) {
			e.printStackTrace();
			sc.close();
			throw new ResultsRetrievalException();
		}
		
	
		System.out.println("Now to parse stuff from unfinished file");
		while (sc.hasNextLine()) {
			String assessmentDetails = sc.nextLine();
			System.out.println("New line: " + assessmentDetails);
			String newStatusStr = updateStatus(writer, api, assessmentDetails);
			System.out.println("New status string: " + newStatusStr);
			if (newStatusStr != null) { // null indicates this status is no longer in the unfinished file, don't want to double count, as we will go over finished file next
				statuses.add(newStatusStr);
			}
		}
		sc.close();
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		oldFile.delete();
		
		tmp.renameTo(oldFile);
	}
	
	private static void addFinishedFileStatuses(List<String> statuses) throws ResultsRetrievalException {
		System.out.println("Now reading stuff from finished file");
		File f = new File(Activator.getFinishedAssessmentsPath());
		if (!f.exists()) {
			return;
		}
		Scanner sc = null;
		try {
			sc = new Scanner(f);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			return;
		}
		
		File tmp = new File(Activator.getFinishedAssessmentsPath() + ".tmp");
		if (tmp.exists()) {
			tmp.delete();
		}
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(tmp, true);
		} catch (IOException e) {
			e.printStackTrace();
			sc.close();
			throw new ResultsRetrievalException();
		}
		
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
				assessmentsToDelete.remove(assessUUID);
			}
			lock.unlock();
		}
		sc.close();
		try {
			writer.close();
		} catch (IOException e) {
		}
		f.delete();
		tmp.renameTo(f);
	}
	
	private static void updateStatusView(List<String> statuses) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					System.out.println("Actually attempting to update status view");
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					if (page != null) {
						StatusView view = (StatusView) page.findView(SwampPerspective.STATUS_VIEW_DESCRIPTOR);
						if (view != null) {
							System.out.println("Actually updating status view!");
							view.clearTable();
							view.addRowsToStatusTable(statuses);
						}
					}
				}
			});
	}
	
	private static void writeToFinishedFile(String newDetailInfo) throws IOException {
		File f = new File(Activator.getFinishedAssessmentsPath());
		if (!f.exists()) {
			f.createNewFile();
		}
		FileWriter finishedWriter = new FileWriter(f, true);
		finishedWriter.write(newDetailInfo);
		finishedWriter.close();
	}

	private static String updateStatus(FileWriter unfinishedWriter, SwampApiWrapper api, String serializedAssessmentDetails) {
		AssessmentDetails ad = new AssessmentDetails(serializedAssessmentDetails);
		if (api == null) {
			ad.updateStatus(USER_NOT_LOGGED_IN_STATUS);
			return ad.serialize();
		}
		String prjUUID = ad.getProjectUUID();
		String assessUUID = ad.getAssessUUID();
		lock.lock();
		if (assessmentsToDelete.contains(assessUUID)) {
			assessmentsToDelete.remove(assessUUID);
			return null;
		}
		lock.unlock();
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
				unfinishedWriter.write(newDetailInfo);
			}
			else if (FINISHED_WITH_ERRORS.equals(status)) { // Note: This will break if the labels are changed, so MIR shouldn't do that
				System.out.println("Finished with errors!");
				newDetailInfo = ad.serialize();
				writeToFinishedFile(newDetailInfo);
				return null;
			}
			else {
				newDetailInfo = ad.serialize();
				unfinishedWriter.write(newDetailInfo);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return newDetailInfo;
		
	}
}