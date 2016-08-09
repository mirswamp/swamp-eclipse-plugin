package eclipseplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eclipseplugin.SwampSubmitter;

public class LaunchHandler extends AbstractHandler {
	
	private IWorkbenchWindow window;
	private SwampSubmitter submitter;

	private static final String MSG_ID = "eclipseplugin.internal.msg";
	private static final String CONFIGURE_MESSAGE = "Configure";
	private static final String RESUBMIT_MESSAGE = "Resubmit";
	private static final String LOGIN_MESSAGE = "Login";
	private static final String LOGOUT_MESSAGE = "Logout";
	
	public LaunchHandler() {
		super();
		window = null;
		submitter = null;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		window = HandlerUtil.getActiveWorkbenchWindow(event);
		submitter = new SwampSubmitter(window);
		String message = event.getParameter(MSG_ID);
		System.out.println("Message: " + message);
		if (message == null) {
			// resubmit
			submitter.launchBackgroundAssessment(getActiveProjectLocation());
			return null;
		}
		switch(message) {
		
		case CONFIGURE_MESSAGE:
			// call configure code
			submitter.launch(getActiveProjectLocation());
			break;
		case RESUBMIT_MESSAGE:
			submitter.launchBackgroundAssessment(getActiveProjectLocation());
			break;
		case LOGIN_MESSAGE:
			// log in
			submitter.logIntoSwamp();
			break;
		case LOGOUT_MESSAGE:
			// log out
			submitter.logOutOfSwamp();
			break;
		default:
			submitter.launchBackgroundAssessment(getActiveProjectLocation());
		}
			
		// TODO Add switch on message and add default case, which does Resubmit
		return null;
	}

	private String getActiveProjectLocation() {
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
