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

import org.continuousassurance.swamp.eclipse.ResultsUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ClearResultsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// (1) Clear all markers
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		if (wsRoot != null) {
			try {
				wsRoot.deleteMarkers("eclipseplugin.highseverity", true, IResource.DEPTH_INFINITE);
				wsRoot.deleteMarkers("eclipseplugin.medseverity", true, IResource.DEPTH_INFINITE);
				wsRoot.deleteMarkers("eclipseplugin.lowseverity", true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// (2) Clear all results
		File f = new File(ResultsUtils.getTopLevelResultsDirectory());
		if (f.exists()) {
			f.delete();
		}
		return null;
	}

}
