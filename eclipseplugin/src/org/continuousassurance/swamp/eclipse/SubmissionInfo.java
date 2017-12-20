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

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.cli.SwampApiWrapper;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This class holds information about a set of submissions. It is the back-end
 * data store for the Dialog Classes. When a one-click submission takes place,
 * data from file populates a SubmissionInfo object. The SwampSubmitter uses
 * this class to actually submit assessments.
 * @author reid-jr
 *
 */
public class SubmissionInfo {

	/**
	 * Has the configuration information been successfully set
	 */
	private boolean configInitialized;
	
	/**
	 * UUID of the SWAMP project to be submitted
	 */
	private String selectedProjectID;
	/**
	 * UUIDs of the SWAMP tools that the assessment will be run on
	 */
	private List<String> selectedToolIDs;
	/**
	 * UUIDs of the platforms that the assessment will be run on
	 */
	private List<String> selectedPlatformIDs;
	/**
	 * Is this a new package (i.e. must be uploaded to SWAMP)
	 */
	private boolean newPackage;
	/**
	 * Does the plug-in need to generate the build file for this package?
	 */
	private boolean createBuildFile;
	/**
	 * If the plug-in is generating the build file and packaging dependencies,
	 * should it also package system libraries (default: false)
	 */
	private boolean packageSystemLibs;
	/**
	 * PackageType String for the package
	 */
	private String packageType;
	/**
	 * Eclipse Projects open in the workspace
	 */
	private IProject[] eclipseProjects;
	/**
	 * Name of the package to be uploaded
	 */
	private String packageName;
	/**
	 * PackageThing for the package
	 */
	private PackageThing packageThing;
	/**
	 * Version descriptor for the package
	 */
	private String packageVersion;
	/**
	 * Package build system
	 */
	private String buildSystem;
	/**
	 * Directory for package's build file
	 */
	private String buildDirectory;
	/**
	 * Name of package's build file
	 */
	private String buildFile;
	/**
	 * Target for build
	 */
	private String buildTarget;
	/**
	 * The Eclipse project to be uploaded
	 */
	private IProject project;
	
	/**
	 * Options/flags for build
	 */
	private String buildOpts;
	/**
	 * Options/flags for configuration
	 */
	private String configOpts;
	/**
	 * Config command/script
	 */
	private String configCmd;
	/**
	 * Directory of configure script
	 */
	private String configDir;
	/**
	 * List of existing PackageThings that the user has access to
	 */
	private List<? extends PackageThing> packages;
	/**
	 * UUID of the selected PackageThing
	 */
	private String selectedPackageThingID;
	/**
	 * Reference to SwampApiWrapper
	 */
	private SwampApiWrapper api;
	/**
	 * Java 8 Source Code Package Type
	 */
	public static final String JAVA_8_SRC 	= "Java 8 Source Code";
	/**
	 * Java 7 Source Code Package Type
	 */
	public static final String JAVA_7_SRC 	= "Java 7 Source Code";
	/**
	 * Java 8 Byte Code Package Type
	 */
	public static final String JAVA_8_BYTE 	= "Java 8 Bytecode";
	/**
	 * Java 7 Byte Code Package Type
	 */
	public static final String JAVA_7_BYTE 	= "Java 7 Bytecode";
	/**
	 * C/C++ Package Type
	 */
	public static final String C_CPP		= "C/C++";
	
	/**
	 * SWAMP String for indicating no build necessary
	 */
	public static final String NO_BUILD_STRING = "no-build";
	/**
	 * Text for selection option to have plug-in generate build file
	 */
	public static final String AUTO_GENERATE_BUILD_STRING = "Auto-generate build file";
	/**
	 * Text for selection option to use Eclipse-generated makefile (this is for C/C++ projects)
	 */
	public static final String ECLIPSE_GENERATED_STRING = "Eclipse-generated makefile";
	/**
	 * Build system options for Java
	 */
	private static final String javaBuildOptions[] = { AUTO_GENERATE_BUILD_STRING, "android+ant", "android+ant+ivy", "android+gradle", "android+maven", "ant", "ant+ivy", "gradle", "java-bytecode", "make", "maven", NO_BUILD_STRING, "other" };
	/**
	 * Build system options for C/C++
	 */
	private static final String cppBuildOptions[] = { ECLIPSE_GENERATED_STRING, "cmake+make", "configure+make", "make", NO_BUILD_STRING, "other" };
	/**
	 * Key for saving preferences associated with Eclipse project
	 */
	private static final String PROJECT_KEY = "PROJECT";
	/**
	 * Delimiter for plug-in preferences
	 */
	private static final String DELIMITER = ",";
	/**
	 * Package type options for Java
	 */
	private static final String javaPkgTypeOptions[] = { JAVA_8_SRC, JAVA_7_SRC, JAVA_8_BYTE, JAVA_7_BYTE };
	/**
	 * Package type options for C/C++
	 */
	private static final String cppPkgTypeOptions[] = { C_CPP };
	/**
	 * Java 8 Package Conf Package Type
	 */
	private static final String PKG_CONF_JAVA8 = "java-8";
	/**
	 * Java 7 Package Conf Package Type
	 */
	private static final String PKG_CONF_JAVA7 = "java-7";
	/**
	 * C/C++ Package Conf Package Type
	 */
	private static final String PKG_CONF_CCPP = "C/C++";
	/**
	 * Suffix for adding onto package type to make preference key for tools
	 */
	private static final String TOOLS_PREFERENCE_SUFFIX = "-TOOLS";
	/**
	 * Suffix for adding onto package type to make preference key for platforms
	 */
	private static final String PLATFORMS_PREFERENCE_SUFFIX = "-PLATFORMS";
	/**
	 * Java language
	 */
	private static final String JAVA_LANG = "JAVA";
	/**
	 * C/C++ language
	 */
	private static final String CCPP_LANG = "C/C++";
	
	/**
	 * Constructor for SubmissionInfo
	 * @param api reference to SwampApiWrapper
	 */
	public SubmissionInfo(SwampApiWrapper api) {
		this.api = api;
		packageVersion = Utils.getCurrentTimestamp();
		selectedProjectID = null;
		selectedToolIDs = null;
		selectedPlatformIDs = null;
		
		buildOpts = "";
		configOpts = "";
		configCmd = "";
		configDir = "";
	}
	
	/**
     * Method returns whether configuration information has been set
	 * @return true if config information has been initialized
	 */
	public boolean isConfigInitialized() {
		return configInitialized;
	}
	
	/**
	 * Set the state of the config information
	 * @param b true if config information has been initialized
	 */
	public void setConfigInitialized(boolean b) {
		configInitialized = b;
	}
	
	/**
	 * Getter for SWAMP package type
	 * @return package type of SWAMP package
	 */
	public String getPackageType() {
		return packageType;
	}
	
	/**
	 * Gets the package type as formatted for Package Conf file that gets
	 * uploaded to the SWAMP
	 * @return package type (i.e. "java-8", "java-7", "C/C++")
	 */
	public String getPkgConfPackageType() {
		if (packageType.equals(JAVA_8_SRC) || packageType.equals(JAVA_8_BYTE)) {
			return PKG_CONF_JAVA8;
		}
		if (packageType.equals(JAVA_7_SRC) || packageType.equals(JAVA_7_BYTE)) {
			return PKG_CONF_JAVA7;
		}
		return PKG_CONF_CCPP;
	}
	
	/**
	 * Gets language of package
	 * @return "C", "C++", or "Java"
	 */
	public String getPackageLanguage() {
		String CPP_LANG = "C++";
		String C_LANG = "C";
		String JAVA = "Java";
		if (CoreModel.hasCCNature(project)) {
			return CPP_LANG;
		}
		if (CoreModel.hasCNature(project)) {
			return C_LANG;
		}
		return JAVA;
	}
	
	/**
	 * Sets package type for a SWAMP package and sets tools and platforms based on the preferences for the package type
	 * @param pkgType package type (i.e. "java-8", "java-7", or "C/C++")
	 * @param fromFile true if package type loaded from file
	 */
	public void setPackageType(String pkgType, boolean fromFile) {
		packageType = pkgType;
		if (fromFile) {
			return;
		}
		Preferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		selectedToolIDs = getPreferences(prefs, getToolKey());
		selectedPlatformIDs = getPreferences(prefs, getPlatformKey());
	}
	
	/**
	 * Gets list of users preferences
	 * @param prefs
	 * @param key
	 * @return list of preferences
	 */
	private List<String> getPreferences(Preferences prefs, String key) {
		String idList = prefs.get(key, null);
		if (idList == null) {
			return null;
		}
		return Utils.convertDelimitedStringToList(idList, DELIMITER);
	}
	
	/**
	 * @return SwampApiWrapper object
	 */
	public SwampApiWrapper getApi() {
		return api;
	}
	
	/**
	 * @return true if tools have been initialized as selected
	 */
	public boolean toolsInitialized() {
		return (selectedToolIDs != null); 
	}
	
	/**
	 * @return true if platforms have been initialized as selected
	 */
	public boolean platformsInitialized() {
		return (selectedPlatformIDs != null);
	}
	
	/**
	 * Gets list of build systems for a language 
	 * @param lang either "C/C++" or "JAVA"
	 * @return array of build systems
	 */
	public static String[] getBuildSystemList(String lang) {
		switch(lang.toUpperCase()) {
			case JAVA_LANG:
				return javaBuildOptions;
			case CCPP_LANG: 
				return cppBuildOptions;
		}
		return new String[0];
	}
	
	/**
	 * Gets list of package types for a language
	 * @param lang either "C/C++" or "JAVA"
	 * @return array of package types
	 */
	public static String[] getPackageTypeList(String lang) {
		switch(lang.toUpperCase()) {
			case JAVA_LANG:
				return javaPkgTypeOptions;
			case CCPP_LANG:
				return cppPkgTypeOptions;
		}
		return new String[0];
	}
	
	/**
	 * Sets list of selected platform UUIDs
	 * @param platformIDs list of platform UUIDs
	 */
	public void setSelectedPlatformIDs(List<String> platformIDs) {
		selectedPlatformIDs = platformIDs;
	}
	
	/**
	 * Gets list of currently selected platform UUIDs
	 * @return list of platform UUIDs
	 */
	public List<String> getSelectedPlatformIDs() {
		return selectedPlatformIDs;
	}
	
	/**
	 * Sets list of selected tool IDs
	 * @param toolIDs list of tool UUIDs
	 */
	public void setSelectedToolIDs(List<String> toolIDs) {
		selectedToolIDs = toolIDs;
	}
	
	/**
	 * Gets list of currently selected tool IDs
	 * @return list of tool UUIDs
	 */
	public List<String> getSelectedToolIDs() {
		return selectedToolIDs;
	}
	
	/**
	 * Sets selected project SWAMP UUID
	 * @param projectID SWAMP UUID
	 */
	public void setSelectedProjectID(String projectID) {
		selectedProjectID = projectID;
	}
	
	/**
	 * Gets selected project SWAMP UUID
	 * @return SWAMP UUID
	 */
	public String getSelectedProjectID() {
		return selectedProjectID;
	}
	
	/**
	 * @return currently selected Eclipse project
	 */
	public IProject getProject() {
		return project;
	}
	
	/**
	 * @return true if Eclipse package is set to be uploaded as a new package
	 */
	public boolean isNewPackage() {
		return newPackage;
	}
	
	/**
	 * @return true if Eclipse project to be assessed is C or C++ project
	 */
	public boolean isCProject() {
		return (CoreModel.hasCCNature(project) || CoreModel.hasCNature(project));
	}
	
	/**
	 * Sets whether Eclipse package should be uploaded as a new package
	 * @param isNew true if package is new
	 */
	public void setNewPackage(boolean isNew) {
		newPackage = isNew;
	}
	
	/**
	 * Getter for PackageThing UUID
	 * @return selected SWAMP package UUID
	 */
	public String getSelectedPackageID() {
		return selectedPackageThingID;
	}
	
	/**
	 * Sets selected package ID
	 * @param pkgUUID SWAMP package UUID
	 * @return
	 */
	public boolean setSelectedPackageID(String pkgUUID) {
		selectedPackageThingID = pkgUUID;
		if (packages == null) {
			packages = api.getPackagesList(selectedProjectID);
		}
		for (int i = 0; i < packages.size(); i++) {
			PackageThing pt = packages.get(i);
			if (pt.getIdentifierString().equals(pkgUUID)) {
				packageThing = pt;
				packageName = pt.getName();
				return true;
			}
		}
		packageThing = null;
		return false;
		// Need to get a package thing here, should be able to do it in O(1) rather than O(N)
		// TODO Talk to Vamshi about this
	}
	
	/**
	 * Attempts to set SWAMP package from name
	 * @param pkgName name of Eclipse package
	 * @return true if SWAMP package successfully set
	 */
	public boolean setPackageIDFromName(String pkgName) {
		if (packages == null) {	
			packages = api.getPackagesList(selectedProjectID);
		}
		for (int i = 0; i < packages.size(); i++) {
			PackageThing pt = packages.get(i);
			System.out.println(pt.getName());
			if (pt.getName().equals(pkgName)) {
				packageThing = pt;
				selectedPackageThingID = pt.getUUIDString();
				packageName = pt.getName();
				return true;
			}
		}
		packageThing = null;
		return false;
	}
	
	
	/**
	 * Initializes list of Eclipse projects and finds project with matching name and project path
	 * @param projectName name of project
	 * @param prjPath path of project
	 * @return true if the specified project is in the workspace, false otherwise
	 */
	public boolean initializeProject(String projectName, String prjPath) {
		// TODO add code to actually get and initialize the project
		eclipseProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < eclipseProjects.length; i++) {
			IProject project = eclipseProjects[i];
			if (project.getName().equals(projectName) && project.getLocation().toOSString().equals(prjPath)) {
				this.project = project;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Getter for whether Eclipse project has been set
	 * @return true if Eclipse project has been set
	 */
	public boolean isProjectInitialized() {
		return project != null;
	}
	
	/**
	 * Sets Eclipse project corresponding to the SWAMP package 
	 * @param p Eclipse project
	 */
	public void setProject(IProject p) {
		this.project = p;
	}
	
	/**
	 * Getter for whether system libraries should be packaged as part of the
	 * package uploaded
	 * @return true if system libraries should be packaged
	 */
	public boolean packageSystemLibraries() {
		return packageSystemLibs;
	}
	
	/**
	 * Sets build information for the Eclipse package
	 * @param buildSys build system
	 * @param needsBuildFile true if package needs build file
	 * @param buildDir build directory 
	 * @param buildFile build file
	 * @param target build target
	 * @param buildOptions build options
	 * @param packageRTLibs true if system/run-time libraries need to be packaged
	 */
	public void setBuildInfo(String buildSys, boolean needsBuildFile, String buildDir, String buildFile, String target, String buildOptions, boolean packageRTLibs) {
		createBuildFile = needsBuildFile;
		if (needsBuildFile) {
			buildSystem = "ant";
			buildTarget = "build";
			buildDirectory = "."; // Top-level directory
			this.buildFile = getProjectName() + BuildfileGenerator.BUILDFILE_EXT;
			buildOpts = "";
			packageSystemLibs = packageRTLibs;
		}
		else if (buildSys.equals(NO_BUILD_STRING)) {
			buildSystem = "no-build";
			buildTarget = "";
			buildDirectory = "";
			this.buildFile = "";
			buildOpts = "";
		}
		else {
			buildSystem = buildSys.equals(ECLIPSE_GENERATED_STRING) ? "make" : buildSys;
			//buildDirectory = buildDir;
			buildDirectory = FilenameUtils.separatorsToUnix(buildDir);
			this.buildFile = buildFile;
			buildTarget = target;
			buildOpts = buildOptions;
		}
	}
	
	/**
	 * Sets configuration information for the Eclipse package
	 * @param configDir config directory
	 * @param configCmd config command
	 * @param configOptions config options
	 */
	public void setConfigInfo(String configDir, String configCmd, String configOptions) {
		//this.configDir = configDir;
		//this.configCmd = configCmd;
		this.configDir =  FilenameUtils.separatorsToUnix(configDir);
		this.configCmd =  FilenameUtils.separatorsToUnix(configCmd);
		this.configOpts = configOptions;
	}
	
	/**
	 * @return name of the project
	 */
	public String getProjectName() {
		return project.getName();
	}
	
	/**
	 * Returns the location of the Eclipse project on the file system
	 * @return project path
	 */
	public String getProjectPath() {
		return project.getLocation().toOSString();
	}
	
	/**
	 * Returns a project-specific location that the plug-in uses to store files
	 * related to build file generation
	 * @return working location path
	 */
	public String getProjectWorkingLocation() {
		return project.getWorkingLocation(PLUGIN_ID).toOSString();
	}
	
	/**
	 * Gets whether the package to be assessed needs a build file
	 * @return true if package needs a build file
	 */
	public boolean needsBuildFile() {
		return this.createBuildFile;
	}
	
	/**
	 * Sets whether the package to be assessed needs a build file
	 * @param b true if package needs a build file
	 */
	public void setNeedsBuildFile(boolean b) {
		this.createBuildFile = b;
	}
	
	/**
	 * Getter for build target
	 * @return build target
	 */
	public String getBuildTarget() {
		return buildTarget;
	}
	
	/**
	 * Getter for build file
	 * @return build file path
	 */
	public String getBuildFile() {
		return buildFile;
	}
	
	/**
	 * Getter for build directory
	 * @return build directory path
	 */
	public String getBuildDirectory() {
		return buildDirectory;
	}
	
	/**
	 * Getter for SWAMP package build system
	 * @return build system
	 */
	public String getBuildSystem() {
		return buildSystem;
	}
	
	/**
	 * Getter for SWAMP package version
	 * @return version string
	 */
	public String getPackageVersion() {
		return packageVersion;
	}
	
	/**
	 * Setter for SWAMP package version
	 * @param pkgVers version string
	 */
	public void setPackageVersion(String pkgVers) {
		packageVersion = pkgVers;
	}
	
	/**
	 * Getter for SWAMP package name
	 * @return SWAMP package name
	 */
	public String getPackageName() {
		return packageName;
	}
	
	/**
	 * Setter for SWAMP package name
	 * @param packageName SWAMP package name
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * Saves plugin preferences
	 */
	public void savePluginSettings() {
		Preferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		prefs.put(PROJECT_KEY, selectedProjectID);
		prefs.put(getToolKey(), Utils.convertListToDelimitedString(selectedToolIDs, DELIMITER));
		prefs.put(getPlatformKey(), Utils.convertListToDelimitedString(selectedPlatformIDs, DELIMITER));
		try {
			prefs.flush();
		} catch(BackingStoreException e) {
		}
	}
	
	/**
	 * Loads plugin preferences
	 */
	public void loadPluginSettings() {
		if (selectedProjectID != null) {
			return;
		}
		Preferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		selectedProjectID = prefs.get(PROJECT_KEY, null);
	}
	
	/**
	 * Getter for key for tools preferences
	 * @return key for tools preferences
	 */
	private String getToolKey() {
		return packageType + TOOLS_PREFERENCE_SUFFIX;
	}
	
	/**
	 * Getter for key for platform preferences
	 * @return key for platform preferences
	 */
	private String getPlatformKey() {
		return packageType + PLATFORMS_PREFERENCE_SUFFIX;
	}
	
	/**
	 * Setter for build options
	 * @param opts options
	 */
	public void setBuildOpts(String opts) {
		buildOpts = opts;
	}
	
	/**
	 * Setter for config options
	 * @param opts options
	 */
	public void setConfigOpts(String opts) {
		configOpts = opts;
	}
	
	/**
	 * Setter for config command
	 * @param cmd
	 */
	public void setConfigCmd(String cmd) {
		configCmd = cmd;
	}
	
	/**
	 * Setter for config directory
	 * @param dir directory path
	 */
	public void setConfigDir(String dir) {
		configDir = dir;
	}
	
	/**
	 * Getter for build options
	 * @return build options
	 */
	public String getBuildOpts() {
		return buildOpts;
	}
	
	/**
	 * Getter for config options
	 * @return config options
	 */
	public String getConfigOpts() {
		return configOpts;
	}

	/**
	 * Getter for config command/script
	 * @return config command
	 */
	public String getConfigCmd() {
		return configCmd;
	}
	
	/**
	 * Getter for config directory
	 * @return config directory path
	 */
	public String getConfigDir() {
		return configDir;
	}
	
	/**
	 * Getter for Package Thing UUID
	 * @return UUID for the Package Thing to be assessed
	 */
	public String getPackageThingUUID() {
		if (packageThing == null) {
			return "";
		}
		return packageThing.getUUIDString();
	}
	
	/**
	 * Setter for PackageThing to be submitted
	 * @param p PackageThing
	 */
	public void setPackageThing(PackageThing p) {
		packageThing = p;
	}
}
