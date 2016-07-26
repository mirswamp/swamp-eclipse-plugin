/* Malcolm Reid Jr.
 * 06/07/2016
 * UW SWAMP
 * SampleAction.java
 * Plug-in code to launch primary dialogs when activated
 */

package eclipseplugin.actions;


import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import eclipseplugin.BuildfileGenerator;
import eclipseplugin.ClasspathHandler;
import eclipseplugin.FileSerializer;
import eclipseplugin.MutexRule;
import eclipseplugin.PackageInfo;
import eclipseplugin.SwampSubmitter;
import eclipseplugin.dialogs.AuthenticationDialog;
import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.SelectionDialog;

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.wisc.cs.swamp.exceptions.InvalidIdentifierException;

import java.util.Date;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

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
		submitter.launchBackgroundAssessment();
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
		Menu menu = new Menu(parent);
		MenuItem item1 = new MenuItem(menu, SWT.PUSH, 0);
		item1.setText("&Configure Assessment Submission");
		item1.addListener(SWT.Selection, e -> submitter.launch());//System.out.println("Selected Config. Time to launch config.")); // lambda expression
		MenuItem item2 = new MenuItem(menu, SWT.PUSH, 1);
		item2.setText("&Resubmit Previous Assessment");
		item2.addListener(SWT.Selection, e -> submitter.launchBackgroundAssessment());//System.out.println("Selected resubmit. Just resubmit in background."));
		return menu;
	}
}
