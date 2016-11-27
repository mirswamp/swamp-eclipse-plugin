package org.continuousassurance.swamp.eclipse;

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;
import static org.eclipse.core.runtime.IPath.SEPARATOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import static org.eclipse.core.runtime.Path.SEPARATOR;

public class ImprovedClasspathHandler {
	private static final String SOURCE_VERSION_OPTION = "org.eclipse.jdt.core.compiler.source";
	private static final String TARGET_VERSION_OPTION = "org.eclipse.jdt.core.compiler.codegen.targetPlatform";
	public static final String SWAMPBIN_DIR = "swampbin";
	private static final char DASH = '-';
	private IPath SWAMPBIN_PATH = null;
	
	// TODO Consider making these sets of entries instead of Lists
	private List<IClasspathEntry> sources = null;
	private List<IClasspathEntry> libs = null;
	private List<IClasspathEntry> systemLibs = null;
	private List<IClasspathEntry> exportedEntries = null; 
	private List<ImprovedClasspathHandler> dependentProjects = null;
	private Map<String, ImprovedClasspathHandler> visitedProjects = null;
	private ImprovedClasspathHandler root = null;
	private Set<String> filesToArchive = null;
	private boolean excludeSysLibs = false;
	private boolean hasSwampbinDependencies = false;
	private IJavaProject project;
	private String srcVersion;
	private String targetVersion;
	private SubMonitor subMonitor;
	
	public  ImprovedClasspathHandler(IJavaProject project, ImprovedClasspathHandler root, boolean exclSysLibs, SubMonitor subMonitor) {
		this.excludeSysLibs = exclSysLibs;
		sources = new ArrayList<IClasspathEntry>();
		libs = new ArrayList<IClasspathEntry>();
		systemLibs = new ArrayList<IClasspathEntry>();
		dependentProjects = new ArrayList<ImprovedClasspathHandler>();
		exportedEntries = new ArrayList<IClasspathEntry>();
		
		this.project = project;
		this.srcVersion = this.project.getOption(SOURCE_VERSION_OPTION, true);
		this.targetVersion = this.project.getOption(TARGET_VERSION_OPTION, true);
		
		if (root == null) {
			this.root = this;
			this.subMonitor = subMonitor;
			this.subMonitor.setWorkRemaining(100);
			visitedProjects = new HashMap<String, ImprovedClasspathHandler>();
			SWAMPBIN_PATH = setupBinDir(project.getProject());
			filesToArchive = new HashSet<String>();
		}
		else {
			this.root = root;
			visitedProjects = root.visitedProjects;
			SWAMPBIN_PATH = root.SWAMPBIN_PATH;
			filesToArchive = root.filesToArchive;
		}
		
		try {
			project.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			System.err.println("Unable to do a clean build on the project for some reason");
			e1.printStackTrace();
		}
		
		IClasspathEntry[] entries = null;
		try {
			entries = project.getRawClasspath();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		try {
		for (IClasspathEntry entry : entries) {
			int kind = entry.getEntryKind();
			if (this.subMonitor != null) {
				if (this.subMonitor.isCanceled()) {
					System.out.println("Sub monitor got cancelled!");
				}
				this.subMonitor.split(100 / SwampSubmitter.CLASSPATH_ENTRY_TICKS);
			}
			if (kind == IClasspathEntry.CPE_SOURCE) {
				handleSource(entry, wsRoot);
			}
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				handleLibrary(entry, wsRoot);
			}
			else if (kind == IClasspathEntry.CPE_PROJECT) {
				handleProject(entry, wsRoot);
			}
			else if (kind == IClasspathEntry.CPE_VARIABLE) {
				handleVariable(entry, wsRoot);
			}
			else { // kind == IClasspathEntry.CPE_CONTAINER
				handleContainer(entry, wsRoot);
			}
		}
		} catch (IOException | JavaModelException e) {
			// TODO Report this error! This is very bad
			e.printStackTrace();
		}
		if (hasSwampbinDependencies) {
			filesToArchive.add(SWAMPBIN_PATH.toOSString());
		}
	}
	
	private IPath setupBinDir(IProject project) {
		// make SWAMPBIN directory
		String path = project.getWorkingLocation(PLUGIN_ID).toOSString() + SEPARATOR + SWAMPBIN_DIR;
		File f = new File(path);
		if (f.exists()) {
			System.out.println("SWAMPBIN already exists but we should be deleting it now!");
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		f.mkdir();
		return new org.eclipse.core.runtime.Path(path);
	}
	
	private void handleSource(IClasspathEntry entry, IWorkspaceRoot root) {
		// Each entry either has an associated Output Location or goes to the project's default output location
		// Associated output locations are also just absolute paths (e.g. /MalcolmsProject/bin) just like sources
		// We'll need to mkdir all of these output locations but that shouldn't be hard
		//System.out.println("Source absolute path: " + entry.getPath());
		//System.out.println("Associated output location: " + entry.getOutputLocation());
		sources.add(entry);
		IFile file = root.getFile(entry.getPath());
		IProject project = file.getProject();
		IPath projectPath = project.getLocation();
		System.out.println("Source location: " + projectPath.toOSString());
		filesToArchive.add(projectPath.toOSString());
	}
	
	private void handleLibrary(IClasspathEntry entry, IWorkspaceRoot root) throws IOException {
		// 3 types of library entries: internal (this project), internal (another project), and external
		// (1) Rooted absolute path
		// (2) Rooted absolute path (but with different project directory) --> Need to copy this to swampbin
		// (3) Actual absolute path to somewhere else on the filesystem --> Need to copy this to swampbin
		System.out.println("\n\n\n\n");
		System.out.println("Library absolute path: " + entry.getPath().makeAbsolute());
		//System.out.println("First segment: " + entry.getPath().segment(0));
		IFile file = root.getFile(entry.getPath());
		System.out.println("File project: " + file.getProject().getName());
		
		if (file.getProject().equals(this.project.getProject())) {
			System.out.println("Is inside project");
			libs.add(entry);
		}
		else {
			System.out.println("Is outside project");
			IFile libFile = root.getFile(entry.getPath());
			IProject libProject = libFile.getProject();
			String filename = getLibraryFileName(entry.getPath());
			IPath destPath;
			if (libProject.exists()) {
				if (libProject.isOpen()) {
					try {
						System.out.println("Local project");
						destPath = copyWorkspacePathIntoBinDir(libFile, filename, SWAMPBIN_PATH);
					}
					catch (Exception e) {
						System.out.println("Local project that failed");
						String srcPath = getProjectLibraryLocation(libProject, entry.getPath());
						destPath = copyAbsolutePathIntoBinDir(srcPath, filename, SWAMPBIN_PATH);
					}
				}
				else {
					System.out.println("Local project that's closed");
					String srcPath = getProjectLibraryLocation(libProject, entry.getPath());
					destPath = copyAbsolutePathIntoBinDir(srcPath, filename, SWAMPBIN_PATH);
				}
			}
			else {
				System.out.println("Not a project - just an absolute path");
				destPath = copyAbsolutePathIntoBinDir(entry.getPath().toOSString(), filename, SWAMPBIN_PATH);
			}
			hasSwampbinDependencies = true;
			System.out.println("SWAMPBIN path: " + destPath);
			IClasspathEntry newEntry = JavaCore.newLibraryEntry(destPath, entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath());
			System.out.println("New entry path: " + newEntry.getPath());
			libs.add(newEntry);
			if (entry.isExported()) {
				exportedEntries.add(newEntry);
			}
		}	
	}
	
	private void handleProject(IClasspathEntry entry, IWorkspaceRoot root) {
		String path = entry.getPath().toOSString();
		ImprovedClasspathHandler ich;
		if (visitedProjects.containsKey(path)) {
			ich = visitedProjects.get(path);
			dependentProjects.add(ich);
		}
		else {
			IProject project = root.getProject(entry.getPath().toOSString());
			ich = new ImprovedClasspathHandler(JavaCore.create(project), this.root, this.root.excludeSysLibs, null);
			dependentProjects.add(ich);
			visitedProjects.put(path, ich);
		}
		for (IClasspathEntry e : ich.getExportedEntries()) {
			this.libs.add(e);
			if (entry.isExported()) {
				this.exportedEntries.add(e);
			}
		}
	}
	
	private void handleVariable(IClasspathEntry entry, IWorkspaceRoot root) throws IOException {
		IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(entry);
		if (resolvedEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
			handleLibrary(resolvedEntry, root);
		}
		else {
			handleProject(resolvedEntry, root);
		}
	}
	
	public void handleContainer(IClasspathEntry entry, IWorkspaceRoot root) throws IOException, JavaModelException {
		IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
		System.out.println("Here's a container" + container);
		int kind = container.getKind();
		if ((this.excludeSysLibs) && (kind == IClasspathContainer.K_APPLICATION || kind == IClasspathContainer.K_DEFAULT_SYSTEM)) {
			System.out.println("System library container");
			System.out.println(entry.getPath());
			return;
		}
		for (IClasspathEntry subEntry : container.getClasspathEntries()) {
			if (subEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				handleLibrary(subEntry, root);
			}
			else { 
				handleProject(subEntry, root);
			}
		}
	}
	
	public void deleteSwampBin() throws IOException {
		File f = SWAMPBIN_PATH.toFile();
		if (f.exists()) {
			FileUtils.forceDelete(f);
		}
	}
	
	// TODO THIS NEEDS SERIOUS, INDUSTRIAL-STRENGTH TESTING - 
	// The assumption that it's making is that a project in our workspace is always a top-level directory in the root
	// (i.e. that entryPath.removeFirstSegments(1) is a valid project name
	private static String getProjectLibraryLocation(IProject project, IPath entryPath) {
		String projectDir = project.getLocation().toOSString();
		String srcFile = projectDir + SEPARATOR + entryPath.removeFirstSegments(1);
		return srcFile;
	}
	
	private static IPath copyWorkspacePathIntoBinDir(IFile file, String filename, IPath swampBinPath) throws CoreException {
		String filePath = swampBinPath.toOSString() + SEPARATOR + filename;
		IPath destPath = new org.eclipse.core.runtime.Path(filePath);
		file.copy(destPath, true, null);
		//return destPath;
		return new org.eclipse.core.runtime.Path(SEPARATOR + SWAMPBIN_DIR + SEPARATOR + filename);
	}
	
	
	private static IPath copyAbsolutePathIntoBinDir(String srcStr, String filename, IPath swampBinPath) throws IOException {
		String destStr = swampBinPath.toOSString() + SEPARATOR + filename;
		Path destPath = new File(destStr).toPath();
		Path srcPath = new File(srcStr).toPath();
		// This copy needs to be forced to disk
		System.out.println("Path we're reading from: " + srcPath);
		byte[] bytes = Files.readAllBytes(srcPath);
		System.out.println("Length of file in bytes: " + bytes.length);
		OpenOption options[] = {StandardOpenOption.DSYNC , StandardOpenOption.CREATE , StandardOpenOption.WRITE};
		System.out.println("Path we're writing to: " + destPath);
		Files.write(destPath, bytes, options);
		IPath path = new org.eclipse.core.runtime.Path(destStr);
		//return path;
		return new org.eclipse.core.runtime.Path(SEPARATOR + SWAMPBIN_DIR + SEPARATOR + filename);
	}
	
	private static String getLibraryFileName(IPath path) {
		String strPath = path.toOSString();
		if (strPath.charAt(0) == IPath.SEPARATOR) {
			strPath = strPath.substring(1);
		}
		strPath = strPath.replace(IPath.SEPARATOR, DASH);
		strPath = strPath.replace(IPath.DEVICE_SEPARATOR, DASH);
		return strPath;
	}

	public Set<String> getFilesToArchive() {
		System.out.println("FILES TO ARCHIVE");
		for (String s : filesToArchive) {
			System.out.println(s);
		}
		return filesToArchive;
	}
	
	public String getProjectName() {
		return project.getProject().getName();
	}
	
	public String getSourceVersion() {
		return srcVersion;
	}
	
	public String getTargetVersion() {
		return targetVersion;
	}
	
	public List<IClasspathEntry> getLibraryClasspath() {
		return libs;
	}
	
	public List<IClasspathEntry> getSystemLibraryClasspath() {
		return systemLibs;
	}
	
	public List<IClasspathEntry> getSourceClasspath() {
		return sources;
	}
	
	public List<IClasspathEntry> getExportedEntries() {
		return exportedEntries;
	}
	
	public IPath getDefaultOutputLocation() {
		try {
			return project.getOutputLocation();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String getProjectPluginLocation() {
		return root.project.getProject().getWorkingLocation(PLUGIN_ID).toOSString();
	}
	
	public List<ImprovedClasspathHandler> getDependentProjects() {
		return dependentProjects;
	}
	
	public boolean isRoot() {
		return true;
	}
	
	public static void getReferencedProjects(IJavaProject jp, Set<IProject> projectSet) {
		System.out.println("Java project: " + jp);
		System.out.println("Project set: " + projectSet);
		projectSet.add(jp.getProject());
		try {
			IProject[] prjArray = jp.getProject().getReferencedProjects();
			for (IProject p : prjArray) {
				getReferencedProjects(JavaCore.create(p), projectSet);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
