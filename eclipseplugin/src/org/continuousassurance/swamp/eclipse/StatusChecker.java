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
		return this.getState() == Job.RUNNING;
	}

}