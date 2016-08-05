package eclipseplugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.apache.commons.io.*;
import static org.eclipse.core.runtime.Path.SEPARATOR;

public class ClasspathHandler {

	private Map<String, IClasspathEntry> entryCache;
	private IClasspathEntry oldEntries[]; // old classpath entries
	private List<IClasspathEntry> libEntries;
	private List<IClasspathEntry> srcEntries;
	private File targetDir; // swamp bin directory
	private String projectPath;
	private IJavaProject project;
	private List<ClasspathHandler> dependentProjects;
	private Set<String> projectsVisited; // this is so an individual ClasspathHandler doesn't add the same project entry twice
	private static String PROJECT_ROOT;
	private ClasspathHandler root;
	private String path;
	private boolean hasCycles;
	private boolean includeSystemLibraries;
	private Map<String, ClasspathHandler> projectCache; // this is so we only have to visit referenced projects once regardless of how many times they've been referenced. We always look at root.
	
	private static String BIN_DIR = ".swampbin";
	private static String PACKAGE_DIR = "package";
	
	public ClasspathHandler(ClasspathHandler root, IJavaProject projectRoot, String path, boolean includeSysLibs) {
		project = projectRoot;
		oldEntries = null;
		libEntries = new ArrayList<IClasspathEntry>(); // library entries
		srcEntries = new ArrayList<IClasspathEntry>(); // source entries
		dependentProjects = null;
		projectsVisited = null;
		projectPath = null;
		entryCache = null;
		includeSystemLibraries = includeSysLibs;
		
		PROJECT_ROOT = projectRoot.getElementName();
		System.out.println("PROJECT ROOT: " + PROJECT_ROOT);
		try {
			oldEntries = project.getRawClasspath();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (oldEntries == null) {
			return;
		}
		if (root == null) {
			CycleDetector cd = new CycleDetector();
			if (cd.projectHasCycle(projectRoot)) {
				hasCycles = true;
				System.err.println("There are cyclic dependencies preventing this project from being built");
				System.err.println("Please remove all cycles before resubmitting.");
				return;
			}
			this.root = this;
			this.path = path + SEPARATOR + PACKAGE_DIR;
			setupDirectory(this.path);
			targetDir = setupDirectory(this.path + SEPARATOR + BIN_DIR);

			IProject newProject = setupProject(projectRoot.getProject(), this.oldEntries);
			this.project = JavaCore.create(newProject);
			this.projectCache = new HashMap<String, ClasspathHandler>();
			entryCache = new HashMap<String, IClasspathEntry>();
		}
		else {
			this.root = root;
		}
		dependentProjects = new ArrayList<ClasspathHandler>();
		projectsVisited = new HashSet<String>();
		// TODO Is project set appropriately at this point? --> It should be
		projectPath = this.root.path + SEPARATOR + this.project.getProject().getName();

		for (IClasspathEntry entry : oldEntries) {
			System.out.println(entry.getPath());
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_SOURCE) {
				handleSource(entry);
			}
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				handleLibrary(entry);
			}
			else if (kind == IClasspathEntry.CPE_CONTAINER) {
				handleContainer(entry);
			}
			else if (kind == IClasspathEntry.CPE_VARIABLE) {
				handleVariable(entry);
			}
			else {//(kind == IClasspathEntry.CPE_PROJECT)
				handleProject(entry);
			}
		}
		ClasspathHandler.listEntries("Resolved entries", this.oldEntries);
		ClasspathHandler.listEntries("New entries", libEntries);
		setProjectClasspath(libEntries);
		
	}
	
	public String getRootPath() {
		return this.root.path;
	}
	
	public boolean hasCycles() {
		return hasCycles;
	}
	
	private File setupDirectory(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		try {
			FileUtils.forceMkdir(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Here is a huge problem!");
			System.err.println("We were unable to make the directories");
		}
		return file;
	}
	
	public List<IClasspathEntry> getLibraryClasspath() {
		return this.libEntries;
	}
	
	public List<IClasspathEntry> getSourceClasspath() {
		return this.srcEntries;
	}
	
	public String getProjectPath() {
		return this.projectPath;
	}

	private static void copyDirectory(String srcPath, String destPath) {
		File src = new File(srcPath);
		File dest = new File(destPath);
		try {
			FileUtils.copyDirectory(src, dest);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getProjectName() {
		return this.project.getProject().getName();
	}
	
	public String getOutputLocation() {
		try {
			return this.root.project.getOutputLocation().toString();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getSwampBinPath() {
		return this.root.targetDir.getAbsolutePath();
	}
	
	public void handleContainer(IClasspathEntry entry) {
		// TODO Question: Is it safe to not ship system libraries? Talk to Vamshi about this in the future
		try {
			IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
			int kind = container.getKind();
			if ((!this.includeSystemLibraries) && (kind == IClasspathContainer.K_APPLICATION || kind == IClasspathContainer.K_DEFAULT_SYSTEM)) {
				System.out.println("System library container");
				System.out.println(entry.getPath());
				return;
			}
			for (IClasspathEntry subEntry : container.getClasspathEntries()) {
				if (subEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					handleLibrary(subEntry);
				}
				else { 
					handleProject(subEntry);
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void handleSource(IClasspathEntry entry) {
		String newSrcPath;
		String entryPath = entry.getPath().toString();
		
		StringBuffer sb = new StringBuffer(entryPath);
		sb.insert(1, ".");
		newSrcPath = this.root.path + sb.toString(); 
		IPath srcIPath = new org.eclipse.core.runtime.Path(newSrcPath);
		IPath outputIPath = entry.getOutputLocation();
		IClasspathEntry newEntry = JavaCore.newSourceEntry(srcIPath, entry.getInclusionPatterns(), entry.getExclusionPatterns(), outputIPath );	
		
		System.out.println("Original OutputPath: " + outputIPath);
		try {
			System.out.println("Default OutputPath: " + this.project.getOutputLocation());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		srcEntries.add(newEntry);
	}
	
	public boolean isRoot() {
		return this == this.root;
	}
	
	public void handleVariable(IClasspathEntry entry) {
		IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(entry);
		if (resolvedEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
			handleLibrary(resolvedEntry);
		}
		else {
			handleProject(resolvedEntry);
		}
	}
	
	public static IProject convertEntryToProject(IProject root, IClasspathEntry entry) {
		// Get the IProject from the IJavaProject
		IProject[] projArray = null;
		try {
			projArray = root.getReferencedProjects();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			System.err.println("Make sure all of the referenced projects are open");
			e.printStackTrace();
		}
		// Call getReferencedProjects on IProject to get an array of IProjects
		if (projArray != null) {
			for (IProject p : projArray) {
				System.out.println("Entry path: " + entry.getPath());
				System.out.println("Full path: " + p.getFullPath());
				System.out.println("Entry absolute path: " + entry.getPath().makeAbsolute());
				System.out.println("Full absolute path: " + p.getFullPath().makeAbsolute());
				// Check whether one of those has the same path as this entry
				if (entry.getPath().makeAbsolute().equals(p.getFullPath().makeAbsolute())) { // check whether entry absolute path and full absolute path will match
					return p;
				}
			}
		}
		return null;
	}
	

	
	public List<ClasspathHandler> getDependentProjects() {
		return this.dependentProjects;
	}
	
	private static void listEntries(String category, IClasspathEntry[] array) {
		System.out.println("-----------------------------------------------------");
		System.out.println(category);
		System.out.println(array.length);
		for (IClasspathEntry entry : array) {
			System.out.println(entry);
		}
		System.out.println("-----------------------------------------------------");
	}
	
	private static void listEntries(String category, List<IClasspathEntry> list) {
		System.out.println("-----------------------------------------------------");
		System.out.println(category);
		System.out.println(list.size());
		for (IClasspathEntry entry : list) {
			System.out.println(entry);
		}
		System.out.println("-----------------------------------------------------");
	}
	
	public boolean isEmpty() {
		return this.oldEntries == null;
	}
	
	private void setProjectClasspath(List<IClasspathEntry> list) {
		try {
			project.setRawClasspath(ClasspathHandler.convertEntryListToArray(list), null);
			listEntries("Set project classpath raw", project.getRawClasspath());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static IClasspathEntry[] convertEntryListToArray(List<IClasspathEntry> list) {
		IClasspathEntry[] array = new IClasspathEntry[list.size()];
		return list.toArray(array);
	}
	
	public void addDependentProject(ClasspathHandler cph) {
		dependentProjects.add(cph);
	}
	
	private IProject setupProject(IProject project, IClasspathEntry[] classpath) {
		IProjectDescription desc = null;
		URI uri = null;
		String destPath = null;
		String srcPath = null;
		String prjName = null;
		// (1) Copy the project directory to the new location
		try {
			desc = project.getDescription();
			uri = desc.getLocationURI();
			if (uri == null) {
				srcPath = project.getLocation().toString();
				if (srcPath == null) {
					System.err.println("Project not found on local file system");
					return null;
				}
			} 
			else {
				srcPath = uri.getPath();
			}
			prjName = project.getName();
			destPath = this.root.path + SEPARATOR + "." + prjName;
			System.out.println("Dest path: " + destPath);
			ClasspathHandler.copyDirectory(srcPath, destPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// (2) Create the new dot project in our workspace
		/* Code adapted from https://wiki.eclipse.org/FAQ_How_do_I_create_a_Java_project%3F */
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// We prepend a "." to the project so it doesn't conflict with names already in our workspace root
		// Since BuildFileCreator is goofy and uses the project name from the root rather than its actual location
		// the project needs to also be named with the "." prepended
		IProject newProject = root.getProject("." + prjName);
		try {
			newProject.delete(true, null);
			newProject.create(null);
			newProject.open(null);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			IProjectDescription newDesc = newProject.getDescription();
			newDesc.setNatureIds(desc.getNatureIds());
			IPath destIPath = new org.eclipse.core.runtime.Path(destPath);
			newDesc.setLocation(destIPath);
			newProject.setDescription(newDesc, null);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// (3) Set the proper output directory for the new project
		/* Not sure if this part is necessary */
		IJavaProject javaProj = null;
		try {
		javaProj = JavaCore.create(newProject);
		//String originalOutputLoc = this.project.getOutputLocation().toString();
		String originalOutputLoc = JavaCore.create(project).getOutputLocation().toString();
		System.out.println("Original output location: " + originalOutputLoc);
		String newOutputLoc = originalOutputLoc.replace(prjName, "." + prjName);
		System.out.println("New output location: " + newOutputLoc);
		//String relPath = BuildfileGenerator.makeRelative(newOutputLoc, "." + prjName);
		//String absPath = destPath + SEPARATOR + relPath;
		
		//IFolder binDir = newProject.getFolder("bin");
		//IPath binPath = binDir.getFullPath();
		//System.out.println("Bin path: " + binPath.toString());
		// TODO Remove this stuff
		//File f = new File(absPath);
		//FileUtils.forceMkdir(f);
		//System.out.println("Forced creation of directory " + absPath);
		
		IPath outputPath = new org.eclipse.core.runtime.Path(newOutputLoc);
		javaProj.setOutputLocation(outputPath, null);
		javaProj.setRawClasspath(classpath, true, null);
		} catch (Exception e1) {
			System.out.println("Started with a project, now we're here");
			e1.printStackTrace();
		}
		return newProject;
	}
	
	private void handleProject(IClasspathEntry entry) {
		IProject project = ClasspathHandler.convertEntryToProject(this.project.getProject(), entry);
		if (project == null) {
			// TODO add some error handling
			return;
		}
		String projectPath = project.getLocation().toString();
		IClasspathEntry prjEntry = null;
		System.out.println("Project path: " + projectPath);
		if (!this.projectsVisited.contains(projectPath)) {
			this.projectsVisited.add(projectPath);
			prjEntry = this.root.entryCache.get(projectPath);
			if (prjEntry == null) {
				IProject newProject = null;
				try {
					newProject = setupProject(project, JavaCore.create(project).getRawClasspath());
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (newProject != null) {
					ClasspathHandler cph = new ClasspathHandler(this.root, JavaCore.create(newProject), entry.getPath().toString(), this.includeSystemLibraries);
					//this.root.addDependentProject(cph);
					this.addDependentProject(cph);
					System.out.println("New project path: " + newProject.getFullPath());
					prjEntry = JavaCore.newProjectEntry(newProject.getFullPath()); // TODO Use constructor with more arguments to handle access rules and the like
					libEntries.add(prjEntry);
					this.root.entryCache.put(projectPath, prjEntry);
					this.root.projectCache.put(projectPath, cph);
				}
			}
			else {
				libEntries.add(prjEntry);
				this.addDependentProject(this.root.projectCache.get(projectPath));
			}
		}
	}
	
	private void handleLibrary(IClasspathEntry entry) {
		IClasspathEntry newEntry;
		if (isInRootDirectory(entry)) {
			System.out.println("This entry is in root directory: " + entry.getPath());
			newEntry = entry;
		}
		else {
			String strPath = entry.getPath().toOSString();
			if (this.root.entryCache.containsKey(strPath)) {
				newEntry = this.root.entryCache.get(strPath);
			}
			else {
				newEntry = copyIntoDirectory(entry);
				this.root.entryCache.put(strPath, newEntry);
			}
			if (newEntry == null) {
				return;
			}
		}
		libEntries.add(newEntry);
	}

	private boolean isInRootDirectory(IClasspathEntry entry) {
		// TODO Is there a better, more reliable way of checking for this?
		return (entry.getPath().segment(0) == PROJECT_ROOT);
	}
	
	private static Path getPathFromIPath(IPath p) {
		return p.toFile().toPath();
	}
	
	public void revertClasspath(IWorkspaceRoot root, Set<ClasspathHandler> visited) {
		if (visited.contains(this)) {
			return;
		}
		visited.add(this);
		for (ClasspathHandler c : this.dependentProjects) {
			c.revertClasspath(root, visited);
		}
		try {
			this.project.setRawClasspath(this.oldEntries, null);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			System.err.println("Java Model Status error");
			listEntries("Old entries for project " + this.getProjectName(), this.oldEntries);
			e.printStackTrace();
		}
		
		IProject project = root.getProject(this.getProjectName());
		if (project == null) {
			System.err.println("Project " + this.getProjectName() + " is null for some unexpected reason.");
			return;
		}

		// delete project from workspace
		try {
			project.delete(true, true, null);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			System.err.println("Unable to delete project" + this.getProjectName() + " properly.");
			e1.printStackTrace();
		}
		
		// delete actual project
		try {
			FileUtils.forceDelete(new File(this.getProjectPath()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// delete package directory
		if (this.isRoot()) {
			try {
				FileUtils.deleteDirectory(new File(this.root.path));
				//FileUtils.deleteDirectory(targetDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private IClasspathEntry copyIntoDirectory(IClasspathEntry entry) {
		// add to new entries, files
		// TODO Worry about softlinks
		// get path from the entry
		char DASH = '-';
		IPath path = entry.getPath();
		IPath sourceAttachmentPath = entry.getSourceAttachmentPath();
		if (sourceAttachmentPath != null) {
			System.out.println("Source attachment path: " + sourceAttachmentPath);
		}
		//String lastSegment = path.lastSegment();
		String strPath = path.toString();
		if (strPath.charAt(0) == IPath.SEPARATOR) {
			strPath = strPath.substring(1);
		}
		strPath = strPath.replace(IPath.SEPARATOR, DASH);
		strPath = strPath.replace(IPath.DEVICE_SEPARATOR, DASH);
		Path src = ClasspathHandler.getPathFromIPath(path);
		if (!src.toFile().exists()) {
			// TODO Some serious logging
			System.out.println("Classpath entry points to non-existent file: " + src);
			return null;
		}
		//File f = new File(this.root.targetDir.getAbsolutePath() + SEPARATOR + lastSegment);
		File f = new File(this.root.targetDir.getAbsolutePath() + SEPARATOR + strPath);
		Path dest = f.toPath();
		try {
			System.out.println("Written to destination: " + dest);
			// This copy needs to be forced to disk
			byte[] bytes;
			try {
				bytes = Files.readAllBytes(src);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			OpenOption options[] = {StandardOpenOption.DSYNC , StandardOpenOption.CREATE , StandardOpenOption.WRITE};
			Files.write(dest, bytes, options);
			//Files.copy(src, dest);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		IPath newPath = new org.eclipse.core.runtime.Path(dest.toString());
		return JavaCore.newLibraryEntry(newPath, newPath, null);
	}
	
	
}
