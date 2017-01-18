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
import org.eclipse.ui.PlatformUI;

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
