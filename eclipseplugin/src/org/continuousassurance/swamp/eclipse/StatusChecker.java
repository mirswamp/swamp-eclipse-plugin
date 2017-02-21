package org.continuousassurance.swamp.eclipse;

import org.continuousassurance.swamp.eclipse.exceptions.ResultsRetrievalException;
import org.continuousassurance.swamp.eclipse.exceptions.UserNotLoggedInException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class StatusChecker extends Job {
	private boolean running = true;
	
	public StatusChecker() {
		super("Status Checker");
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		System.out.println("\n\nRunning status checker background job\n\n");
		schedule(30000); // runs once every 30s
		try {
			ResultsRetriever.retrieveResults();
		}
		catch (UserNotLoggedInException e) {
			System.err.println("User not logged into SWAMP");
		}
		catch (ResultsRetrievalException e) {
			System.err.println("Error in results retrieval");
		}
		return Status.OK_STATUS;
	}
	
	@Override
	public boolean shouldSchedule() {
		return running;
	}
	
	public void stop() {
		running = false;
	}
	
	public boolean isRunning() {
		return running;
	}

}