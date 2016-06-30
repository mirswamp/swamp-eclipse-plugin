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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import eclipseplugin.ClasspathHandler;
import eclipseplugin.FileSerializer;
import eclipseplugin.PackageInfo;
import eclipseplugin.dialogs.AuthenticationDialog;
import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.SelectionDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.wisc.cs.swamp.SwampApiWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.ant.internal.ui.datatransfer.BuildFileCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
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
	private MessageConsoleStream out;
	/**
	 * The constructor.
	 */
	private static String SESSION_STRING = ".SESSION";
	public SampleAction() {
	}
	
	private MessageConsoleStream initializeConsole(String consoleName) {
		/* View http://wiki.eclipse.org/FAQ_How_do_I_write_to_the_console_from_a_plug-in%3F for more details */
		/* Adapted from the above link */
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMgr = plugin.getConsoleManager();
		MessageConsole console = new MessageConsole(consoleName, null);
		conMgr.addConsoles(new IConsole[]{console});
		IWorkbenchPage page = window.getActivePage();
		try {
			IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			view.display(console);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return console.newMessageStream();
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		SwampApiWrapper api;
		SelectionDialog sd;
		ConfigDialog cd;
		String serializedConfigFilepath;
		
		serializedConfigFilepath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/.swampconfig";
		System.out.println(serializedConfigFilepath);
		out = initializeConsole("SWAMP Plugin");
		out.println("Status: Launched SWAMP plugin");
		try {
			api = new SwampApiWrapper(SwampApiWrapper.HostType.DEVELOPMENT);
		} catch (Exception e) {
			out.println("Error: Unable to initialize SWAMP API.");
			e.printStackTrace();
			return;
		}
		
		sd = new SelectionDialog(window.getShell(), api);
		cd = new ConfigDialog(window.getShell());
		if (!api.restoreSession(SESSION_STRING)) {
		// Add authentication dialog here
			AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), api, out);
			ad.create();
			if (ad.open() != Window.OK) {
				return;
			}
			api.saveSession(SESSION_STRING);
		}
		else {
			// deserialize from file
			boolean returnCode = FileSerializer.deserialize(serializedConfigFilepath, sd, cd);
			if (!returnCode) {
				out.println("Warning: Unable to completely restore previous assessment information");
			}
		}
		sd.create();
		if (sd.open() != Window.OK) {
			// TODO Handle error - These aren't actually errors - this is just user canceling out
		}
		else {
			cd.create();
			if (cd.open() != Window.OK) {
				// TODO Handle error - These aren't actually errors - this is just user canceling out
			}
			else {
				boolean autoGenBuild = cd.needsGeneratedBuildFile();
				ClasspathHandler classpathHandler = null;
				if (autoGenBuild) {
					// Generating Buildfile
					out.println("Status: Generating build file");
					IProject proj = cd.getProject();
					IJavaProject javaProj = JavaCore.create(proj);
					classpathHandler = new ClasspathHandler(javaProj, cd.getPkgPath());
					Set<IJavaProject> projects = new HashSet<IJavaProject>();
					// projects = classpathHandler.getProjects();
					// TODO Add console logging for a problem in generating buildfile (e.g. cycles)
					projects.add(javaProj);
					BuildFileCreator.setOptions("build.xml", "jUnit", true, false);
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
				out.println("Status: Generating .zip archive");
				Date date = new Date();
				String timestamp = date.toString();
				String pkgName = cd.getPkgName();
				String path = cd.getPkgPath();
				String filename = timestamp + "-" + pkgName + ".zip";
				String filenameNoSpaces = filename.replace(" ", "-").replace(":", "").toLowerCase(); // PackageVersionHandler mangles the name for some reason if there are colons or uppercase letters
				System.out.println("Package Name: " + pkgName);
				System.out.println("Path: " + path);
				System.out.println("Filename: " + filenameNoSpaces);
				// output name should be some combination of pkg name, version, timestamp, extension (.zip)
				
				PackageInfo pkg = new PackageInfo(path, filenameNoSpaces); // pass in path and output zip file name
				pkg.setPkgShortName(pkgName);
				pkg.setVersion(cd.getPkgVersion());
				pkg.setBuildSys(cd.getBuildSys());
				pkg.setBuildDir(cd.getBuildDir());
				pkg.setBuildFile(cd.getBuildFile());
				pkg.setBuildTarget(cd.getBuildTarget());
				
				boolean retCode = FileSerializer.serialize(serializedConfigFilepath, sd, cd); 
				
				pkg.writePkgConfFile();

				String parentDir = pkg.getParentPath();
				// Upload package
				String prjUUID = sd.getProjectUUID();
				System.out.println("Uploading package");
				System.out.println("Package-conf directory: " + parentDir + "/package.conf");
				System.out.println("Archive directory: " + parentDir + "/" + filenameNoSpaces);
				System.out.println("Project UUID: " + prjUUID);
				String pkgUUID = api.uploadPackage(parentDir + "/package.conf", parentDir + "/" + filenameNoSpaces, prjUUID, true); 
				if (pkgUUID == null) {
					// TODO handle error here
					out.println("Error: There was an error in uploading your package to the SWAMP");
					System.err.println("Error in uploading package.");
				}
				
				// Deletion code - uncomment for release
				/*
				pkg.deleteFiles();
				if (autoGenBuild) {
					System.out.println("Auto-generated build file at " + path + "/build.xml");
					File f = new File(path + "/build.xml");
					if (f != null) {
						if (!f.delete()) {
							System.err.println("Unable to delete auto-generated build file");
						}
					}
				}
				*/
				
				if (classpathHandler != null) {
					classpathHandler.revertClasspath();
				}
				
				for (String platformUUID : sd.getPlatformUUIDs()) {
					for (String toolUUID : sd.getToolUUIDs()) {
						submitAssessment(api, pkgUUID, toolUUID, prjUUID, platformUUID);
					}
				}
				
			}

		}
		
		out.println("Status: Plugin completed executing");
		/*MessageDialog.openInformation(
			window.getShell(),
			"Success",
			"Here's the information about your submission");*/
	}
	
	private void submitAssessment(SwampApiWrapper api, String pkgUUID, String toolUUID, String prjUUID, String pltUUID) {
		// Submit assessment
		System.out.println("Package UUID: " + pkgUUID);
		System.out.println("Tool UUID: " + toolUUID);
		System.out.println("Project UUID: " + prjUUID);
		System.out.println("Platform UUID: " + pltUUID);
		String toolName = api.getTool(toolUUID).getName();
		String pkgName = api.getPackage(pkgUUID).getPackageThing().getName();
		String platformName = api.getPlatform(pltUUID).getName();

		String assessUUID = api.runAssessment(pkgUUID, toolUUID, prjUUID, pltUUID);
		if (assessUUID == null) {
			out.println("Error: There was an error in uploading assessment for package {" + pkgName + "} with tool {" + toolName + "} on platform {" + platformName + "}");
			// TODO handle error here
			System.err.println("Error in running assessment.");
		}
		else {
			out.println("Status: Successfully submitted assessment with tool {" + toolName + "} on platform {" + platformName +"}");
		}
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
