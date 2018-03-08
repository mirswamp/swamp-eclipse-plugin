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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.continuousassurance.swamp.api.AssessmentRun;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.PackageVersion;
import org.continuousassurance.swamp.cli.SwampApiWrapper;
import org.continuousassurance.swamp.cli.exceptions.IncompatibleAssessmentTupleException;
import org.continuousassurance.swamp.cli.exceptions.InvalidIdentifierException;
import org.continuousassurance.swamp.cli.exceptions.SessionExpiredException;
import org.continuousassurance.swamp.cli.exceptions.SessionRestoreException;
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

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;


public class SwampSubmitter {

	/**
	 * Reference to console output stream
	 */
	private MessageConsoleStream out;
	/**
	 * Reference to SwampApiWrapper, which is the interface by which we
	 * communicate with the SWAMP
	 */
	private org.continuousassurance.swamp.cli.SwampApiWrapper api;
	/**
	 * Currently opened window
	 */
	private IWorkbenchWindow window;
	/**
	 * Path to information the plug-in keeps about the submission
	 */
	private String configFilepath;

	/**
	 * Eclipse Job family for the SWAMP jobs
	 */
	private static final String SWAMP_FAMILY 		 = "SWAMP_FAMILY";
	/**
	 * Directory name of directory that stores SWAMP results
	 */
	public static final String SWAMP_RESULTS_DIRNAME = ".SWAMP_RESULTS";
	/**
	 * File name of file with information about the submission
	 */
	private static  final String CONFIG_FILENAME 	 = "swampconfig.txt";
	/**
	 * Message to print to console if plug-in is manually exited
	 */
	private static final String PLUGIN_EXIT_MANUAL = "Status: Plugin exited manually.";
	/**
	 * Name of SWAMP assessment submission job
	 */
	private static final String SWAMP_JOB_TITLE    = "SWAMP Assessment Submission";
	/**
	 * Unable to deserialize previous submission error code
	 */
	private static final int UNABLE_TO_DESERIALIZE = 0;
	/**
	 * Unable to generate build error code
	 */
	private static final int UNABLE_TO_GENERATE_BUILD = 1;
	/**
	 * Cyclical dependencies error code
	 */
	private static final int CYCLICAL_DEPENDENCIES = 2;
	/**
	 * File patterns to delete if job is cancelled
	 */
	private static final String[] FILE_PATTERNS = { ".*\\" + BuildfileGenerator.BUILDFILE_EXT, ImprovedClasspathHandler.SWAMPBIN_DIR, PackageInfo.PACKAGE_CONF_NAME, ".*\\.zip" };
	/**
	 * Number of ticks for uploading package
	 */
	private static final int UPLOAD_TICKS = 80;
	/**
	 * Number of ticks for submitting an individual assessment to the SWAMP
	 */
	private static final int SUBMISSION_TICKS = 10;
	/**
	 * Number of ticks to write package conf
	 */
	private static final int PKG_CONF_TICKS = 10;
	/**
	 * Number of ticks to create archive with all the necessary files
	 */
	private static final int ZIP_TICKS = 40;
	/**
	 * Number of ticks to clean project
	 */
	private static final int CLEAN_PROJECTS_TICKS = 10;
	/**
	 * Number of ticks to handle a single classpath entry
	 */
	public static final int CLASSPATH_ENTRY_TICKS = 5;
	/**
	 * Default console name for plug-in
	 */
	private static final String CONSOLE_NAME = "SWAMP Plugin";
	
	/**
	 * Constructor for SwampSubmitter
	 * @param window currently opened window
	 */
	public SwampSubmitter(IWorkbenchWindow window) {
		this.window = window;
		this.out = initializeConsole(Activator.SWAMP_PLUGIN_CONSOLE_NAME);
		//configFilepath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + CONFIG_FILENAME;
	}
	
	/**
	 * Initializes Console view (note: Console != System.out)
	 * @param consoleName name of the Console
	 * @return reference to stream that writes to Console
	 */
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
			e.printStackTrace();
		}
		MessageConsoleStream stream = console.newMessageStream();
		printInitialInfo(stream);
		return stream;
	}
	
	/**
	 * Prints message to Console view
	 * @param msg message to print
	 */
	private void printToConsole(String msg) {
		if (out == null) {
			out = initializeConsole(CONSOLE_NAME);
		}
		if (out != null) {
			out.println(msg);
		}
	}

	/**
	 * Submits an Eclipse project to the SWAMP. Project is assumed to already
	 * have a build file
	 * @param si Submission object with info about the submission
	 */
	private void submitPreConfiguredJob(SubmissionInfo si) {
		Job job = new Job(SWAMP_JOB_TITLE) {
			
			@Override public boolean belongsTo(Object family) {
				return family.equals(SWAMP_FAMILY);
			}
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				int total = calculateTotalTicks(true, 0, si.getSelectedToolIDs().size());
				SubMonitor subMonitor = SubMonitor.convert(monitor, total);
				
				printToConsole(Utils.getBracketedTimestamp() + "Status: Packaging project " + si.getProjectName());
				String pluginLoc = si.getProject().getWorkingLocation(PLUGIN_ID).toOSString();
				Date date = new Date();
				String timestamp = date.toString();
				//String filename = timestamp + "-" + si.getPackageName() + ".zip";
				String filename = timestamp + "-" + si.getPackageName() + ".tar.gz";
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
				//Path archivePath = Utils.zipFiles(files, pluginLoc, archiveName);
				Path archivePath = null;
				try {
					archivePath = TarUtils.createTarGzip(files, pluginLoc, archiveName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				subMonitor.split(PKG_CONF_TICKS);
				File pkgConf = PackageInfo.generatePkgConfFile(archivePath, pluginLoc, si.getPackageName(), si.getPackageVersion(), ".", si.getPackageLanguage(), si.getPkgConfPackageType(), si.getBuildSystem(), si.getBuildDirectory(), si.getBuildFile(), si.getBuildTarget(), si.getBuildOpts(), si.getConfigDir(), si.getConfigCmd(), si.getConfigOpts());
				
				printToConsole(Utils.getBracketedTimestamp() + "Status: Uploading package " + si.getPackageName() + " to SWAMP");
				String prjUUID = si.getSelectedProjectID();
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				subMonitor.split(UPLOAD_TICKS);
				String pkgVersUUID = uploadPackage(pkgConf.getPath(), archivePath.toString(), prjUUID, si.isNewPackage());
				si.setPackageThing(api.getPackageVersion(pkgVersUUID, prjUUID).getPackageThing());
				// Always do this in case there were problems setting up these directories before
				doNewPackageResultsSetup(si.getPackageThingUUID());
				
			
				String pkgThingUUID = si.getPackageThingUUID();
				setEclipseProjectToPackageThingMapping(pkgThingUUID, si.getProject().getWorkingLocation(PLUGIN_ID).toOSString());
				// TODO: Add results for dependent projects
				
				
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
				
				printToConsole(Utils.getBracketedTimestamp() + "Status: Submitting assessments");
				AssessmentDetails details = new AssessmentDetails(prjUUID, si.getPackageName(), si.getPackageVersion(), si.getProjectName());
				for (String toolUUID : si.getSelectedToolIDs()) {
					for (String platformUUID : si.getSelectedPlatformIDs()) {
						subMonitor.split(SUBMISSION_TICKS);
						details.setResultsFilepath(ResultsUtils.constructFilepath(prjUUID, pkgThingUUID, toolUUID, platformUUID));
						details.setToolName(api.getTool(toolUUID, prjUUID).getName());
						details.setPlatformName(api.getPlatformVersion(platformUUID).getName());
						submitAssessment(pkgVersUUID, toolUUID, prjUUID, platformUUID, details);
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
	
	/**
	 * Generates build file for an Eclipse project and submits it to the SWAMP.
	 * This can all happen in the background
	 * @param si SubmissionInfo object with information about the submission
	 */
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
					printToConsole(Utils.getBracketedTimestamp() + "Error: Unable to parse classpath.");
					Status status = new Status(IStatus.ERROR, "eclipsepluin", UNABLE_TO_GENERATE_BUILD, "Unable to generate build for this project", null);
					done(status);
					return status;
				}
				
				total = calculateTotalTicks(false, numClasspathEntries, si.getSelectedToolIDs().size());
				SubMonitor subMonitor = SubMonitor.convert(monitor, total);
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				if (jp.hasClasspathCycle(entries)) {
					printToConsole(Utils.getBracketedTimestamp() + "Error: Classpath has cyclical dependencies. Please resolve these issues and resubmit.");
					Status status = new Status(IStatus.ERROR, "org.continuousassurance.swamp.eclipse", CYCLICAL_DEPENDENCIES, "Project has cyclical dependencies", null);
					done(status);
					return status;
				}
				
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				printToConsole(Utils.getBracketedTimestamp() + "Status: Generating build file");
				SubMonitor childSubMonitor = subMonitor.split(numClasspathEntries * CLASSPATH_ENTRY_TICKS);
				ImprovedClasspathHandler ich = new ImprovedClasspathHandler(jp, null, !si.packageSystemLibraries(), childSubMonitor);
				Set<String> files = ich.getFilesToArchive();
				
				// TODO: Modularize these into a function call
				if (subMonitor.isCanceled()) {
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
				System.out.println("Java Classpath: " + System.getProperty("java.classpath"));
				BuildfileGenerator.generateBuildFile(ich, files);
				
				try {
					cleanProjects(si.getProject());
				} catch (CoreException e) {
					printToConsole(Utils.getBracketedTimestamp() + "Error: Unable to clean project or dependent projects. The tools may be unable to assess this package.");
				}
				
				subMonitor.split(ZIP_TICKS);
				printToConsole(Utils.getBracketedTimestamp() + "Status: Packaging project " + si.getProjectName());
				String pluginLoc = ich.getRootProjectPluginLocation();
				Date date = new Date();
				String timestamp = date.toString();
				//String filename = timestamp + "-" + si.getPackageName() + ".zip";
				String filename = timestamp + "-" + si.getPackageName() + ".tar.gz";
				String archiveName = filename.replace(" ", "-").replace(":", "").toLowerCase(); 
				//Path archivePath = Utils.zipFiles(files, ich.getRootProjectPluginLocation(), archiveName);
				Path archivePath = null;
				try {
					archivePath = TarUtils.createTarGzip(files, ich.getRootProjectPluginLocation(), archiveName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					IStatus status = Status.CANCEL_STATUS;
					done(status);
					return status;
				}
				
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
				
				printToConsole(Utils.getBracketedTimestamp() + "Status: Uploading package " + si.getPackageName() + " to SWAMP");
				String prjUUID = si.getSelectedProjectID();
				String pkgVersUUID = uploadPackage(pkgConf.getPath(), archivePath.toString(), prjUUID, si.isNewPackage());
				String pkgThingUUID = api.getPackageVersion(pkgVersUUID, prjUUID).getPackageThing().getUUIDString();
				si.setPackageThing(api.getPackageVersion(pkgVersUUID, prjUUID).getPackageThing());
				// Always do this in case there were problems setting up these directories before
				doNewPackageResultsSetup(pkgThingUUID);
				
				setEclipseProjectToPackageThingMapping(pkgThingUUID, ich.getRootProjectPluginLocation());
				for (ImprovedClasspathHandler i : ich.getDependentProjects()) {
					setEclipseProjectToPackageThingMapping(pkgThingUUID, i.getProjectPluginLocation());
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
				
				printToConsole(Utils.getBracketedTimestamp() + "Status: Submitting assessments");
				AssessmentDetails details = new AssessmentDetails(prjUUID, si.getPackageName(), si.getPackageVersion(), si.getProjectName());
				
				for (String toolUUID : si.getSelectedToolIDs()) {
					for (String platformUUID : si.getSelectedPlatformIDs()) {
						subMonitor.split(SUBMISSION_TICKS);
						details.setResultsFilepath(ResultsUtils.constructFilepath(prjUUID, pkgThingUUID, toolUUID, platformUUID));
						details.setToolName(api.getTool(toolUUID, prjUUID).getName());
						details.setPlatformName(api.getPlatformVersion(platformUUID).getName());
						submitAssessment(pkgVersUUID, toolUUID, prjUUID, platformUUID, details);
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
	
	/**
	 * Writes mapping from Eclipse project to SWAMP package thing to file
	 * @param pkgThingUUID SWAMP PackageThing UUID
	 * @param projectPluginLoc file path of project's plug-in directory
	 */
	private void setEclipseProjectToPackageThingMapping(String pkgThingUUID, String projectPluginLoc) {
		//String path = projectPluginLoc + org.eclipse.core.runtime.Path.SEPARATOR + ResultsUtils.ECLIPSE_TO_SWAMP_FILENAME;
		String path = projectPluginLoc + File.separator + ResultsUtils.ECLIPSE_TO_SWAMP_FILENAME;
		
		File f = new File(path);
		if (f.exists()) {
			f.delete();
		}
		try {
			f.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			OutputStreamWriter filewriter = new OutputStreamWriter(new FileOutputStream(f), Activator.ENCODING);
			filewriter.write(pkgThingUUID);
			filewriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets up results directory for results to be downloaded for this
	 * package
	 * @param pkgThingUUID package thing UUID
	 */
	private void doNewPackageResultsSetup(String pkgThingUUID) {
		String dirPath = ResultsUtils.constructFilepath(pkgThingUUID);
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	/**
	 * Estimates total "ticks" for submitting the assessments
	 * @param autoGen is the plug-in generating the build or is the build
	 * already configured
	 * @param numClasspathEntries number of classpath entries in the project
	 * @param numSubmissions number of assessments to be submitted
	 * @return
	 */
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
	
	/**
	 * Removes binaries and other artifacts from project and the projects it
	 * depends on
	 * @param project project to clean
	 * @throws CoreException
	 */
	private void cleanProjects(IProject project) throws CoreException {
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		for (IProject p : project.getReferencedProjects()) {
			cleanProjects(p);
		}
	}
	
	/**
	 * Uploads package to the SWAMP
	 * @param pkgConfPath file path to the package.conf file
	 * @param archivePath file path to the package archive
	 * @param prjUUID UUID of the SWAMP project that this package is part of
	 * @param newPackage true if package is new package
	 * @return
	 */
	private String uploadPackage(String pkgConfPath, String archivePath, String prjUUID, boolean newPackage) {
		// Upload package
		System.out.println("Uploading package");
		System.out.println("Package-conf directory: " + pkgConfPath);
		System.out.println("Archive directory: " + archivePath);
		System.out.println("Project UUID: " + prjUUID);
		String pkgVersUUID = null;
		try {
			pkgVersUUID = api.uploadPackage(pkgConfPath, archivePath, prjUUID, null, newPackage);
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
	/**
	 * Initializes SwampApiWrapper
	 * @return true if api is initialized, false otherwise
	 */
	private boolean initializeSwampApi() {
		if (api != null) {
			return true;
		}
		try {
			System.out.println("Initialized SWAMP API");
			api = new SwampApiWrapper();
		} catch (Exception e) {
			printToConsole(Utils.getBracketedTimestamp() + "Error: Unable to initialize SWAMP API.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Prints initial information  about the SWAMP plug-in to Console view
	 * @param out stream to write to console
	 */
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
	
	/**
	 * Launches assessment submission in background
	 * @param project Eclipse project to be submitted
	 */
	public void launchBackgroundAssessment(IProject project) {
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
			printToConsole(Utils.getBracketedTimestamp() + "Error: No Eclipse project open.");
			return;
		}
	
		configFilepath = project.getWorkingLocation(PLUGIN_ID).toOSString() + File.separator + CONFIG_FILENAME;
		SubmissionInfo si = new SubmissionInfo(this.api);
		if ((configFilepath == null) || (!new File(configFilepath).exists())) {
			printToConsole(Utils.getBracketedTimestamp() + "Error: No previous assessment found.");
			System.out.println("No previous assessment found at " + configFilepath);
			si.initializeProject(project.getName(), project.getLocation().toOSString());
			launchConfiguration(si);
		}
		else if (!FileSerializer.deserializeSubmissionInfo(configFilepath, si)) {
			File f = new File(configFilepath);
			f.delete();
			printToConsole(Utils.getBracketedTimestamp() + "Warning: Unable to reload previous assesment. Configuration dialog will popup now.");
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
	
	/**
	 * Logs into the SWAMP
	 * @return true if user is now logged in
	 */
	public boolean logIntoSwamp() {
		if (!initializeSwampApi()) {
			return false;
		}
		return authenticateUser();
	}
	
	/**
	 * Authenticates the user. Requires user to enter username and password
	 * and checks credentials with the SWAMP
	 * @return true if user is logged in
	 */
	public boolean authenticateUser() {
		AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), this.out);
		ad.create();
		if (ad.open() != Window.OK) {
			printToConsole(Utils.getBracketedTimestamp() + "Status: User manually exited login dialog.");
			return false;
		}
		api = ad.getSwampApiWrapper();
		Activator.setLoggedIn(true);
		return true;
	}
	
	/**
	 * Launches the dialogs (ConfigDialog --> ToolDialog --> PlatformDialog)
	 * @param si SubmissionInfo object storing information about 
	 * this submission
	 */
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
				printToConsole(Utils.getBracketedTimestamp() + PLUGIN_EXIT_MANUAL);
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
		configFilepath = si.getProjectWorkingLocation() + File.separator + CONFIG_FILENAME;
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
	
	/**
	 * Launch process for submitting a set of assessments to the SWAMP. This
	 * will launch the sequence of dialogs 
	 * @param project project selected
	 */
	public void launch(IProject project) {
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
			printToConsole(Utils.getBracketedTimestamp() + "Error: No Eclipse project open.");
			return;
		}
		
		// TODO we can fail here, i.e. by not connecting and we're not handling it as of now
		SubmissionInfo si = new SubmissionInfo(this.api);
		configFilepath = project.getWorkingLocation(PLUGIN_ID).toOSString() + File.separator + CONFIG_FILENAME;
		if ((configFilepath == null) || ((!(new File(configFilepath).exists()))) || (!FileSerializer.deserializeSubmissionInfo(configFilepath, si))) {
			si.initializeProject(project.getName(), project.getLocation().toOSString());
		}
		launchConfiguration(si);
	}
	
	/**
	 * Checks whether a user is logged into the SWAMP
	 * @return true if user is logged in
	 */
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
	
	/**
	 * Logs a user out of the SWAMP
	 */
	public void logOutOfSwamp() {
		Activator.setLoggedIn(false);
		if (!initializeSwampApi()) {
			return;
		}
		api.logout();
	}
	
	/**
	 * Submits a single assessment to the SWAMP
	 * @param pkgUUID UUID for the package version being assessed
	 * @param toolUUID UUID for the tool that the assessment will run on
	 * @param prjUUID UUID for the project that this package is part of
	 * @param pltUUID UUID for the platform that this assessment will run on
	 * @param details info about this assessment
	 */
	private void submitAssessment(String pkgUUID, String toolUUID, String prjUUID, String pltUUID, AssessmentDetails details) {
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
		String platformName = api.getPlatformVersion(pltUUID).getName();

		String assessUUID = null;
		try {
			//AssessmentRun arun = api.runAssessment(pkgUUID, toolUUID, prjUUID, pltUUID);
		    AssessmentRun arun = api.runAssessment(pkg, 
		            api.getTool(toolUUID, prjUUID), 
		            api.getProject(prjUUID),
		            api.getPlatformVersion(pltUUID));
			assessUUID = arun.getIdentifierString();
		} catch (InvalidIdentifierException | IncompatibleAssessmentTupleException e) {
			printToConsole(Utils.getBracketedTimestamp() + "Error: There was an error in uploading assessment for package {" + pkgName + "} with tool {" + toolName + "} on platform {" + platformName + "}");
			// TODO handle error here
			System.err.println("Error in running assessment.");
			e.printStackTrace();
			return;
		}
		if (assessUUID == null) {
			printToConsole(Utils.getBracketedTimestamp() + "Error: There was an error in uploading assessment for package {" + pkgName + "} with tool {" + toolName + "} on platform {" + platformName + "}");
			// TODO handle error here
			System.err.println("Error in running assessment.");
		}
		else {
			details.setSubmissionTime();
			details.setAssessmentUUID(assessUUID);
			System.out.println("Project UUID: " + prjUUID + " Assessment UUID: " + assessUUID);
			details.setStatus(api.getAssessmentRecord(prjUUID, assessUUID).getStatus());
			try {
				SwampSubmitter.appendToUnfinishedFile(details.serialize());
			} catch (IOException e) {
				printToConsole(Utils.getBracketedTimestamp() + "Error: There was an error in storing the assessment information for this submission. Your results for this assessment will not show in Eclipse but will show online.");
				e.printStackTrace();
			}
			// TODO: Take snapshot of the codebase and put it here - possibly even use archive
			printToConsole(Utils.getBracketedTimestamp() + "Status: Successfully submitted assessment with tool {" + toolName + "} on platform {" + platformName +"}");
		}
	}
	
	/**
	 * Appends a new serialized AssessmentDetails object to the unfinished assessments file
	 * @param info serialized AssessmentDetails object
	 * @throws IOException
	 */
	private static void appendToUnfinishedFile(String info) throws IOException {
		System.out.println("Unfinished assessments located at: " + Activator.getUnfinishedAssessmentsPath());
		File resultsDir = new File(ResultsUtils.getTopLevelResultsDirectory());
		if (!resultsDir.exists()) {
			resultsDir.mkdirs();
		}
		File file = new File(Activator.getUnfinishedAssessmentsPath());
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStreamWriter filewriter = new OutputStreamWriter(new FileOutputStream(file, true), Activator.ENCODING);
		filewriter.write(info);
		filewriter.close();
	}
	
	/**
	 * This class allows cancellation of jobs and does clean-up after the
	 * the job is cancelled
	 * @author reid-jr
	 *
	 */
	private static class JobCancellationListener implements IJobChangeListener {

		/**
		 * File patterns that files we want to delete will match
		 */
		private String[] filePatterns;
		/**
		 * Directory that we will delete files
		 */
		private String pluginLocation;
		/**
		 * Reference to the console
		 */
		private MessageConsoleStream out;

		/**
		 * Constructor for JobCancellationListener
		 * @param location file path of directory to delete files in
		 * @param patterns file patterns to be deleted
		 * @param stream allows us to write to console
		 */
		public JobCancellationListener(String location, String[] patterns, MessageConsoleStream stream) {
			filePatterns = patterns;
			pluginLocation = location;
			out = stream;
		}

		@Override
		public void aboutToRun(IJobChangeEvent arg0) {
		}

		@Override
		public void awake(IJobChangeEvent arg0) {
		}

		@Override
		/**
		 * Runs when job is cancelled or finished
		 */
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
		}

		@Override
		public void scheduled(IJobChangeEvent arg0) {
		}

		@Override
		public void sleeping(IJobChangeEvent arg0) {
		}
	}
	
}
