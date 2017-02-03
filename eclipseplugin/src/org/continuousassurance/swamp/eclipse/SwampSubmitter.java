/*
 * Copyright 2016-2017 Malcolm Reid Jr.
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

package org.continuousassurance.swamp.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.continuousassurance.swamp.eclipse.dialogs.AuthenticationDialog;
import org.continuousassurance.swamp.eclipse.dialogs.ConfigDialog;
import org.continuousassurance.swamp.eclipse.dialogs.PlatformDialog;
import org.continuousassurance.swamp.eclipse.dialogs.ToolDialog;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.wisc.cs.swamp.exceptions.IncompatibleAssessmentTupleException;
import edu.wisc.cs.swamp.exceptions.InvalidIdentifierException;
import edu.wisc.cs.swamp.exceptions.SessionExpiredException;
import edu.wisc.cs.swamp.exceptions.SessionRestoreException;

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;
import static org.eclipse.core.runtime.Path.SEPARATOR;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class SwampSubmitter {

	private MessageConsoleStream out;
	private SwampApiWrapper api;
	private IWorkbenchWindow window;
	private String configFilepath;

	private static String SWAMP_FAMILY 		 = "SWAMP_FAMILY";
	public static String SWAMP_RESULTS_DIRNAME = ".SWAMP_RESULTS";
	private static String CONFIG_FILENAME 	 = "swampconfig.txt";
	private static String PLUGIN_EXIT_MANUAL = "Status: Plugin exited manually.";
	private static String SWAMP_JOB_TITLE    = "SWAMP Assessment Submission";
	private static int UNABLE_TO_DESERIALIZE = 0;
	private static int UNABLE_TO_GENERATE_BUILD = 1;
	private static int CYCLICAL_DEPENDENCIES = 2;
	private static String[] FILE_PATTERNS = { ".*\\" + BuildfileGenerator.BUILDFILE_EXT, ImprovedClasspathHandler.SWAMPBIN_DIR, PackageInfo.PACKAGE_CONF_NAME, ".*\\.zip" };

	
	private static int UPLOAD_TICKS = 80;
	private static int SUBMISSION_TICKS = 10;
	private static int PKG_CONF_TICKS = 10;
	private static int ZIP_TICKS = 40;
	private static int CLEAN_PROJECTS_TICKS = 10;
	public static int CLASSPATH_ENTRY_TICKS = 5;
	
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

	private void submitPreConfiguredJob(SubmissionInfo si) {
		Job job = new Job(SWAMP_JOB_TITLE) {
			
			@Override public boolean belongsTo(Object family) {
				return family.equals(SWAMP_FAMILY);
			}
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				int total = calculateTotalTicks(true, 0, si.getSelectedToolIDs().size());
				SubMonitor subMonitor = SubMonitor.convert(monitor, total);
				
				out.println(Utils.getBracketedTimestamp() + "Status: Packaging project " + si.getProjectName());
				String pluginLoc = si.getProject().getWorkingLocation(PLUGIN_ID).toOSString();
				Date date = new Date();
				String timestamp = date.toString();
				String filename = timestamp + "-" + si.getPackageName() + ".zip";
				String archiveName = filename.replace(" ", "-").replace(":", "").toLowerCase(); 
				Set<String> files = new HashSet<String>();
				files.add(si.getProjectPath());
				
				/* Note: for some reason split's cancel wasn't working */
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}

				subMonitor.split(ZIP_TICKS);
				Path archivePath = Utils.zipFiles(files, pluginLoc, archiveName);
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				subMonitor.split(PKG_CONF_TICKS);
				File pkgConf = PackageInfo.generatePkgConfFile(archivePath, pluginLoc, si.getPackageName(), si.getPackageVersion(), ".", si.getPackageLanguage(), si.getPkgConfPackageType(), si.getBuildSystem(), si.getBuildDirectory(), si.getBuildFile(), si.getBuildTarget(), si.getBuildOpts(), si.getConfigDir(), si.getConfigCmd(), si.getConfigOpts());
				
				out.println(Utils.getBracketedTimestamp() + "Status: Uploading package " + si.getPackageName() + " to SWAMP");
				String prjUUID = si.getSelectedProjectID();
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				subMonitor.split(UPLOAD_TICKS);
				String pkgVersUUID = uploadPackage(pkgConf.getPath(), archivePath.toString(), prjUUID, si.isNewPackage());
				File f = new File(pluginLoc + SEPARATOR + "swamp-package.txt");
				if (f.exists()) {
					f.delete();
				}
				try {
					f.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				FileWriter filewriter = null;
				BufferedWriter writer = null;
				
				try {
					filewriter = new FileWriter(f);
					writer = new BufferedWriter(filewriter);
					writer.write(prjUUID);
					writer.close();
				}
				catch (Exception e) {
					System.err.println("Unable to write Eclipse project to SWAMP package mapping to file");
					e.printStackTrace();
				}
				
				// Delete archive
				// Delete package.conf
				/*
				try {
					// TODO Uncomment these before release
					//FileUtils.forceDelete(pkgConf);
					//FileUtils.forceDelete(archivePath.toFile());
				} catch (IOException e) {
					// This isn't really a problem but why?
					e.printStackTrace();
				}
				*/

				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				out.println(Utils.getBracketedTimestamp() + "Status: Submitting assessments");
				
				for (String toolUUID : si.getSelectedToolIDs()) {
					for (String platformUUID : si.getSelectedPlatformIDs()) {
						subMonitor.split(SUBMISSION_TICKS);
						submitAssessment(pkgVersUUID, toolUUID, prjUUID, platformUUID);
					}
				}

				IStatus status = Status.OK_STATUS;
				done(status);
				return status;
			}
		};
		String pluginLocation = si.getProject().getWorkingLocation(PLUGIN_ID).toOSString();
		job.addJobChangeListener(new JobCancellationListener(pluginLocation, SwampSubmitter.FILE_PATTERNS, out));
		job.setRule(si.getProject()); // we have to lock just the project
		job.setUser(true);
		job.schedule();
	}
	
	private void submitAutoGenJob(SubmissionInfo si) {
		Job job = new Job(SWAMP_JOB_TITLE) {
			
			@Override
			public boolean belongsTo(Object family) {
				return family.equals(SWAMP_FAMILY);
			}
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IJavaProject jp = JavaCore.create(si.getProject());
				int total = 0;
				int numClasspathEntries = 0;
				IClasspathEntry[] entries = null;
				try {
					entries = jp.getRawClasspath();
					numClasspathEntries = entries.length;
				} catch (Exception e) {
					out.println(Utils.getBracketedTimestamp() + "Error: Unable to parse classpath.");
					Status status = new Status(IStatus.ERROR, "eclipsepluin", UNABLE_TO_GENERATE_BUILD, "Unable to generate build for this project", null);
					done(status);
					return status;
				}
				
				total = calculateTotalTicks(false, numClasspathEntries, si.getSelectedToolIDs().size());
				SubMonitor subMonitor = SubMonitor.convert(monitor, total);
				System.out.println("Total ticks: " + total);
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				if (jp.hasClasspathCycle(entries)) {
					out.println(Utils.getBracketedTimestamp() + "Error: Classpath has cyclical dependencies. Please resolve these issues and resubmit.");
					Status status = new Status(IStatus.ERROR, "org.continuousassurance.swamp.eclipse", CYCLICAL_DEPENDENCIES, "Project has cyclical dependencies", null);
					done(status);
					return status;
				}
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				out.println(Utils.getBracketedTimestamp() + "Status: Generating build file");
				SubMonitor childSubMonitor = subMonitor.split(numClasspathEntries * CLASSPATH_ENTRY_TICKS);
				ImprovedClasspathHandler ich = new ImprovedClasspathHandler(jp, null, !si.packageSystemLibraries(), childSubMonitor);
				Set<String> files = ich.getFilesToArchive();
				
				// TODO: Modularize these into a function call
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				BuildfileGenerator.generateBuildFile(ich, files);
				
				try {
					cleanProjects(si.getProject());
				} catch (CoreException e) {
					out.println(Utils.getBracketedTimestamp() + "Error: Unable to clean project or dependent projects. The tools may be unable to assess this package.");
				}
				
				subMonitor.split(ZIP_TICKS);
				out.println(Utils.getBracketedTimestamp() + "Status: Packaging project " + si.getProjectName());
				String pluginLoc = ich.getProjectPluginLocation();
				Date date = new Date();
				String timestamp = date.toString();
				String filename = timestamp + "-" + si.getPackageName() + ".zip";
				String archiveName = filename.replace(" ", "-").replace(":", "").toLowerCase(); 
				Path archivePath = Utils.zipFiles(files, ich.getProjectPluginLocation(), archiveName);
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				subMonitor.split(PKG_CONF_TICKS);
				File pkgConf = PackageInfo.generatePkgConfFile(archivePath, pluginLoc, si.getPackageName(), si.getPackageVersion(), ".", "Java", si.getPkgConfPackageType(), si.getBuildSystem(), si.getBuildDirectory(), si.getBuildFile(), si.getBuildTarget(), si.getBuildOpts(), si.getConfigDir(), si.getConfigCmd(), si.getConfigOpts());
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				out.println(Utils.getBracketedTimestamp() + "Status: Uploading package " + si.getPackageName() + " to SWAMP");
				String prjUUID = si.getSelectedProjectID();
				String pkgVersUUID = uploadPackage(pkgConf.getPath(), archivePath.toString(), prjUUID, si.isNewPackage());
				if (si.isNewPackage()) { // All packages will need to be made new for this to be configured properly
					String pkgThingUUID = api.getPackageVersion(pkgVersUUID, prjUUID).getPackageThing().getUUIDString();
					System.out.println("PackageThingUUID: " + pkgThingUUID);
					System.out.println("PackageVersionUUID: " + pkgVersUUID);
					String path = si.getProject().getWorkingLocation(PLUGIN_ID).toOSString() + org.eclipse.core.runtime.Path.SEPARATOR + ResultsUtils.ECLIPSE_TO_SWAMP_FILENAME;
					File f = new File(path);
					if (f.exists()) {
						f.delete();
					}
					try {
						f.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					// TODO Somehow get the PackageThing UUID
					try {
						PrintWriter pw = new PrintWriter(f);
						pw.write(pkgThingUUID);
						pw.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String dirPath = ResultsUtils.constructFilepath(pkgThingUUID);
					File dir = new File(dirPath);
					if (!dir.exists()) {
						dir.mkdir();
					}
				}
				
				/*
				// Delete ant buildfile
				// Delete swampbin
				// Delete archive
				// Delete package.conf
				try {
					// TODO Uncomment these before release, also delete build file
					//FileUtils.forceDelete(pkgConf);
					//FileUtils.forceDelete(archivePath.toFile());
					ich.deleteSwampBin();
				} catch (IOException e) {
					// This isn't really a problem but why?
					e.printStackTrace();
				}
				*/

				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				out.println(Utils.getBracketedTimestamp() + "Status: Submitting assessments");
				
				for (String toolUUID : si.getSelectedToolIDs()) {
					for (String platformUUID : si.getSelectedPlatformIDs()) {
						subMonitor.split(SUBMISSION_TICKS);
						submitAssessment(pkgVersUUID, toolUUID, prjUUID, platformUUID);
					}
				}
			
				IStatus status = Status.OK_STATUS;
				done(status);
				return status;
			}
		};
		String pluginLocation = si.getProject().getWorkingLocation(PLUGIN_ID).toOSString();
		job.addJobChangeListener(new JobCancellationListener(pluginLocation, SwampSubmitter.FILE_PATTERNS, out)); 
		job.setRule(ResourcesPlugin.getWorkspace().getRoot()); // we have to lock the root for building projects (i.e. cleaning them). We could potentially get the set of projects, clean the set of projects, and then get a lesser project-scoped rule?
		job.setUser(true);
		job.schedule();
		
	}
	
	private int calculateTotalTicks(boolean autoGen, int numClasspathEntries, int numSubmissions) {
		int total = 0;
		
		// zip ticks
		total += ZIP_TICKS;
		
		// generate package conf
		total += PKG_CONF_TICKS;
		
		// upload
		total += UPLOAD_TICKS;
				
		// submissions
		total += (numSubmissions * SUBMISSION_TICKS);
		
		if (autoGen) {
			total += CLEAN_PROJECTS_TICKS;
			total += (numClasspathEntries * CLASSPATH_ENTRY_TICKS);
		}
		return total;
	}
	
	private void cleanProjects(IProject project) throws CoreException {
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		for (IProject p : project.getReferencedProjects()) {
			cleanProjects(p);
		}
	}
	
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
			api = new SwampApiWrapper(SwampApiWrapper.HostType.CUSTOM, Activator.getLastHostname());
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
		out = initializeConsole("SWAMP Plugin");
		if (!initializeSwampApi()) {
			return;
		}
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
		
		if (project == null) {
			out.println(Utils.getBracketedTimestamp() + "Error: No Eclipse project open.");
			return;
		}
	
		configFilepath = project.getWorkingLocation(PLUGIN_ID).toOSString() + SEPARATOR + CONFIG_FILENAME;
		SubmissionInfo si = new SubmissionInfo(this.api);
		if ((configFilepath == null) || (!new File(configFilepath).exists())) {
			out.println(Utils.getBracketedTimestamp() + "Error: No previous assessment found.");
			System.out.println("No previous assessment found at " + configFilepath);
			si.initializeProject(project.getName(), project.getLocation().toOSString());
			launchConfiguration(si);
		}
		else if (!FileSerializer.deserializeSubmissionInfo(configFilepath, si)) {
			File f = new File(configFilepath);
			f.delete();
			out.println(Utils.getBracketedTimestamp() + "Warning: Unable to reload previous assesment. Configuration dialog will popup now.");
			si.initializeProject(project.getName(), project.getLocation().toOSString());
			launchConfiguration(si);
		}
		else {
			if (si.needsBuildFile()) {
				submitAutoGenJob(si);
			}
			else {
				submitPreConfiguredJob(si);
			}
		}
	}
	
	public void logIntoSwamp() {
		out = initializeConsole("SWAMP Plugin");
		if (!initializeSwampApi()) {
			return;
		}
		authenticateUser();
	}
	
	private boolean authenticateUser() {
		AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), this.out);
		ad.create();
		if (ad.open() != Window.OK) {
			out.println(Utils.getBracketedTimestamp() + "Status: User manually exited login dialog.");
			return false;
		}
		api = ad.getSwampApiWrapper();
		Activator.setLoggedIn(true);
		return true;
	}
	
	private void launchConfiguration(SubmissionInfo si) {
		ConfigDialog cd;
		ToolDialog td;
		PlatformDialog pd;
		Deque<TitleAreaDialog> stack = new ArrayDeque<>();
		
		// load plug-in preferences
		si.loadPluginSettings();
		
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
		
		// save plug-in preferences
		si.savePluginSettings();
		configFilepath = si.getProjectWorkingLocation() + SEPARATOR + CONFIG_FILENAME;
		FileSerializer.serializeSubmissionInfo(configFilepath, si);
		if (si.isCProject()) {
			submitPreConfiguredJob(si);
			// here we'll call method to handle C project properly
			// TODO The fun stuff

			// (DONE) Get path of makefile relative to project - should be done in ConfigDialog
			// (DONE) Zip project - just add it to files to be zipped
			// (3) Make appropriate modifications to pkgConf
			// (Later) Also zip dependent projects
		}
		else if (si.needsBuildFile()) {
			submitAutoGenJob(si);
		}
		else {
			submitPreConfiguredJob(si);
		}
		
	}
	
	public void fetchResults() {
		if (!initializeSwampApi()) {
			return;
		}
	}
	
	public void launch(IProject project) {
		out = initializeConsole("SWAMP Plugin");

		if (!initializeSwampApi()) {
			return;
		}
		
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
		
		if (project == null) {
			out.println(Utils.getBracketedTimestamp() + "Error: No Eclipse project open.");
			return;
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
	
	public void logOutOfSwamp() {
		Activator.saveResults();
		Activator.setLoggedIn(false);
		if (!initializeSwampApi()) {
			return;
		}
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
		PackageVersion pkg = api.getPackageVersion(pkgUUID, prjUUID);
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
			//File f = new File(System.getProperty("user.home") + File.separator + SWAMP_RESULTS_DIRNAME + File.separator + prjUUID + File.separator + pkgUUID);
			//if (!f.exists()) {
			//	f.mkdirs();
			//}
			// TODO: Take snapshot of the codebase and put it here - possibly even use archive
			out.println(Utils.getBracketedTimestamp() + "Status: Successfully submitted assessment with tool {" + toolName + "} on platform {" + platformName +"}");
			Activator.addAssessment(prjUUID, assessUUID);
		}
	}
	
	private class JobCancellationListener implements IJobChangeListener {

		private String[] filePatterns;
		private String pluginLocation;
		private MessageConsoleStream out;

		public JobCancellationListener(String location, String[] patterns, MessageConsoleStream stream) {
			filePatterns = patterns;
			pluginLocation = location;
			out = stream;
		}

		@Override
		public void aboutToRun(IJobChangeEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void awake(IJobChangeEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void done(IJobChangeEvent event) {
			System.out.println("Done!!");
			System.out.println("Event results: " + event.getResult());

			if (event.getResult().getSeverity() == IStatus.CANCEL) {
				out.println(Utils.getBracketedTimestamp() + "Status: Submission cancelled by user");
				File f = new File(pluginLocation);
				File[] files = f.listFiles();
				if (files != null && files.length > 0) {
					for (File file : files) {
						String fileName = file.getName();
						for (String pattern : filePatterns) {
							if (fileName.matches(pattern)) {
								System.out.println("Deleted file name: " + fileName);
								try {
									if (file.isDirectory()) {
										FileUtils.deleteDirectory(file);
									}
									else {
										FileUtils.forceDelete(file);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
								break;
							}
						}
					}
				}
			}
		}

		@Override
		public void running(IJobChangeEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void scheduled(IJobChangeEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void sleeping(IJobChangeEvent arg0) {
			// TODO Auto-generated method stub	
		}
	}
	
}
