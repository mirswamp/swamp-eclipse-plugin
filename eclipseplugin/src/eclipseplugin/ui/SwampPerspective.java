package eclipseplugin.ui;

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

import eclipseplugin.handlers.HandlerUtilityMethods;

public class SwampPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		defineActions(layout);
		defineLayout(layout);
	}
	
	public void defineActions(IPageLayout layout) {
		
	}
	
	public void defineLayout(IPageLayout layout) {
		
		MessageDialog md = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Test Message Dialog", null, "Please select a project to view results from", 
				MessageDialog.QUESTION, null, 0);
		md.open();
		
		String editorArea = layout.getEditorArea();
		annotateEditor();
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		// TODO: Add custom view for list of bugs
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.3f, editorArea);
		bottom.addView("eclipseplugin.ui.views.tableview");
		// TODO: Add custom view for detailed look at a single bug
		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.2f, editorArea);
		right.addView("eclipseplugin.ui.views.detailview");
		//right.addView(IPageLayout.ID_TASK_LIST);
	}
	
	public void annotateEditor() {
		
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		//IEditorInput editor = HandlerUtilityMethods.getActiveEditorInput(window);
		
		
		// TODO Use actual input
		IFile file = HandlerUtilityMethods.getActiveFile(window);
		try {
			createMarkerForResource(file);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	/* The following method is adapted from http://www.eclipse.org/articles/Article-Mark%20My%20Words/mark-my-words.html */
	public void createMarkerForResource(IFile resource) throws CoreException {
		//IMarker marker = resource.createMarker("eclipseplugin.swampmarker");
		IMarker marker = resource.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.MESSAGE, "Invalid use of keyword");
		marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		marker.setAttribute(IMarker.LINE_NUMBER, 6);
	}
	
}
