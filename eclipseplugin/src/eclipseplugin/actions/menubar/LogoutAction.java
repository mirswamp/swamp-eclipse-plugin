package eclipseplugin.actions.menubar;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import eclipseplugin.SwampSubmitter;

public class LogoutAction implements IWorkbenchWindowActionDelegate {
	
	IWorkbenchWindow window = null;
	SwampSubmitter ss = null;
	
	public void init(IAction action) {
		
	}
	
	public void run(IAction action) {
		ss.logOutOfSwamp();
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		ss = new SwampSubmitter(window);
	}
	
}
