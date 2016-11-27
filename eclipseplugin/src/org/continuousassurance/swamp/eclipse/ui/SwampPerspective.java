package org.continuousassurance.swamp.eclipse.ui;

import org.continuousassurance.swamp.eclipse.handlers.HandlerUtilityMethods;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbench;
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
		IEditorInput editor = HandlerUtilityMethods.getActiveEditorInput(window);

		
		
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
	
}
