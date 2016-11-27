package org.continuousassurance.swamp.eclipse.handlers;


import org.continuousassurance.swamp.eclipse.dialogs.AboutSWAMPDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

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
