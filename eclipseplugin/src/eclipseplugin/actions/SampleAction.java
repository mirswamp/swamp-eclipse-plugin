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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
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
import eclipseplugin.dialogs.AuthenticationDialog;
import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.SelectionDialog;

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.wisc.cs.swamp.exceptions.IncompatibleException;
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
 * @see IWorkbenchWindowActionDelegate
 */
public class SampleAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	private MessageConsoleStream out;
	private SwampApiWrapper api;

	private static String SWAMP_FAMILY = "SWAMP_FAMILY";
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

	private void runBackgroundJob(SelectionDialog sd, ConfigDialog cd, String prjUUID) {
		Job job = new Job("SWAMP Assessment Submission") {
			
			@Override
			public boolean belongsTo(Object family) {
				return family == SWAMP_FAMILY;
			}
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				ClasspathHandler classpathHandler = null;
				if (cd.needsGeneratedBuildFile()) {
					out.println("Status: Generating build file");
					classpathHandler = generateBuildFiles(cd.getProject());
					if (classpathHandler == null) {
						// TODO Handle this error better
						return Status.CANCEL_STATUS;
					}
				}
				out.println("Status: Packaging Project");
				PackageInfo pkgInfo = packageProject(cd.getPkgName(), cd.getPkgVersion(), cd.getBuildSys(), cd.getBuildDir(), cd.getBuildFile(), cd.getBuildTarget());
				
				out.println("Status: Uploading package to SWAMP");
				String pkgVersUUID = uploadPackage(pkgInfo.getParentPath(), prjUUID, pkgInfo.getArchiveFilename(), cd.createNewPackage());
				
				if (classpathHandler != null) {
					classpathHandler.revertClasspath(ResourcesPlugin.getWorkspace().getRoot(), new HashSet<ClasspathHandler>());
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
				out.println("Status: Submitting assessments");
				for (String platformUUID : sd.getPlatformUUIDs()) {
					for (String toolUUID : sd.getToolUUIDs()) {
						submitAssessment(pkgVersUUID, toolUUID, prjUUID, platformUUID);
					}
				}
				
				return Status.OK_STATUS;
			}
		};
		//job.setRule(new MutexRule());
		job.setUser(true);
		job.schedule();
	}
	
	private ClasspathHandler generateBuildFiles(IProject proj) {
		ClasspathHandler classpathHandler = null;
		// Generating Buildfile
		IJavaProject javaProj = JavaCore.create(proj);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		String rootPath = root.getLocation().toString();
		classpathHandler = new ClasspathHandler(null, javaProj, rootPath);// cd.getPkgPath()); // TODO replace this w/ workspace path
		System.out.println(classpathHandler.getProjectName());
		
		if (classpathHandler.hasCycles()) {
			out.println("Error: There are cyclic dependencies in this project. Please remove all cycles before resubmitting.");
			System.err.println("Huge error. Cyclic dependencies!");
			classpathHandler = null;
			return null;
		}
		BuildfileGenerator.generateBuildFile(classpathHandler);
		System.out.println("Build file generated");
		return classpathHandler;
	}
	
	private PackageInfo packageProject(String packageName, String packageVersion, String buildSystem, String buildDir, String buildFile, String buildTarget) {
		// Zipping and generating package.conf
		Date date = new Date();
		String timestamp = date.toString();
		//String path = cd.getPkgPath();
		String path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/package";
		String filename = timestamp + "-" + packageName + ".zip";
		String filenameNoSpaces = filename.replace(" ", "-").replace(":", "").toLowerCase(); // PackageVersionHandler mangles the name for some reason if there are colons or uppercase letters
		System.out.println("Package Name: " + packageName);
		System.out.println("Path: " + path);
		System.out.println("Filename: " + filenameNoSpaces);
		// output name should be some combination of pkg name, version, timestamp, extension (.zip)
		
		PackageInfo pkg = new PackageInfo(path, filenameNoSpaces, packageName); // pass in path and output zip file name
		
		pkg.setPkgShortName(packageName);
		pkg.setVersion(packageVersion);
		pkg.setBuildSys(buildSystem);
		pkg.setBuildDir(buildDir);
		pkg.setBuildFile(buildFile);
		pkg.setBuildTarget(buildTarget);
		
		pkg.writePkgConfFile();

		return pkg;
	}

	private String uploadPackage(String parentDir, String prjUUID, String filename, boolean newPackage) {
		// Upload package
		System.out.println("Uploading package");
		System.out.println("Package-conf directory: " + parentDir + "/package.conf");
		System.out.println("Archive directory: " + parentDir + "/" + filename);
		System.out.println("Project UUID: " + prjUUID);
		String pkgVersUUID = null;
		try {
			pkgVersUUID = api.uploadPackage(parentDir + "/package.conf", parentDir + "/" + filename, prjUUID, newPackage);
		} catch (InvalidIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		if (pkgVersUUID == null) {
			// TODO handle error here
			System.err.println("Error in uploading package.");
		}	
		return pkgVersUUID;
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
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
		
		sd = new SelectionDialog(window.getShell(), this.api);
		cd = new ConfigDialog(window.getShell(), api);
		try {
			if (!api.restoreSession(SESSION_STRING)) {
			// Add authentication dialog here
				AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), this.api, this.out);
				ad.create();
				if (ad.open() != Window.OK) {
					return;
				}
				api.saveSession(SESSION_STRING);
			}
		} catch (Exception e) {
			AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), this.api, this.out);
			ad.create();
			if (ad.open() != Window.OK) {
				return;
			}
			api.saveSession(SESSION_STRING);
		}
		boolean returnCode = FileSerializer.deserialize(serializedConfigFilepath, sd, cd);
		sd.create();
		if (sd.open() != Window.OK) {
			// TODO Handle error
		}
		else {
			String prjUUID = sd.getProjectUUID();
			cd.setSwampProject(prjUUID);
			cd.create();
			System.out.println("Made it to config dialog");
			if (cd.open() != Window.OK) {
				// TODO Handle error
			}
			else {
				boolean retCode = FileSerializer.serialize(serializedConfigFilepath, sd, cd, prjUUID);
				runBackgroundJob(sd, cd, prjUUID);
			}
			out.println("Status: Plugin completed executing");
		}
			
	}
	
	private void submitAssessment(String pkgUUID, String toolUUID, String prjUUID, String pltUUID) {
		// Submit assessment
		System.out.println("Package UUID: " + pkgUUID);
		System.out.println("Tool UUID: " + toolUUID);
		System.out.println("Project UUID: " + prjUUID);
		System.out.println("Platform UUID: " + pltUUID);
		
		api.printAllPackages(prjUUID, true);
		
		String toolName = api.getTool(toolUUID).getName();
		PackageVersion pkg = api.getPackage(pkgUUID, prjUUID);
		assert(pkg != null);
		PackageThing pkgThing = pkg.getPackageThing();
		assert (pkgThing != null);
		String pkgName = pkgThing.getName();
		String platformName = api.getPlatform(pltUUID).getName();

		String assessUUID = null;
		try {
			assessUUID = api.runAssessment(pkgUUID, toolUUID, prjUUID, pltUUID);
		} catch (IncompatibleException e) {
			// This means that the platform and tool were incompatible
			// This should never happen given that we check the platform-tool pairs before this
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidIdentifierException e) {
			// This means that some UUID was invalid
			// This really should never happen
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
