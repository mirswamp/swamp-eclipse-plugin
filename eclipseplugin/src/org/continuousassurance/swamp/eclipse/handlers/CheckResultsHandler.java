/*
 * Copyright 2016-2017 Malcolm Reid Jr.
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
package org.continuousassurance.swamp.eclipse.handlers;

import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.ResultsRetriever;
import org.continuousassurance.swamp.eclipse.StatusChecker;
import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.continuousassurance.swamp.eclipse.exceptions.ResultsRetrievalException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Handler for checking for results
 * @author reid-jr
 *
 */
public class CheckResultsHandler extends AbstractHandler {

	@Override
	/**
	 * Checks for results
	 * @param event click event
	 * @return null
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		StatusChecker sc = Activator.getStatusChecker();
		if (sc != null && sc.isRunning()) {
			return null;
		}
		if (!Activator.getLoggedIn()) {
			IWorkbench wb = PlatformUI.getWorkbench();
			if (wb != null) {
				IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
				if (window != null) {
					SwampSubmitter ss = new SwampSubmitter(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
					if (ss.authenticateUser()) {
						try {
							ResultsRetriever.retrieveResults();
						} catch (ResultsRetrievalException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return null;
	}
	
}
