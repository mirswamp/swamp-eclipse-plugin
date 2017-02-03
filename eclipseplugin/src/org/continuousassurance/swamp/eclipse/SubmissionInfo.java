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

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.wisc.cs.swamp.SwampApiWrapper;

/**
 * This class serves as the backend for the Dialog classes.
 * @author reid-jr
 *
 */
public class SubmissionInfo {

	private boolean configInitialized;
	
	private String selectedProjectID;
	private List<String> selectedToolIDs;
	private List<String> selectedPlatformIDs;
	
	private boolean newPackage;
	private boolean createBuildFile;
	private boolean packageSystemLibs;

	private String packageType;
	private IProject[] eclipseProjects;
	
	private String packageName;
	private PackageThing packageThing;
	private String packageVersion;
	private String buildSystem;
	private String buildDirectory;
	private String buildFile;
	private String buildTarget;
	private IProject project;
	
	private String buildOpts;
	private String configOpts;
	private String configCmd;
	private String configDir;

	private List<? extends PackageThing> packages;
	private String selectedPackageThingID;
	
	// Other
	private SwampApiWrapper api;
	
	public static final String JAVA_8_SRC 	= "Java 8 Source Code";
	public static final String JAVA_7_SRC 	= "Java 7 Source Code";
	public static final String JAVA_8_BYTE 	= "Java 8 Bytecode";
	public static final String JAVA_7_BYTE 	= "Java 7 Bytecode";
	public static final String C_CPP		= "C/C++";
	
	public static String NO_BUILD_STRING = "no-build";
	public static String AUTO_GENERATE_BUILD_STRING = "Auto-generate build file";
	public static String ECLIPSE_GENERATED_STRING = "Eclipse-generated makefile";
	private static String javaBuildOptions[] = { AUTO_GENERATE_BUILD_STRING, "android+ant", "android+ant+ivy", "android+gradle", "android+maven", "ant", "ant+ivy", "gradle", "java-bytecode", "make", "maven", NO_BUILD_STRING, "other" };
	private static String cppBuildOptions[] = { ECLIPSE_GENERATED_STRING, "cmake+make", "configure+make", "make", NO_BUILD_STRING, "other" };
	private static String PROJECT_KEY = "PROJECT";
	private static String DELIMITER = ",";
	private static String javaPkgTypeOptions[] = { JAVA_8_SRC, JAVA_7_SRC, JAVA_8_BYTE, JAVA_7_BYTE };
	private static String cppPkgTypeOptions[] = { C_CPP };
	
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
	 * @return package type of Eclipse package
	 */
	public String getPackageType() {
		return packageType;
	}
	
	/**
	 * Gets the package type
	 * @return package type (i.e. "java-8", "java-7", "C/C++")
	 */
	public String getPkgConfPackageType() {
		if (packageType.equals(JAVA_8_SRC) || packageType.equals(JAVA_8_BYTE)) {
			return "java-8";
		}
		if (packageType.equals(JAVA_7_SRC) || packageType.equals(JAVA_7_BYTE)) {
			return "java-7";
		}
		return "C/C++";
	}
	
	/**
	 * Gets language of package
	 * @return "C", "C++", or "Java"
	 */
	public String getPackageLanguage() {
		if (CoreModel.hasCCNature(project)) {
			return "C++";
		}
		if (CoreModel.hasCNature(project)) {
			return "C";
		}
		return "Java";
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
			case "JAVA":
				return javaBuildOptions;
			case "C/C++":
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
			case "JAVA":
				return javaPkgTypeOptions;
			case "C/C++":
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
	 * @return true if Eclipse project is C or C++ project
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
			buildDirectory = buildDir;
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
		this.configDir = configDir;
		this.configCmd = configCmd;
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
	 * Prints list of package types
	 */
	public void printPackageTypes() {
		List<String> packageTypes = api.getPackageTypesList();
		System.out.println("\n\n\nPackageTypes\n=================================================");
		for (String s : packageTypes) {
			System.out.println(s);
		}
		
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
	 * @return key for tools preferences
	 */
	private String getToolKey() {
		return packageType + "-TOOLS"; 
	}
	
	/**
	 * @return key for platform preferences
	 */
	private String getPlatformKey() {
		return packageType + "-PLATFORMS";
	}
	
	/**
	 * Sets build options
	 * @param opts options
	 */
	public void setBuildOpts(String opts) {
		buildOpts = opts;
	}
	
	/**
	 * Sets config options
	 * @param opts options
	 */
	public void setConfigOpts(String opts) {
		configOpts = opts;
	}
	
	/**
	 * Sets config command
	 * @param cmd
	 */
	public void setConfigCmd(String cmd) {
		configCmd = cmd;
	}
	
	/**
	 * Sets config directory
	 * @param dir directory path
	 */
	public void setConfigDir(String dir) {
		configDir = dir;
	}
	
	/**
	 * @return build options
	 */
	public String getBuildOpts() {
		return buildOpts;
	}
	
	/**
	 * @return config options
	 */
	public String getConfigOpts() {
		return configOpts;
	}

	/**
	 * @return config command
	 */
	public String getConfigCmd() {
		return configCmd;
	}
	
	/**
	 * @return config directory path
	 */
	public String getConfigDir() {
		return configDir;
	}
	
	public String getPackageThingUUID() {
		if (packageThing == null) {
			return "";
		}
		return packageThing.getUUIDString();
	}
}
