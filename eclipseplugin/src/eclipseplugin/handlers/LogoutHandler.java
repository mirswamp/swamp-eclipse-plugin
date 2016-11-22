package eclipseplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import eclipseplugin.SwampSubmitter;

public class LogoutHandler extends AbstractHandler {
	SwampSubmitter ss;
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Executed Logout Handler");
		ss = new SwampSubmitter(HandlerUtil.getActiveWorkbenchWindow(event));
		// TODO: Save the results that we're currently waiting on
		ss.logOutOfSwamp();	
		return null;
	}

}
