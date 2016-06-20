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
import eclipseplugin.dialogs.SelectionDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.wisc.cs.swamp.SwampApiWrapper;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.ant.internal.ui.datatransfer.BuildFileCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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
				
				ConfigDialog c = new ConfigDialog(window.getShell());
				//c.setHandlerFactory(h);
				c.create();
				System.out.println("Made it to config dialog");
				if (c.open() != Window.OK) {
					// TODO Handle error
				}
				else {
					if (c.needsGeneratedBuildFile()) {
						// Generating Buildfile
						IProject proj = c.getProject();
						BuildFileCreator.setOptions("build.xml", "jUnit", true, true);
						IJavaProject project = JavaCore.create(proj);
						Set<IJavaProject> projects = new HashSet<IJavaProject>();
						projects.add(project);
						try {
							BuildFileCreator.createBuildFiles(projects, window.getShell(), null);
						} catch (JavaModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (TransformerConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (TransformerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// Zipping and generating package.conf
					Date date = new Date();
					String timestamp = date.toString();
					String pkgName = c.getPkgName();
					String path = c.getPkgPath();
					String filename = timestamp + "-" + pkgName + ".zip";
					String filenameNoSpaces = filename.replace(" ", "-");
					//String filenameNoSpaces = "dummyfilename.zip";
					System.out.println("Package Name: " + pkgName);
					System.out.println("Path: " + path);
					System.out.println("Filename: " + filenameNoSpaces);
					// output name should be some combination of pkg name, version, timestamp, extension (.zip)
					
					PackageInfo pkg = new PackageInfo(path, filenameNoSpaces); // pass in path and output zip file name
					pkg.setPkgShortName(pkgName);
					pkg.setVersion(c.getPkgVersion());
					pkg.setBuildSys(c.getBuildSys());
					pkg.setBuildTarget(c.getBuildTarget());
					
					pkg.writePkgConfFile();

					String parentDir = pkg.getParentPath();
					// Upload package
					System.out.println("Uploading package");
					System.out.println("Package-conf directory: " + parentDir + "/package.conf");
					System.out.println("Archive directory: " + parentDir + "/" + filenameNoSpaces);
					String pkgUUID = api.uploadPackage(parentDir + "/package.conf", parentDir + "/" + filenameNoSpaces, prjUUID); 
					
					// Submit assessment
					System.out.println("Package UUID: " + pkgUUID);
					System.out.println("Tool UUID: " + toolUUID);
					System.out.println("Project UUID: " + prjUUID);
					System.out.println("Platform UUID: " + pltUUID);
					api.runAssessment(pkgUUID, toolUUID, prjUUID, pltUUID);
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
