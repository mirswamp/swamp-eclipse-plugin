/* Malcolm Reid Jr.
 * 06/07/2016
 * UW SWAMP
 * SampleAction.java
 * Plug-in code to launch primary dialogs when activated
 */

package eclipseplugin.actions;


import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import eclipseplugin.SwampSubmitter;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowPulldownDelegate
 */
public class LaunchAction implements IWorkbenchWindowPulldownDelegate {
	private IWorkbenchWindow window;
	private SwampSubmitter submitter;
	
	public LaunchAction() {
		window = null;
		submitter = null;
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		submitter.launchBackgroundAssessment(HandlerUtils.getActiveProjectLocation(window));
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
		submitter = new SwampSubmitter(this.window);
	}

	@Override
	public Menu getMenu(Control parent) {
		System.out.println("Active project location: " + HandlerUtils.getActiveProjectLocation(window));
		Menu menu = new Menu(parent);
		int index = 0;
		MenuItem configLaunch = makeMenuItem(menu, "&Configure Assessment Submission", index++);
		configLaunch.addListener(SWT.Selection, e -> submitter.launch(HandlerUtils.getActiveProjectLocation(window)));
		MenuItem resubmit = makeMenuItem(menu, "&Resubmit Previous Assessment", index++);
		resubmit.addListener(SWT.Selection, e -> submitter.launchBackgroundAssessment(HandlerUtils.getActiveProjectLocation(window)));
		MenuItem logIn = makeMenuItem(menu, "Log &In", index++);
		MenuItem logOut = makeMenuItem(menu, "Log &Out", index++);
		boolean loggedIn = submitter.loggedIntoSwamp();
		System.out.println("User is logged in: " + loggedIn);
		if (loggedIn) {
			logOut.addListener(SWT.Selection, e -> submitter.logOutOfSwamp());
			logIn.setEnabled(false);
		}
		else {
			logIn.addListener(SWT.Selection, e -> submitter.logIntoSwamp());
			logOut.setEnabled(false);
		}
		return menu;
	}
	
	private MenuItem makeMenuItem(Menu menu, String label, int index) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
		menuItem.setText(label);
		return menuItem;
	}
	

	
	
	
}
