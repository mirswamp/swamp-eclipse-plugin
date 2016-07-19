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

import eclipseplugin.BuildfileGenerator;
import eclipseplugin.ClasspathHandler;
import eclipseplugin.FileSerializer;
import eclipseplugin.PackageInfo;
import eclipseplugin.dialogs.AuthenticationDialog;
import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.SelectionDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.wisc.cs.swamp.exceptions.InvalidIdentifierException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.ant.internal.ui.datatransfer.BuildFileCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
	private SwampApiWrapper api;
	/**
	 * The constructor.
	 */
	private static String SESSION_STRING = ".SESSION";
	public SampleAction() {
	}
	
	private void runBackgroundJob(SelectionDialog sd, ConfigDialog cd, String configFilepath, String prjUUID) {
		Job job = new Job("SWAMP Assessment Submission") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				ClasspathHandler classpathHandler = null;
				if (cd.needsGeneratedBuildFile()) {
					classpathHandler = generateBuildFiles(cd.getProject());
					if (classpathHandler == null) {
						// TODO Handle this error better
						return Status.CANCEL_STATUS;
					}
				}
				
				PackageInfo pkg = packageProject(cd.getPkgName(), cd.getPkgVersion(), cd.getBuildSys(), cd.getBuildDir(), cd.getBuildFile(), cd.getBuildTarget());
				boolean retCode = FileSerializer.serialize(configFilepath, sd, cd); 
				String pkgUUID = uploadPackage(pkg.getParentPath(), prjUUID, pkg.getArchiveFilename());
				
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
				
				for (String platformUUID : sd.getPlatformUUIDs()) {
					for (String toolUUID : sd.getToolUUIDs()) {
						submitAssessment(pkgUUID, toolUUID, prjUUID, platformUUID);
					}
				}
				
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
	private ClasspathHandler generateBuildFiles(IProject proj) {
		ClasspathHandler classpathHandler = null;
		// Generating Buildfile
		IJavaProject javaProj = JavaCore.create(proj);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		String rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		classpathHandler = new ClasspathHandler(null, javaProj, rootPath);// cd.getPkgPath()); // TODO replace this w/ workspace path
		System.out.println(classpathHandler.getProjectName());
		if (classpathHandler.hasCycles()) {
			System.err.println("Huge error. Cyclic dependencies!");
			classpathHandler = null;
			// TODO Add message to console - out.println("Error: Project has cyclic dependencies");
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

	private String uploadPackage(String parentDir, String prjUUID, String filename) {
		// Upload package
		System.out.println("Uploading package");
		System.out.println("Package-conf directory: " + parentDir + "/package.conf");
		System.out.println("Archive directory: " + parentDir + "/" + filename);
		System.out.println("Project UUID: " + prjUUID);
		String pkgUUID = null;
		try {
			pkgUUID = api.uploadPackage(parentDir + "/package.conf", parentDir + "/" + filename, prjUUID, true);
		} catch (InvalidIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		if (pkgUUID == null) {
			// TODO handle error here
			System.err.println("Error in uploading package.");
		}	
		return pkgUUID;
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
		
		try {
			api = new SwampApiWrapper(SwampApiWrapper.HostType.DEVELOPMENT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		sd = new SelectionDialog(window.getShell());
		sd.setSwampApiWrapper(api);
		cd = new ConfigDialog(window.getShell(), api);
		try {
		if (!api.restoreSession(SESSION_STRING)) {
		// Add authentication dialog here
			AuthenticationDialog ad = new AuthenticationDialog(window.getShell());
			ad.create();
			ad.setSwampApiWrapper(api);
			if (ad.open() != Window.OK) {
				return;
			}
			api.saveSession(SESSION_STRING);
		}
		else {
			// deserialize from file
			// TODO Bring this back in the merge
			//boolean returnCode = FileSerializer.deserialize(serializedConfigFilepath, sd, cd);
		}
		} catch (Exception e) {
			AuthenticationDialog ad = new AuthenticationDialog(window.getShell());
			ad.create();
			ad.setSwampApiWrapper(api);
			if (ad.open() != Window.OK) {
				return;
			}
			api.saveSession(SESSION_STRING);
		}
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
				runBackgroundJob(sd, cd, serializedConfigFilepath, prjUUID);
			}

		}
			
		/*MessageDialog.openInformation(
			window.getShell(),
			"Success",
			"Here's the information about your submission");*/
	}
	
	private void submitAssessment(String pkgUUID, String toolUUID, String prjUUID, String pltUUID) {
		// Submit assessment
		System.out.println("Package UUID: " + pkgUUID);
		System.out.println("Tool UUID: " + toolUUID);
		System.out.println("Project UUID: " + prjUUID);
		System.out.println("Platform UUID: " + pltUUID);
		String assessUUID = api.runAssessment(pkgUUID, toolUUID, prjUUID, pltUUID);
		if (assessUUID == null) {
			// TODO handle error here
			System.err.println("Error in running assessment.");
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
