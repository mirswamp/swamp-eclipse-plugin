package org.continuousassurance.swamp.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

	public static void retrieveResults() throws UserNotLoggedInException, ResultsRetrievalException {
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
			String[] parts = assessmentDetails.split(AssessmentDetails.DELIMITER);
			String prjUUID = parts[0];
			String assessUUID = parts[1];
			String newStatusStr = updateStatus(writer, api, prjUUID, assessUUID, assessmentDetails);
			System.out.println("New status string: " + newStatusStr);
			if (newStatusStr != null) { // null indicates this status is no longer in the unfinished file
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
	
	private static void addFinishedFileStatuses(List<String> statuses) {
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
		while (sc.hasNext()) {
			String status = sc.nextLine();
			statuses.add(status);
		}
		sc.close();
	}
	
	private static void updateStatusView(List<String> statuses) {
		if (statuses.size() > 0) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					System.out.println("Actually attempting to update status view");
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					StatusView view = (StatusView) page.findView(SwampPerspective.STATUS_VIEW_DESCRIPTOR);
					if (view != null) {
						System.out.println("Actually updating status view!");
						view.clearTable();
						view.addRowsToStatusTable(statuses);
					}
				}
			});
		}
		else {
			System.out.println("No statuses");
		}
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

	private static String updateStatus(FileWriter unfinishedWriter, SwampApiWrapper api, String prjUUID, String assessUUID, 
			String serializedAssessmentDetails) {
		System.out.println("Querying for assessment record with project UUID " + prjUUID + " and assessment UUID " + assessUUID);
		if (api == null) {
			return AssessmentDetails.updateStatus(serializedAssessmentDetails, USER_NOT_LOGGED_IN_STATUS);
		}
		AssessmentRecord rec = api.getAssessmentRecord(prjUUID, assessUUID);
		String status = rec.getStatus();
		System.out.println("Status: " + status);
		String newDetailInfo = AssessmentDetails.updateStatus(serializedAssessmentDetails, status);
		try {
			if ("Finished".equals(status)) { // TODO: Fix this when Vamshi modifies the AssessmentRecord.java API. This is NOT how we should be doing it
				System.out.println("Finished with no errors!");
				newDetailInfo = AssessmentDetails.addBugCount(serializedAssessmentDetails, Integer.toString(rec.getWeaknessCount()));
				String filepath = AssessmentDetails.getFilepath(serializedAssessmentDetails);
				File f = new File(filepath);
				if (f.exists()) {
					f.delete();
				}
				api.getAssessmentResults(prjUUID, assessUUID, filepath);
				writeToFinishedFile(newDetailInfo);
				return null;
			}
			else if ("Finished with Errors".equals(status)) { // I've left these hardcoded and without a var as a reminder that this needs to be fixed ASAP. This will break as soon as they change the status labels
				System.out.println("Finished with errors!");
				writeToFinishedFile(newDetailInfo);
				return null;
			}
			else {
				unfinishedWriter.write(newDetailInfo);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return newDetailInfo;
		
	}
}