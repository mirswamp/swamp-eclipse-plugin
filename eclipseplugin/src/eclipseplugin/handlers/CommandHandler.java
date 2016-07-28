package eclipseplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import eclipseplugin.SwampSubmitter;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// gets workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		// gets selection service
		ISelectionService service = window.getSelectionService();
		
		// gets structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();
		System.out.println("Selection: " + structured);
		System.out.println("Type of selection: " + structured.getFirstElement().getClass());
		
		if (!(structured.getFirstElement() instanceof IJavaProject)) {
			System.out.println("Somehow the selection is not a project");
			return null;
		}
		
		IJavaProject project = (IJavaProject)structured.getFirstElement();
		System.out.println("Project is open? " + project.isOpen());
		if (!project.isOpen()) {
			// TODO Try to open project here
			return null;
		}
		
		// TODO (1) Make this project-specific
		SwampSubmitter ss = new SwampSubmitter(window);
		String configDir = project.getProject().getLocation().toOSString();
		
		ss.launchBackgroundAssessment(configDir);
		
		return null;
	}
}
