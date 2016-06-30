package eclipseplugin;

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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.apache.commons.io.*;

public class ClasspathHandler {

	private Map<IPath, IClasspathEntry> cache;
	private IClasspathEntry oldEntries[]; // old classpath entries
	private List<IClasspathEntry> newEntries;
	private File targetDir;
	private IJavaProject project;
	private List<ClasspathHandler> dependentProjects;
	private static String PROJECT_ROOT;
	private ClasspathHandler root;
	private boolean foundCycle;
	
	public ClasspathHandler(ClasspathHandler root, IJavaProject projectRoot, String path) {
		project = projectRoot;
		oldEntries = null;
		newEntries = new ArrayList<IClasspathEntry>();
		dependentProjects = null;
		cache = null;
		foundCycle = false;
		
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
			if (projectHasCycle(projectRoot)) {
				System.err.println("There are cyclic dependencies preventing this project from being built");
				System.err.println("Please remove all cycles before resubmitting.");
				// throw some sort of cyclic dependency exception
				return;
			}
			dependentProjects = new ArrayList<ClasspathHandler>();
			cache = new HashMap<IPath, IClasspathEntry>();
			setupTargetDirectory(path);
			this.root = this;
		}
		else {
			this.root = root;
		}

		for (IClasspathEntry entry : oldEntries) {
			System.out.println(entry.getPath());
			// for each entry check if it's outside of our current directory
			// if so, we'll need to (1) copy it into current directory and (2) modify the classpath of the project accordingly
			// CPE_SOURCE - this will be in the proper directory already
			// CPE_LIBRARY - this will have an absolute path to the binary/jar
			// CPE_PROJECT - this is a required project, this will have an absolute path to the project - recursively look at the classpath for this
			// CPE_VARIABLE - we'll need to use JavaCore.getResolvedClasspathEntry(entry) to give us either a CPE_LIBRARY or CPE_PROJECT
			// CPE_CONTAINER - lol this is a container which can contain a bunch of the other 3
			// make a directory named eclipse_swamp/lib
			// TODO accomodate access rules, inclusion patterns, extra attributes, etc. - basically just copy over as much other info as possible from the original entry
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_SOURCE) {
				newEntries.add(entry);
			}
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				handleLibrary(entry);
			}
			else if (kind == IClasspathEntry.CPE_CONTAINER) {
				try {
					// TODO Question: Is it safe to not ship system libraries? Talk to Vamshi about this in the future
					IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
					for (IClasspathEntry subEntry : container.getClasspathEntries()) {
						if (subEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
							handleLibrary(subEntry);
						}
						else { // subEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT
							handleProject(subEntry);
						}
					}
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (kind == IClasspathEntry.CPE_VARIABLE) {
				handleVariable(entry);
			}
			else {//(kind == IClasspathEntry.CPE_PROJECT)
				handleProject(entry);
			}
		}
		ClasspathHandler.listEntries("Resolved entries", this.oldEntries);
		setProjectClasspath(newEntries);
		
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
	
	public boolean hasCycles() {
		return foundCycle;
	}
	
	public void setupTargetDirectory(String path) {
		targetDir = new File(path + "/.swampbin");
		if (targetDir.exists()) {
			try {
				FileUtils.deleteDirectory(targetDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (!targetDir.mkdirs()) {
			System.err.println("Here is a huge problem!");
			System.err.println("We were unable to make the directories");
			// TODO This is a bad error that'll pretty much stop us in our tracks
		}
		else {
			System.out.println("Created directory successfully");
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
	
	// Generate adjacency list from root IJavaProject and then look for cycles
	public boolean projectHasCycle(IJavaProject root) {
		List<ArrayList<Integer>> adjList = new ArrayList<ArrayList<Integer>>();
		Set<Integer> visitedVertices = new HashSet<Integer>();
		Map<IPath, Integer> map = new HashMap<IPath, Integer>();
		ClasspathHandler.generateDigraphFromIJavaProject(root, 0, 1, map, adjList, visitedVertices);
		return digraphHasCycle(adjList, visitedVertices.size()); 
	}
	
	public boolean digraphHasCycle(List<ArrayList<Integer>> adjacencyList, int numVertices) {
		boolean[] visited = new boolean[numVertices];
		boolean[] completed = new boolean[numVertices];
		search(0, adjacencyList, visited, completed);
		return foundCycle;
	}
	
	private void search(int vertex, List<ArrayList<Integer>> adjList, boolean[] visited, boolean[] completed) {
		visited[vertex] = true;
		for (Integer i : adjList.get(vertex)) {
			if (!visited[i]) {
				search(i, adjList, visited, completed);
				//return search(i, adjList, visited, completed);
			}
			else {
				if (!completed[i]) {
					foundCycle = true;
					return; //true; // this means we have a cycle
				}
			}
			completed[i] = true;
		}
	}
	
	public static void generateDigraphFromIJavaProject(IJavaProject root, int vertex, int vertexCount, Map<IPath, Integer> map, List<ArrayList<Integer>> adjacencyList, Set<Integer> visited) {
		//Map<IPath, Integer> map = new HashMap<IPath, Integer>();
		adjacencyList.add(vertex, new ArrayList<Integer>());
		List<IProject> projects = new ArrayList<IProject>();
		try {
			for (IClasspathEntry e : root.getRawClasspath()) {
				int kind = e.getEntryKind();
				IProject p = null;
				if (kind == IClasspathEntry.CPE_PROJECT) {
					p = ClasspathHandler.convertEntryToProject(root.getProject(), e);
					if (p != null) {
						projects.add(p);
					}
				}
				else if (kind == IClasspathEntry.CPE_VARIABLE) {
					IClasspathEntry resolvedEntry = e.getResolvedEntry();
					if (resolvedEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
						p = ClasspathHandler.convertEntryToProject(root.getProject(), resolvedEntry);
						if (p != null) {
							projects.add(p);
						}
					}
				}
				else if (kind == IClasspathEntry.CPE_CONTAINER) {
					IClasspathContainer container = null;
					try {
						container = JavaCore.getClasspathContainer(e.getPath(), root);
					} catch (JavaModelException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					for (IClasspathEntry subEntry : container.getClasspathEntries()) {
						if (subEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
							IProject proj = ClasspathHandler.convertEntryToProject(root.getProject(), subEntry);
							if (proj != null) {
								projects.add(proj);
							}
						}
					}
				}
			}
				
			for (IProject proj : projects) {
				IPath path = proj.getFullPath();
				int v;
				if (map.containsKey(path)) {
					v = map.get(path);
				}
				else {
					v = vertexCount++;
				}
				adjacencyList.get(vertex).add(v);
				if (!visited.contains(v)) {
					visited.add(v);
					generateDigraphFromIJavaProject(JavaCore.create(proj), v, vertexCount, map, adjacencyList, visited);
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Set<IJavaProject> getProjectList() {
		Set<IJavaProject> set = new HashSet<IJavaProject>();
		for (ClasspathHandler c : dependentProjects) {
			set.add(c.project);
		}
		return set;
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
	
	public boolean isEmpty() {
		return this.oldEntries == null;
	}
	
	private void setProjectClasspath(List<IClasspathEntry> list) {
		try {
			project.setRawClasspath(ClasspathHandler.convertEntryListToArray(list), null);
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
	
	private void handleProject(IClasspathEntry entry) {
		IProject project = ClasspathHandler.convertEntryToProject(this.project.getProject(), entry);
		if (project != null) {
			ClasspathHandler cph = new ClasspathHandler(this.root, JavaCore.create(project), entry.getPath().toString());
			this.root.addDependentProject(cph);
		}
	}
	
	public boolean containsClasspathEntry(IPath path) {
		return cache.containsKey(path);
	}
	
	public void addClasspathEntry(IPath path, IClasspathEntry entry) {
		cache.put(path, entry);
	}
	
	private void handleLibrary(IClasspathEntry entry) {
		IClasspathEntry newEntry;
		if (isInRootDirectory(entry)) {
			System.out.println("This entry is in root directory: " + entry.getPath());
			newEntry = entry;
		}
		else {
			IPath path = entry.getPath().makeAbsolute();
			if (this.root.containsClasspathEntry(path)) {
				newEntry = this.root.cache.get(path);
			}
			else {
				newEntry = copyIntoDirectory(entry);
				this.root.addClasspathEntry(path, newEntry);
			}
			if (newEntry == null) {
				return;
			}
		}
		newEntries.add(newEntry);
	}

	private boolean isInRootDirectory(IClasspathEntry entry) {
		return (entry.getPath().segment(0) == PROJECT_ROOT);
	}
	
	private static Path getPathFromIPath(IPath p) {
		return p.toFile().toPath();
	}
	
	public void revertClasspath() {
		for (ClasspathHandler c : dependentProjects) {
			try {
				c.project.setRawClasspath(c.oldEntries, null);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			FileUtils.deleteDirectory(targetDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			project.setRawClasspath(oldEntries, null);
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private IClasspathEntry copyIntoDirectory(IClasspathEntry entry) {
		// add to new entries, files
		// TODO Worry about softlinks
		// TODO Instead of just doing lastSegment, do the entire path and replace separator (e.g. "/") with some other char (e.g. "-")
		// get path from the entry
		IPath path = entry.getPath();
		IPath sourceAttachmentPath = entry.getSourceAttachmentPath();
		if (sourceAttachmentPath != null) {
			System.out.println("Source attachment path: " + sourceAttachmentPath);
		}
		String lastSegment = path.lastSegment();
		Path src = ClasspathHandler.getPathFromIPath(path);
		if (!src.toFile().exists()) {
			// TODO Some serious logging
			System.out.println("Classpath entry points to non-existent file: " + src);
			return null;
		}
		File f = new File(targetDir.getAbsolutePath() + "/" + lastSegment);
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
