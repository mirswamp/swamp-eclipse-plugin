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

import static eclipseplugin.Activator.PLUGIN_ID;

import java.util.List;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.wisc.cs.swamp.SwampApiWrapper;

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
	
	public boolean isConfigInitialized() {
		return configInitialized;
	}
	
	public void setConfigInitialized(boolean b) {
		configInitialized = b;
	}
	
	public String getPackageType() {
		return packageType;
	}
	

	public String getPkgConfPackageType() {
		if (packageType.equals(JAVA_8_SRC) || packageType.equals(JAVA_8_BYTE)) {
			return "java-8";
		}
		if (packageType.equals(JAVA_7_SRC) || packageType.equals(JAVA_7_SRC)) {
			return "java-7";
		}
		return "C/C++";
	}
	
	public String getPackageLanguage() {
		if (CoreModel.hasCCNature(project)) {
			return "C++";
		}
		if (CoreModel.hasCNature(project)) {
			return "C";
		}
		return "Java";
	}
	
	public void setPackageType(String pkgType, boolean fromFile) {
		packageType = pkgType;
		if (fromFile) {
			return;
		}
		Preferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		selectedToolIDs = getPreferences(prefs, getToolKey());
		selectedPlatformIDs = getPreferences(prefs, getPlatformKey());
	}
	
	private List<String> getPreferences(Preferences prefs, String key) {
		String idList = prefs.get(key, null);
		if (idList == null) {
			return null;
		}
		return Utils.convertDelimitedStringToList(idList, DELIMITER);
	}
	
	public SwampApiWrapper getApi() {
		return api;
	}
	
	public boolean toolsInitialized() {
		return (selectedToolIDs != null); 
	}
	
	public boolean platformsInitialized() {
		return (selectedPlatformIDs != null);
	}
	
	public String[] getBuildSystemList(String lang) {
		switch(lang.toUpperCase()) {
			case "JAVA":
				return javaBuildOptions;
			case "C/C++":
				return cppBuildOptions;
		}
		return new String[0];
	}
	
	public String[] getPackageTypeList(String lang) {
		switch(lang.toUpperCase()) {
			case "JAVA":
				return javaPkgTypeOptions;
			case "C/C++":
				return cppPkgTypeOptions;
		}
		return new String[0];
	}
	
	public void setSelectedPlatformIDs(List<String> platformIDs) {
		selectedPlatformIDs = platformIDs;
	}
	
	public List<String> getSelectedPlatformIDs() {
		return selectedPlatformIDs;
	}
	
	public void setSelectedToolIDs(List<String> toolIDs) {
		selectedToolIDs = toolIDs;
	}
	
	public List<String> getSelectedToolIDs() {
		return selectedToolIDs;
	}
	
	public void setSelectedProjectID(String projectID) {
		selectedProjectID = projectID;
	}
	
	public String getSelectedProjectID() {
		return selectedProjectID;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public boolean isNewPackage() {
		return newPackage;
	}
	
	public boolean isCProject() {
		return (CoreModel.hasCCNature(project) || CoreModel.hasCNature(project));
	}
	
	public void setNewPackage(boolean b) {
		newPackage = b;
	}
	
	public String getSelectedPackageID() {
		return selectedPackageThingID;
	}
	
	public boolean setSelectedPackageID(String pkgUUID) {
		selectedPackageThingID = pkgUUID;
		if (packages == null) {
			packages = api.getPackagesList(selectedProjectID);
		}
		for (int i = 0; i < packages.size(); i++) {
			PackageThing pt = packages.get(i);
			if (pt.getIdentifierString().equals(pkgUUID)) {
				packageName = pt.getName();
				return true;
			}
		}
		return false;
		// Need to get a package thing here, should be able to do it in O(1) rather than O(N)
		// TODO Talk to Vamshi about this
	}
	
	public boolean setPackageIDFromName(String pkgName) {
		if (packages == null) {	
			packages = api.getPackagesList(selectedProjectID);
		}
		for (int i = 0; i < packages.size(); i++) {
			PackageThing pt = packages.get(i);
			System.out.println(pt.getName());
			if (pt.getName().equals(pkgName)) {
				selectedPackageThingID = pt.getUUIDString();
				packageName = pt.getName();
				return true;
			}
		}
		return false;
	}
	
	
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
	
	public boolean isProjectInitialized() {
		return project != null;
	}
	
	public void setProject(IProject p) {
		this.project = p;
	}
	
	public boolean packageSystemLibraries() {
		return packageSystemLibs;
	}
	
	public void setBuildInfo(String buildSys, boolean needsBuildFile, String buildDir, String buildFile, String target, String buildOptions, boolean packageRTLibs) {
		if (needsBuildFile) {
			createBuildFile = true;
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
	
	public void setConfigInfo(String configDir, String configCmd, String configOptions) {
		this.configDir = configDir;
		this.configCmd = configCmd;
		this.configOpts = configOptions;
	}
	
	public String getProjectName() {
		return project.getName();
	}
	
	
	public String getProjectPath() {
		return project.getLocation().toOSString();
	}
	
	public String getProjectWorkingLocation() {
		return project.getWorkingLocation(PLUGIN_ID).toOSString();
	}
	
	public boolean needsBuildFile() {
		return this.createBuildFile;
	}
	
	public void setNeedsBuildFile(boolean b) {
		this.createBuildFile = b;
	}
	
	public String getBuildTarget() {
		return buildTarget;
	}
	
	public String getBuildFile() {
		return buildFile;
	}
	
	public String getBuildDirectory() {
		return buildDirectory;
	}
	
	public String getBuildSystem() {
		return buildSystem;
	}
	
	public String getPackageVersion() {
		return packageVersion;
	}
	
	public void setPackageVersion(String pkgVers) {
		packageVersion = pkgVers;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void printPackageTypes() {
		List<String> packageTypes = api.getPackageTypesList();
		System.out.println("\n\n\nPackageTypes\n=================================================");
		for (String s : packageTypes) {
			System.out.println(s);
		}
		
	}
	
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
	
	public void loadPluginSettings() {
		if (selectedProjectID != null) {
			return;
		}
		Preferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		selectedProjectID = prefs.get(PROJECT_KEY, null);
	}
	
	private String getToolKey() {
		return packageType + "-TOOLS"; 
	}
	
	private String getPlatformKey() {
		return packageType + "-PLATFORMS";
	}
	
	public void setBuildOpts(String opts) {
		buildOpts = opts;
	}
	
	public void setConfigOpts(String opts) {
		configOpts = opts;
	}
	
	public void setConfigCmd(String cmd) {
		configCmd = cmd;
	}
	
	public void setConfigDir(String dir) {
		configDir = dir;
	}
	
	public String getBuildOpts() {
		return buildOpts;
	}
	
	public String getConfigOpts() {
		return configOpts;
	}
	
	public String getConfigCmd() {
		return configCmd;
	}
	
	public String getConfigDir() {
		return configDir;
	}
}
