package org.continuousassurance.swamp.eclipse.handlers;

import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public class LoginHandler extends AbstractHandler {
	SwampSubmitter ss;
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Executed Login Handler");
		ss = new SwampSubmitter(HandlerUtil.getActiveWorkbenchWindow(event));
		ss.logIntoSwamp();
		return null;
	}

}
