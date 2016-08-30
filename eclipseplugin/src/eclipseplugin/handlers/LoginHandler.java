package eclipseplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import eclipseplugin.SwampSubmitter;

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
