/*
 * Copyright 2016 Malcolm Reid Jr.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eclipseplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Version;

import eclipseplugin.dialogs.AuthenticationDialog;
import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.PlatformDialog;
import eclipseplugin.dialogs.ToolDialog;
import eclipseplugin.exceptions.CyclicDependenciesException;
import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;
import edu.uiuc.ncsa.swamp.api.Platform;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.wisc.cs.swamp.exceptions.IncompatibleAssessmentTupleException;
import edu.wisc.cs.swamp.exceptions.InvalidIdentifierException;
import edu.wisc.cs.swamp.exceptions.SessionExpiredException;
import edu.wisc.cs.swamp.exceptions.SessionRestoreException;

import static org.eclipse.core.runtime.Path.SEPARATOR;
import static eclipseplugin.Activator.PLUGIN_ID;

public class SwampSubmitter {

	private MessageConsoleStream out;
	private SwampApiWrapper api;
	private IWorkbenchWindow window;
	private String configFilepath;

	private static String SWAMP_FAMILY 		= "SWAMP_FAMILY";
	private static String CONFIG_FILENAME 	= "swampconfig.txt";
	private static String PLUGIN_EXIT_MANUAL = "Status: Plugin exited manually.";
	
	public SwampSubmitter(IWorkbenchWindow window) {
		this.window = window;
		//configFilepath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + CONFIG_FILENAME;
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
		MessageConsoleStream stream = console.newMessageStream();
		printInitialInfo(stream);
		return stream;
	}

	private void runBackgroundJob(SubmissionInfo si, boolean fromFile) {
		int UNABLE_TO_DESERIALIZE = 0;
		int UNABLE_TO_GENERATE_BUILD = 1;
		int CYCLICAL_DEPENDENCIES = 2;
		
		Job job = new Job("SWAMP Assessment Submission") {
			
			@Override
			public boolean belongsTo(Object family) {
				return family == SWAMP_FAMILY;
			}
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				if (fromFile) {
					if (!FileSerializer.deserializeSubmissionInfo(configFilepath, si)) {
						// TODO Strengthen error here
						File f = new File(configFilepath);
						f.delete();
						out.println(Utils.getBracketedTimestamp() + "Error: Error in loading from previous assessment found. Please relaunch plugin.");
						Status status = new Status(IStatus.ERROR, "eclipseplugin", UNABLE_TO_DESERIALIZE, "Unable to deserialize previous assessment", null);
						done(status);
						return status;
					}
				}
				
				if (si.needsBuildFile()) {
					IJavaProject jp = JavaCore.create(si.getProject());
					try {
						if (jp.hasClasspathCycle(jp.getRawClasspath())) {
							out.println(Utils.getBracketedTimestamp() + "Error: Classpath has cyclical dependencies. Please resolve these issues and resubmit.");
							Status status = new Status(IStatus.ERROR, "eclipseplugin", CYCLICAL_DEPENDENCIES, "Project has cyclical dependencies", null);
							done(status);
							return status;
						}
					} catch (JavaModelException e1) {
						// TODO Auto-generated catch block
						out.println(Utils.getBracketedTimestamp() + "Error: Unable to access classpath. Please resolve any issues in the project's build path and resubmit.");
						e1.printStackTrace();
						Status status = new Status(IStatus.ERROR, "eclipseplugin", UNABLE_TO_GENERATE_BUILD, "Unable to generate build for this project", null);
						done(status);
						return status;
					}
					out.println(Utils.getBracketedTimestamp() + "Status: Generating build file");
					ImprovedClasspathHandler ich = new ImprovedClasspathHandler(jp, !si.packageSystemLibraries());
					Set<String> files = ich.getFilesToArchive();
					
					String path = BuildfileGenerator.generateBuildFile(ich);
					if (path != null) {
						files.add(path);
					} 
					
					out.println(Utils.getBracketedTimestamp() + "Status: Packaging project " + si.getProjectName());
					String pluginLoc = ich.getProjectPluginLocation();
					Date date = new Date();
					String timestamp = date.toString();
					String filename = timestamp + "-" + si.getPackageName() + ".zip";
					String archiveName = filename.replace(" ", "-").replace(":", "").toLowerCase(); 
					Path archivePath = Utils.zipFiles(files, ich.getProjectPluginLocation(), archiveName);
					
					File pkgConf = PackageInfo.generatePkgConfFile(archivePath, pluginLoc, si.getPackageName(), si.getPackageVersion(), ".", si.getPkgConfPackageType(), si.getBuildSystem(), si.getBuildDirectory(), si.getBuildFile(), si.getBuildTarget());
					//PackageInfo pkgInfo = packageProject(si.getPackageName(), si.getPackageVersion(), pkgDir, pathToArchive, si.getBuildSystem() , si.getBuildDirectory(), si.getBuildFile(), si.getBuildTarget());
					
					out.println(Utils.getBracketedTimestamp() + "Status: Uploading package " + si.getPackageName() + " to SWAMP");
					String prjUUID = si.getSelectedProjectID();
					String pkgVersUUID = uploadPackage(pkgConf.getPath(), archivePath.toString(), prjUUID, si.isNewPackage());
					
					// Delete ant buildfile
					try {
						//FileUtils.forceDelete(pkgConf);
						//FileUtils.forceDelete(archivePath.toFile());
						ich.deleteSwampBin();
					} catch (IOException e) {
						// This isn't really a problem but why?
						e.printStackTrace();
					}
					// Delete swampbin
					// Delete archive
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
					out.println(Utils.getBracketedTimestamp() + "Status: Submitting assessments");
					for (String toolUUID : si.getSelectedToolIDs()) {
						List<Platform> platforms = api.getSupportedPlatforms(toolUUID, prjUUID);
						Set<String> platformSet = new HashSet<>();
						for (Platform p : platforms) {
							platformSet.add(p.getUUIDString());
						}
						for (String platformUUID : si.getSelectedPlatformIDs()) {
							if (platformSet.contains(platformUUID)) {
								submitAssessment(pkgVersUUID, toolUUID, prjUUID, platformUUID);
							}
						}
					}
				}
				IStatus status = Status.OK_STATUS;
				done(status);
				return status;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot()); // we have to lock the root for building projects (i.e. cleaning them). We could potentially get the set of projects, clean the set of projects, and then get a lesser project-scoped rule?
		job.setUser(true);
		job.schedule();
		
	}
	
	/*
	private ClasspathHandler generateBuildFiles(IProject proj, boolean includeSysLibs) throws CyclicDependenciesException, JavaModelException {
		ClasspathHandler classpathHandler = null;
		// Generating Buildfile
		IJavaProject javaProj = JavaCore.create(proj);
		if (javaProj.hasClasspathCycle(javaProj.getRawClasspath())) {
			throw new CyclicDependenciesException("Cycle exists in dependencies making it impossible to build project");
		} 
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		String rootPath = root.getLocation().toString();
		classpathHandler = new ClasspathHandler(null, javaProj, rootPath, includeSysLibs);// cd.getPkgPath()); // TODO replace this w/ workspace path
		//BuildfileGenerator.generateBuildFile(classpathHandler);
		System.out.println("Build file generated");
		return classpathHandler;
	}
	*/
	
	private String uploadPackage(String pkgConfPath, String archivePath, String prjUUID, boolean newPackage) {
		// Upload package
		System.out.println("Uploading package");
		System.out.println("Package-conf directory: " + pkgConfPath);
		System.out.println("Archive directory: " + archivePath);
		System.out.println("Project UUID: " + prjUUID);
		String pkgVersUUID = null;
		try {
			pkgVersUUID = api.uploadPackage(pkgConfPath, archivePath, prjUUID, newPackage);
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
	
	// TODO Throw an exception if we can't get here
	private boolean initializeSwampApi() {
		if (api != null) {
			return true;
		}
		try {
			System.out.println("Initialized SWAMP API");
			api = new SwampApiWrapper(SwampApiWrapper.HostType.DEVELOPMENT);
		} catch (Exception e) {
			out.println(Utils.getBracketedTimestamp() + "Error: Unable to initialize SWAMP API.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static void printInitialInfo(MessageConsoleStream out) {
		Version version = org.eclipse.core.runtime.Platform.getBundle("org.eclipse.platform").getVersion();
		String versionStr = "Eclipse";
		if (version != null) {
			versionStr += " " + version.toString();
		}
		else {
			versionStr += " (could not detect version)";
		}
		out.println(Utils.getBracketedTimestamp() + "Status: Launched SWAMP plugin - running on " + versionStr + ".");
	}
	
	public void launchBackgroundAssessment(IProject project) {
		initializeSwampApi();
		out = initializeConsole("SWAMP Plugin");
		try {
			if (!api.restoreSession()) {
				// launch authentication dialog
				if (!authenticateUser()) {
					return;
				}
			}	
		} catch (Exception e) {
				// launch authentication dialog
			if (!authenticateUser()) {
				return;
			}
		}
	
		configFilepath = project.getWorkingLocation(PLUGIN_ID).toOSString() + SEPARATOR + CONFIG_FILENAME;
		SubmissionInfo si = new SubmissionInfo(this.api);
		if ((configFilepath == null) || (!new File(configFilepath).exists())) {
			out.println(Utils.getBracketedTimestamp() + "Error: No previous assessment found.");
			System.out.println("No previous assessment found at " + configFilepath);
			si.initializeProject(project.getName(), project.getLocation().toOSString());
			launchConfiguration(si);
		}
		else {
			runBackgroundJob(si, true);
		}
	}
	
	private boolean authenticateUser() {
		AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), this.api, this.out);
		ad.create();
		if (ad.open() != Window.OK) {
			return false;
		}
		api.saveSession();
		return true;
	}
	
	private void launchConfiguration(SubmissionInfo si) {
		ConfigDialog cd;
		ToolDialog td;
		PlatformDialog pd;
		Deque<TitleAreaDialog> stack = new ArrayDeque<>();
		
		cd = new ConfigDialog(window.getShell(), si);
		
		td = new ToolDialog(window.getShell(), si);
		
		pd = new PlatformDialog(window.getShell(), si);
		
		stack.addFirst(pd);
		stack.addFirst(td);
		stack.addFirst(cd);
		
		while (!stack.isEmpty()) {
			TitleAreaDialog dialog = stack.removeFirst();
			int retCode = dialog.open();
			if (retCode == Window.CANCEL) {
				out.println(Utils.getBracketedTimestamp() + PLUGIN_EXIT_MANUAL);
				return;
			}
			else if (retCode == IDialogConstants.BACK_ID) {
				stack.addFirst(dialog);
				if (dialog instanceof ToolDialog) {
					stack.addFirst(cd);
				}
				else { // dialog instanceof PlatformDialog
					stack.addFirst(td);
				}
			}
		}
		
		configFilepath = si.getProjectWorkingLocation() + SEPARATOR + CONFIG_FILENAME;
		FileSerializer.serializeSubmissionInfo(configFilepath, si);
		runBackgroundJob(si, false);
		
	}
	
	public void launch(IProject project) {
		initializeSwampApi();
		
		out = initializeConsole("SWAMP Plugin");
		
		try {
			if (!api.restoreSession()) {
				// Add authentication dialog here
				if (!authenticateUser()) {
					return;
				}
			}
		} catch (Exception e) {
			if (!authenticateUser()) {
				return;
			}
		}
		// TODO we can fail here, i.e. by not connecting and we're not handling it as of now
		SubmissionInfo si = new SubmissionInfo(this.api);
		configFilepath = project.getWorkingLocation(PLUGIN_ID).toOSString() + SEPARATOR + CONFIG_FILENAME;
		if ((configFilepath == null) || ((!(new File(configFilepath).exists()))) || (!FileSerializer.deserializeSubmissionInfo(configFilepath, si))) {
			si.initializeProject(project.getName(), project.getLocation().toOSString());
		}
		launchConfiguration(si);
	}
	
	public boolean loggedIntoSwamp() {
		if (!initializeSwampApi()) {
			return false;
		}
		try {
			if (!api.restoreSession()) {
				return false;
			}
		} catch (SessionRestoreException e) {
			return false;
		} catch (SessionExpiredException e) {
			return false;
		}
		
		return true;
	}
	
	public void logIntoSwamp() {
		out = initializeConsole("SWAMP Plugin");
		AuthenticationDialog ad = new AuthenticationDialog(this.window.getShell(), api, out);
		ad.create();
		if (ad.open() != Window.OK) {
			out.println(Utils.getBracketedTimestamp() + "Status: User manually exited login dialog.");
		}
		else {
			api.saveSession();
		}
	}
	
	public void logOutOfSwamp() {
		api.logout();
	}
	
	private void submitAssessment(String pkgUUID, String toolUUID, String prjUUID, String pltUUID) {
		// Submit assessment
		System.out.println("Package UUID: " + pkgUUID);
		System.out.println("Tool UUID: " + toolUUID);
		System.out.println("Project UUID: " + prjUUID);
		System.out.println("Platform UUID: " + pltUUID);
		
		String toolName;
		try {
			toolName = api.getTool(toolUUID, prjUUID).getName();
		} catch (InvalidIdentifierException e1) {
			toolName = "Invalid tool";
		}
		PackageVersion pkg = api.getPackage(pkgUUID, prjUUID);
		assert(pkg != null);
		PackageThing pkgThing = pkg.getPackageThing();
		assert (pkgThing != null);
		String pkgName = pkgThing.getName();
		String platformName = api.getPlatform(pltUUID).getName();

		String assessUUID = null;
		try {
			assessUUID = api.runAssessment(pkgUUID, toolUUID, prjUUID, pltUUID);
		} catch (InvalidIdentifierException | IncompatibleAssessmentTupleException e) {
			// This means that some UUID was invalid
			// This really should never happen
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (assessUUID == null) {
			out.println(Utils.getBracketedTimestamp() + "Error: There was an error in uploading assessment for package {" + pkgName + "} with tool {" + toolName + "} on platform {" + platformName + "}");
			// TODO handle error here
			System.err.println("Error in running assessment.");
		}
		else {
			out.println(Utils.getBracketedTimestamp() + "Status: Successfully submitted assessment with tool {" + toolName + "} on platform {" + platformName +"}");
		}
	}
	
}
