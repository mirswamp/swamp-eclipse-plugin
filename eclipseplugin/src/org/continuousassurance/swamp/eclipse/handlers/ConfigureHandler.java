package org.continuousassurance.swamp.eclipse.handlers;

import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConfigureHandler extends AbstractHandler {

	private IWorkbenchWindow window;
	private SwampSubmitter submitter;

	public ConfigureHandler() {
		super();
		window = null;
		submitter = null;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		window = HandlerUtil.getActiveWorkbenchWindow(event);
		submitter = new SwampSubmitter(window);
		submitter.launch(HandlerUtilityMethods.getActiveProject(window));
		return null;
	}

}
