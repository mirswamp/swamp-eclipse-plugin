/*
 * Copyright 2016 Malcolm Reid Jr.
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

package eclipseplugin.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

public class HandlerUtils {

	public static String getActiveProjectLocation(IWorkbenchWindow window) {
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
		if (!(input instanceof IFileEditorInput)) {
			return null;
		}
		IResource resource = (IResource)((IFileEditorInput)input).getFile();
		if (resource == null) {
			return null;
		}
		IProject project = resource.getProject();
		if (project == null) {
			return null;
		}
		return project.getLocation().toOSString();
	}
}
