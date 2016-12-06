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

package org.continuousassurance.swamp.eclipse.ui;

import org.continuousassurance.swamp.eclipse.handlers.HandlerUtilityMethods;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class SwampPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		defineActions(layout);
		defineLayout(layout);
	}
	
	public void defineActions(IPageLayout layout) {
		
	}
	
	public void defineLayout(IPageLayout layout) {
		
		String editorArea = layout.getEditorArea();
		annotateEditor();
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		// TODO: Add custom view for list of bugs
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.3f, editorArea);
		bottom.addView("org.continuousassurance.swamp.eclipse.ui.views.tableview");
		// TODO: Add custom view for detailed look at a single bug
		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.2f, editorArea);
		right.addView("org.continuousassurance.swamp.eclipse.ui.views.detailview");
		//right.addView(IPageLayout.ID_TASK_LIST);
	}
	
	public void annotateEditor() {
		
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		
		IPartService service = window.getPartService();
		service.addPartListener(new FileChangeListener());
		//IEditorInput editor = HandlerUtilityMethods.getActiveEditorInput(window);
	
		// TODO Use actual input
		IFile file = HandlerUtilityMethods.getActiveFile(window);
		try {
			for (int i = 0; i < 10; i++) {
				if ((i+1 % 3) == 0)
					createMarkerForResource(file, i+1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	/* The following method is adapted from http://www.eclipse.org/articles/Article-Mark%20My%20Words/mark-my-words.html */
	public void createMarkerForResource(IFile resource, int lineNum) throws CoreException {
		//IMarker marker = resource.createMarker("org.continuousassurance.swamp.eclipse.swampmarker");
		IMarker marker = resource.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.MESSAGE, "Invalid use of keyword");
		marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		marker.setAttribute(IMarker.LINE_NUMBER, lineNum);
	}
	
	public class FileChangeListener implements IPartListener2 {

		@Override
		public void partActivated(IWorkbenchPartReference part) {
			System.out.println("Part Activated");
			printPartInfo(part);
			// TODO Auto-generated method stub
			
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void partClosed(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void partHidden(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference part) {
			System.out.println("Part Input Changed");
			// TODO Auto-generated method stub
			
		}

		@Override
		public void partOpened(IWorkbenchPartReference part) {
			System.out.println("Part Opened");
			printPartInfo(part);
			
		}

		@Override
		public void partVisible(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
			
		}
		
		private void printPartInfo(IWorkbenchPartReference part) {
			System.out.println("Part Opened");
			System.out.println("Part class: " + part.getClass());
			System.out.println("Part ID: " + part.getId());
			System.out.println("Part title: " + part.getTitle());
		}
		
	}
	
}
