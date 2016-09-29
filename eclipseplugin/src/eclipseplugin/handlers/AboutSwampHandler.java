package eclipseplugin.handlers;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import eclipseplugin.dialogs.AboutSWAMPDialog;

public class AboutSwampHandler extends AbstractHandler {

		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			// gets workbench window
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			if (window != null) {
				AboutSWAMPDialog asd = new AboutSWAMPDialog(window.getShell());
				asd.open();
			}
			return null;
		}
}
