package org.continuousassurance.swamp.eclipse.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

public class HandlerUtilityMethods {

	public static IProject getActiveProject(IWorkbenchWindow window) {
IEditorInput input = getActiveEditorInput(window);
		if (!(input instanceof IFileEditorInput)) {
			return null;
		}
		IResource resource = (IResource)getActiveFile(window);
		if (resource == null) {
			return null;
		}
		IProject project = resource.getProject();
		if (project == null) {
			return null;
		}
		return project;
	}
	
	public static IEditorInput getActiveEditorInput(IWorkbenchWindow window) {
		IWorkbenchPage workbenchPage = window.getActivePage();
		if (workbenchPage == null) {
			// TODO Add some MessageDialog to say we were unable to get the project - are you sure you have an editor open?
			return null;
		}
		IEditorPart editorPart = workbenchPage.getActiveEditor();
		if (editorPart == null) {
			// TODO Add some MessageDialog to say we were unable to get the project - are you sure you have an editor open?
			return null;
		}
		
		/* Code adapted from Eclipse wiki (https://wiki.eclipse.org/FAQ_How_do_I_access_the_active_project%3F) */
		IEditorInput input = editorPart.getEditorInput();
		return input;
	}
	
	public static IFile getActiveFile(IWorkbenchWindow window) {
		IEditorInput input = getActiveEditorInput(window);
		if (!(input instanceof IFileEditorInput)) {
			return null;
		}
		IFile file = ((IFileEditorInput) input).getFile();
		return file;
	}
}
