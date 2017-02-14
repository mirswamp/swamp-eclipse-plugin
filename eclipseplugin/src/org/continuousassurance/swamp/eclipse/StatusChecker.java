package org.continuousassurance.swamp.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.continuousassurance.swamp.eclipse.ui.StatusView;
import org.continuousassurance.swamp.eclipse.ui.SwampPerspective;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import edu.uiuc.ncsa.swamp.api.AssessmentRecord;
import edu.wisc.cs.swamp.SwampApiWrapper;
import org.eclipse.swt.widgets.Display;

public class StatusChecker extends Job {
	private boolean running = true;
	
	public StatusChecker() {
		super("Status Checker");
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		System.out.println("\n\nRunning status checker background job\n\n");
		schedule(30000); // runs once every 30s
		// Here's where we do the work
		// (1) If user's not logged in, quit out immediately
		if (!Activator.getLoggedIn()) {
			return Status.OK_STATUS;
		}
		// (2) Read through unfinished file
		File oldFile = new File(Activator.getUnfinishedAssessmentsPath());
		Scanner sc = null;
		try {
			sc = new Scanner(oldFile);
		} catch (FileNotFoundException e) {
			return Status.OK_STATUS;
		}
		SwampApiWrapper api = null;
		try {
			api = new SwampApiWrapper(SwampApiWrapper.HostType.CUSTOM, Activator.getLastHostname());
			if (!api.restoreSession()) {
				sc.close();
				return Status.OK_STATUS;
			}
		} catch (Exception e) {
			e.printStackTrace();
			sc.close();
			return Status.OK_STATUS;
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
			return Status.OK_STATUS;
		}
		

		System.out.println("Now to parse stuff from unfinished file");
		List<String> statuses = new ArrayList<>();
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
		
		addFinishedFileStatuses(statuses);
		
		System.out.println("Updating status view");
		updateStatusView(statuses);
		return Status.OK_STATUS;
	}
	
	private void addFinishedFileStatuses(List<String> statuses) {
		File f = new File(Activator.getFinishedAssessmentsPath());
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
	
	private void updateStatusView(List<String> statuses) {
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
	
	private String updateStatus(FileWriter unfinishedWriter, SwampApiWrapper api, String prjUUID, String assessUUID, 
			String serializedAssessmentDetails) {
		System.out.println("Querying for assessment record with project UUID " + prjUUID + " and assessment UUID " + assessUUID);
		AssessmentRecord rec = api.getAssessmentRecord(prjUUID, assessUUID);
		String status = rec.getStatus();
		System.out.println("Status: " + status);
		String newDetailInfo = AssessmentDetails.updateStatus(serializedAssessmentDetails, status);
		FileWriter finishedWriter = null;
		try {
			if ("Complete".equals(status)) {
				File f = new File(Activator.getFinishedAssessmentsPath());
				if (!f.exists()) {
					f.createNewFile();
				}
				finishedWriter = new FileWriter(f, true);
				finishedWriter.write(newDetailInfo);
				finishedWriter.close();
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
	
	@Override
	public boolean shouldSchedule() {
		return running;
	}
	
	public void stop() {
		running = false;
	}

}
