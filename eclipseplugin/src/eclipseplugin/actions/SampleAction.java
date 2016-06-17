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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import eclipseplugin.PackageInfo;
import eclipseplugin.dialogs.AuthenticationDialog;
import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.NewProjectDialog;
import eclipseplugin.dialogs.SelectionDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.wisc.cs.swamp.SwampApiWrapper;

import java.util.Date;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class SampleAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public SampleAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		PackageInfo pkg;
		SwampApiWrapper api;
		try {
			api = new SwampApiWrapper(SwampApiWrapper.HostType.DEVELOPMENT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		// Add authentication dialog here
		AuthenticationDialog d = new AuthenticationDialog(window.getShell());
		d.create();
		d.setSwampApiWrapper(api);
		if (d.open() != Window.OK) {
			// TODO Handle error
		}
		else {
			//HandlerFactory h = d.getHandlerFactory();
			SelectionDialog s = new SelectionDialog(window.getShell());
			//s.setHandlerFactory(h);
			s.setSwampApiWrapper(api);
			s.create();
			if (s.open() != Window.OK) {
				// TODO Handle error
			}
			else {
				// TODO Get the project or create a new project here
				//Project project = ParseCommandLine.getProjectFromIndex(s.getProjectIndex());
				String prjUUID = s.getProjectUUID();
				String toolUUID = s.getToolUUID();
				String pltUUID = s.getPlatformUUID();
				if (prjUUID == null) {
					// TODO Make NewProjectDialog
					NewProjectDialog n = new NewProjectDialog(window.getShell());
					if (n.open() != Window.OK) {
						// TODO Handle error
					}
					prjUUID = n.getProjectUUID();
				}
				ConfigDialog c = new ConfigDialog(window.getShell());
				//c.setHandlerFactory(h);
				c.create();
				System.out.println("Made it to config dialog");
				if (c.open() != Window.OK) {
					// TODO Handle error
				}
				else {
					Date date = new Date();
					String timestamp = date.toString();
					String pkgName = c.getPkgName();
					String path = c.getPkgPath();
					String filename = timestamp + "-" + pkgName + ".zip";
					String filenameNoSpaces = filename.replace(" ", "-");
					System.out.println("Package Name: " + pkgName);
					System.out.println("Path: " + path);
					System.out.println("Filename: " + filenameNoSpaces);
					// output name should be some combination of pkg name, version, timestamp, extension (.zip)
					
					pkg = new PackageInfo(path, filenameNoSpaces); // pass in path and output zip file name
					pkg.setPkgShortName(pkgName);
					pkg.setVersion(c.getPkgVersion());
					pkg.setBuildSys(c.getBuildSys());
					pkg.setBuildTarget(c.getBuildTarget());
					
					// short name comes from config dialog
					// version comes from config dialog
					// archive stuff comes from pkg info
					// package-dir should be extracted from path
					// package build sys comes from config dialog
					// package build target comes from config dialog
					/* writer.println("package-short-name=");
					writer.println("package-version=");
					writer.println("package-archive=");
					writer.println("package-archive-md5=");
					writer.println("package-archive-sha512=");
					writer.println("package-dir=");
					writer.println("package-language=Java");
					writer.println("build-sys=");
					writer.println("build-target=");
					*/
					pkg.writePkgConfFile();
				}
			}

		// Here's where the business logic goes
		}
		
		// Trying to do automated menu selection
		Menu menu = window.getShell().getMenu();
		if (menu == null) {
			System.out.println("Empty menu");
		}
		else {
			MenuItem[] menuItems = menu.getItems();
			for (MenuItem m : menuItems) {
				System.out.println(m);
			}
		}
		
		
		/*MessageDialog.openInformation(
			window.getShell(),
			"Success",
			"Here's the information about your submission");*/
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
	}
}