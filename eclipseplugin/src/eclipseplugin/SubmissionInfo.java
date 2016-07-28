package eclipseplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.Platform;
import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.api.Tool;
import edu.wisc.cs.swamp.SwampApiWrapper;

public class SubmissionInfo {

	private boolean selectionInitialized;
	private boolean configInitialized;
	
	// Selection
	private List<? extends Project> projects;
	private List<? extends Tool> tools;
	private List<? extends Platform> platforms;
	private String selectedProjectID;
	private List<String> selectedPlatformIDs;
	private List<String> selectedToolIDs;
	private Map<String, Integer> platformMap;
	private Map<String, Integer> toolMap;
	
	// Configuration
	private IProject[] eclipseProjects;
	private List<? extends PackageThing> swampPackages;

	private int prjIndex;
	private int pkgIndex;
	private int buildSysIndex;
	
	private String packageName;
	private String packageVersion;
	private String buildSystem;
	private String buildDirectory;
	private String buildFile;
	private String buildTarget;
	private IProject project;
	private boolean createBuildFile;
	private List<? extends PackageThing> packages;
	private String selectedPackageThingID;
	
	// Other
	private SwampApiWrapper api;
	
	private static int NO_BUILD = 11;
	private static int AUTO_GENERATE_BUILD = 0;	
	private static String buildOptions[] = { "Auto-generate build file", "android+ant", "android+ant+ivy", "android+gradle", "android+maven", "ant", "ant+ivy", "gradle", "java-bytecode", "make", "Maven", "no-build", "other" };

	
	public SubmissionInfo(SwampApiWrapper api) {
		this.api = api;
		prjIndex = -1;
		pkgIndex = -1;
		buildSysIndex = -1;
		packageVersion = Utils.getCurrentTimestamp();
	}
	
	public boolean isSelectionInitialized() {
		return selectionInitialized;
	}
	
	public void setSelectionInitialized(boolean b) {
		selectionInitialized = b;
	}
	
	public boolean isConfigInitialized() {
		return configInitialized;
	}
	
	public void setConfigInitialized(boolean b) {
		configInitialized = b;
	}
	
	public String[] getProjectList() {
		if (projects == null) {
			projects = api.getProjectsList();
		}
		int length = projects.size();
		String[] array = new String[length];
		for (int i = 0; i < length; i++) {
			array[i] = projects.get(i).getFullName();
		}
		return array;
	}
	
	public String[] getPlatformList() {
		if (platforms == null) {
			platforms = api.getPlatformsList();
		}
		int length = platforms.size();
		String[] array = new String[length];
		for (int i = 0; i < length; i++) {
			array[i] = platforms.get(i).getName();
		}
		return array;
	}
	
	public String[] getToolList() {
		// TODO Fill this in
		/*
		if (selectedProjectID != null) {
			tools = api.getTools(selectedProjectID, pkgType);
		}
		*/
		// TODO Add dialog for selecting package type
		tools = api.getTools("Java 8 Source Code", selectedProjectID);
		int length = tools.size();
		String[] array = new String[length];
		for (int i = 0; i < length; i++) {
			array[i] = tools.get(i).getName();
		}
		return array;
	}
	
	public String[] getEclipseProjectList() {
		if (eclipseProjects == null) {
			eclipseProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		}
		ArrayList<String> list = new ArrayList<String>();
		String[] array = null;
		for (IProject prj : eclipseProjects) {
			System.out.println(prj.getName());
			list.add(prj.getName());
			try {
				prj.open(null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (list != null) {
			array = new String[list.size()];
			list.toArray(array);
		}
		return array;
	}
	
	public String[] getSwampPackageList() {
		swampPackages = api.getPackagesList(selectedProjectID);
		int numPackages = swampPackages.size() + 1;
		String[] pkgNames = new String[numPackages];
		pkgNames[0] = "Create new package";
		for (int i = 1; i < numPackages; i++) {
			pkgNames[i] = swampPackages.get(i-1).getName();
		}
		return pkgNames; 
	}
	
	public String[] getBuildSystemList() {
		return buildOptions;
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
	
	public int getProjectIndex() {
		for (int i = 0; i < projects.size(); i++) {
			if (projects.get(i).getUUIDString().equals(selectedProjectID)) {
				return i;
			}
		}
		return -1;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public int[] getPlatformIndices() {
		platformMap = new HashMap<String, Integer>();
		List<Integer> indices = new ArrayList<Integer>();
		for (int i = 0; i < platforms.size(); i++) {
			platformMap.put(platforms.get(i).getUUIDString(), i);
		}
		for (int i = 0; i < selectedPlatformIDs.size(); i++) {
			String uuid = selectedPlatformIDs.get(i);
			if (!platformMap.containsKey(uuid)) {
				continue;
			}
			indices.add(platformMap.get(uuid));
		}
		if (indices.isEmpty()) {
			return null;
		}
		return Utils.convertIntListToArray(indices);
	}
	
	public int[] getToolIndices() {
		toolMap = new HashMap<String, Integer>();
		List<Integer> indices = new ArrayList<Integer>();
		for (int i = 0; i < tools.size(); i++) {
			toolMap.put(tools.get(i).getUUIDString(), i);
		}
		for (int i = 0; i < selectedToolIDs.size(); i++) {
			String uuid = selectedToolIDs.get(i);
			if (!toolMap.containsKey(uuid)) {
				continue;
			}
			indices.add(toolMap.get(uuid));
		}
		if (indices.isEmpty()) {
			return null;
		}
		return Utils.convertIntListToArray(indices);
	}
	
	public void setProject(int index) {
		Project project = projects.get(index);
		selectedProjectID = project.getUUIDString();
	}
	
	public void setPlatforms(int[] indices) {
		List<String> list = new ArrayList<String>(indices.length);
		for (int i : indices) {
			Platform platform = platforms.get(i);
			list.add(platform.getUUIDString());
		}
		selectedPlatformIDs = list;
	}
	
	public void setTools(int[] indices) {
		List<String> list = new ArrayList<String>(indices.length);
		for (int i : indices) {
			Tool tool = tools.get(i);
			list.add(tool.getUUIDString());
		}
		selectedToolIDs = list;
	}
	
	public boolean validPlatformToolPairsExist() {
		Set<String> platformSet = new HashSet<String>(platforms.size());
		for (int i = 0; i < selectedPlatformIDs.size(); i++) {
			platformSet.add(selectedPlatformIDs.get(i));
		}
		System.out.println("Selected Project ID: " + selectedProjectID);
		for (int i = 0; i < selectedToolIDs.size(); i++) {
			System.out.println("Selected Tool ID: " + selectedToolIDs.get(i));
			List<Platform> supportedPlatforms = api.getSupportedPlatforms(selectedToolIDs.get(i), selectedProjectID);
			for (Platform p : supportedPlatforms) {
				if (platformSet.contains(p.getUUIDString())) {
					return true;
				}
			}
		}
		return false;
	}

	public String getSelectedPackageID() {
		return selectedPackageThingID;
	}
	
	public void setSelectedPackageID(String pkgUUID) {
		selectedPackageThingID = pkgUUID;
	}
	
	public boolean setPackageIDFromName(String pkgName) {
		if (packages == null) {	
			packages = api.getPackagesList(selectedProjectID);
		}
		for (int i = 0; i < packages.size(); i++) {
			PackageThing pt = packages.get(i);
			if (pt.getName().equals(pkgName)) {
				pkgIndex = i+1; // +1 for create new package
				selectedPackageThingID = pt.getUUIDString();
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
			if (project.getName().equals(projectName) && project.getLocation().toString().equals(prjPath)) {
				prjIndex = i;
				this.project = project;
				return true;
			}
		}
		return false;
	}
	
	public int getSelectedProjectIndex() {
		return prjIndex;
	}
	
	public int getSelectedPackageIndex() {
		if (this.isNewPackage()) {
			return 0;
		}
		if (pkgIndex > -1) {
			return pkgIndex; // +1 for create new package
		}
		for (int i = 0; i < swampPackages.size(); i++) {
			PackageThing pt = swampPackages.get(i);
			if (pt.getUUIDString() == selectedPackageThingID) {
				pkgIndex = i+1;
				return pkgIndex;
			}
		}
		return 0;
	}
	
	public void setSelectedPackageIndex(int index) {
		pkgIndex = index;
		if (pkgIndex == 0) {
			selectedPackageThingID = null;
			return;
		}
		PackageThing pt = swampPackages.get(index-1);
		selectedPackageThingID = pt.getUUIDString();
		packageName = pt.getName();
	}
	
	public void setSelectedBuildSysIndex(int index) {
		buildSysIndex = index;
	}
	
	public void setSelectedProjectIndex(int index) {
		prjIndex = index;
		project = eclipseProjects[index];
	}
	
	public int getSelectedBuildSysIndex() {
		if (buildSysIndex == AUTO_GENERATE_BUILD) {
			return AUTO_GENERATE_BUILD;
		}
		for (int i = 0; i < buildOptions.length; i++) {
			if (buildSystem.equals(buildOptions[i])) {
				buildSysIndex = i;
				return i;
			}
		}
		// TODO Add error handling here SHOULDN'T HAPPEN
		return 0;
	}
	
	public void setBuildInfo(String dir, String file, String target) {
		if (buildSysIndex == AUTO_GENERATE_BUILD) {
			createBuildFile = true;
			buildSystem = "ant";
			buildTarget = "build";
			buildDirectory = "." + project.getName();
			buildFile = "build.xml";
		}
		else if (buildSysIndex == NO_BUILD) {
			buildSystem = "no-build";
			buildTarget = "";
			buildDirectory = "";
			buildFile = "";
		}
		else {
			buildDirectory = dir;
			buildFile = file;
			buildTarget = target;
		}
	}
	
	public boolean noBuild() {
		return buildSysIndex == NO_BUILD;
	}
	
	public boolean generateBuild() {
		return buildSysIndex == AUTO_GENERATE_BUILD;
	}
	
	public String getProjectName() {
		return project.getName();
	}
	
	public String getProjectPath() {
		return project.getLocation().toString();
	}
	
	public boolean needsBuildFile() {
		return this.createBuildFile;
	}
	
	public void setNeedsBuildFile(boolean b) {
		this.createBuildFile = b;
		buildSysIndex = AUTO_GENERATE_BUILD;
	}
	
	public boolean isNewPackage() {
		return selectedPackageThingID == null;
	}
	
	public String getBuildTarget() {
		return buildTarget;
	}
	public void setBuildTarget(String buildTarget) {
		this.buildTarget = buildTarget;
	}
	
	public String getBuildFile() {
		return buildFile;
	}
	
	public void setBuildFile(String buildFile) {
		this.buildFile = buildFile;
	}
	
	public String getBuildDirectory() {
		return buildDirectory;
	}
	
	public void setBuildDirectory(String buildDirectory) {
		this.buildDirectory = buildDirectory;
	}
	
	public String getBuildSystem() {
		return buildSystem;
	}
	
	public void setBuildSystem(String buildSystem) {
		this.buildSystem = buildSystem;
	}
	
	public String getPackageVersion() {
		return packageVersion;
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
