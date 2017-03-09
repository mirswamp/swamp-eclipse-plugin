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

import java.io.File;

import org.continuousassurance.swamp.eclipse.Controller;
import org.continuousassurance.swamp.eclipse.ResultsUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This is a handler for deleting all existing results
 * @author reid-jr
 *
 */
public class ClearResultsHandler extends AbstractHandler {

	@Override
	/**
	 * Clears all result markers, deletes results files, and refreshes the workspace
	 * @param event click event
	 * @return null
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// (1) Clear all markers
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			Controller.resetFileMarkers(wb.getActiveWorkbenchWindow());
		}
		
		// (2) Clear all results
		File f = new File(ResultsUtils.getTopLevelResultsDirectory());
		if (f.exists()) {
			f.delete();
		}
		Controller.refreshWorkspace();
		return null;
	}
}
