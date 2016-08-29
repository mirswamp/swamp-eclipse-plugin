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

import java.util.List;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.wisc.cs.swamp.SwampApiWrapper;

import static eclipseplugin.Activator.PLUGIN_ID;

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

	private List<? extends PackageThing> packages;
	private String selectedPackageThingID;
	
	// Other
	private SwampApiWrapper api;
	
	public static final String JAVA_8_SRC = "Java 8 Source Code";
	public static final String JAVA_7_SRC = "Java 7 Source Code";
	public static final String JAVA_8_BYTE = "Java 8 Bytecode";
	public static final String JAVA_7_BYTE = "Java 7 Bytecode";
	
	public static String NO_BUILD_STRING = "no-build";
	public static String AUTO_GENERATE_BUILD_STRING = "Auto-generate build file";
	private static String buildOptions[] = { "Auto-generate build file", "android+ant", "android+ant+ivy", "android+gradle", "android+maven", "ant", "ant+ivy", "gradle", "java-bytecode", "make", "Maven", "no-build", "other" };
	private static String pkgTypeOptions[] = { JAVA_8_SRC, JAVA_7_SRC, JAVA_8_BYTE, JAVA_7_BYTE };
	
	public SubmissionInfo(SwampApiWrapper api) {
		this.api = api;
		packageVersion = Utils.getCurrentTimestamp();
		selectedProjectID = null;
		selectedToolIDs = null;
		selectedPlatformIDs = null;
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
		return "java-7";
	}
	
	public void setPackageType(String pkgType) {
		packageType = pkgType;
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
	
	public String[] getBuildSystemList() {
		return buildOptions;
	}
	
	public String[] getPackageTypeList() {
		/*List<String> pkgTypes = api.getPackageTypesList();
		return Utils.convertStringListToArray(pkgTypes);*/
		return pkgTypeOptions;
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
	
	public void setNewPackage(boolean b) {
		newPackage = b;
	}
	
	public String getSelectedPackageID() {
		return selectedPackageThingID;
	}
	
	public void setSelectedPackageID(String pkgUUID) {
		selectedPackageThingID = pkgUUID;
		if (packages == null) {
			packages = api.getPackagesList(selectedProjectID);
		}
		for (int i = 0; i < packages.size(); i++) {
			PackageThing pt = packages.get(i);
			if (pt.getIdentifierString().equals(pkgUUID)) {
				packageName = pt.getName();
				return;
			}
		}
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
	
	public void setBuildInfo(String buildSys, boolean needsBuildFile, String buildDir, String buildFile, String target, boolean packageRTLibs) {
		if (needsBuildFile) {
			createBuildFile = true;
			buildSystem = "ant";
			buildTarget = "build";
			buildDirectory = "."; // Top-level directory
			this.buildFile = getProjectName() + BuildfileGenerator.BUILDFILE_EXT;
			packageSystemLibs = packageRTLibs;
		}
		else if (buildSys.equals(NO_BUILD_STRING)) {
			buildSystem = "no-build";
			buildTarget = "";
			buildDirectory = "";
			this.buildFile = "";
		}
		else {
			buildSystem = buildSys;
			buildDirectory = buildDir;
			this.buildFile = buildFile;
			buildTarget = target;
		}
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
}
