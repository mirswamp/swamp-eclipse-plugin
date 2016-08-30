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

public class ResubmitHandler extends AbstractHandler {
	
	private IWorkbenchWindow window;
	private SwampSubmitter submitter;

	public ResubmitHandler() {
		super();
		window = null;
		submitter = null;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		window = HandlerUtil.getActiveWorkbenchWindow(event);
		submitter = new SwampSubmitter(window);
		submitter.launchBackgroundAssessment(HandlerUtilityMethods.getActiveProjectLocation(window));
		return null;
	}
	
}
