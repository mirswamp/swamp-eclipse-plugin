package eclipseplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.apache.commons.io.*;

public class ClasspathHandler {

	Set<IPath> paths;
	Set<File> files;
	IClasspathEntry oldEntries[]; // old classpath entries
	List<IClasspathEntry> newEntries;
	int index;
	File targetDir;
	IJavaProject project;
	private static String PROJECT_ROOT;
	
	public ClasspathHandler(IJavaProject root, String path) {
		index = 0;
		project = root;
		paths = new HashSet<IPath>();
		oldEntries = null;
		newEntries = new ArrayList<IClasspathEntry>();
		
		PROJECT_ROOT = root.getElementName();
		System.out.println("PROJECT ROOT: " + PROJECT_ROOT);
		try {
			oldEntries = project.getResolvedClasspath(true);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (oldEntries == null) {
			return;
		}
		targetDir = new File(path + "/.eclipsepluginbin");
		
		try {
			FileUtils.forceMkdir(targetDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*if (!targetDir.mkdirs()) {
			// TODO This is a bad error that'll pretty much stop us in our tracks
		}
		*/
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
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_SOURCE) {
				newEntries.add(entry);
			}
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				handleLibrary(entry);
			}
			else  {//(kind == IClasspathEntry.CPE_PROJECT)
				// TODO handle project function
			}
		}
		setProjectClasspath(newEntries);
		
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
	
	private void handleProject(IClasspathEntry entry) {
		
	}
	
	private void handleLibrary(IClasspathEntry entry) {
		IClasspathEntry newEntry;
		if (isInRootDirectory(entry)) {
			System.out.println("This entry is in root directory: " + entry.getPath());
			newEntry = entry;
		}
		else {
			newEntry = copyIntoDirectory(entry);
			if (newEntry == null) {
				return;
			}
		}
		newEntries.add(newEntry);
	}

	private boolean isInRootDirectory(IClasspathEntry entry) {
		return (entry.getPath().segment(0) == PROJECT_ROOT);
		/*
		Path p = ClasspathHandler.getPathFromIPath(entry.getPath());
		System.out.println("Old path: " + p);
		Path dirPath = targetDir.toPath();//.toRealPath(null);
		System.out.println("Dir path:" + dirPath);
		return p.toString().contains(dirPath.toString());
		*/
	}
	
	private static Path getPathFromIPath(IPath p) {
		System.out.println(p);
		System.out.println(p.toFile());
		System.out.println(p.toFile().toPath());
		return p.toFile().toPath();
	}
	
	public void revertClasspath() {
		try {
			FileUtils.deleteDirectory(targetDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		try {
			project.setRawClasspath(oldEntries, null);
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
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
		File f = new File(targetDir + "/" + lastSegment);
		Path dest = f.toPath();
		try {
			System.out.println("Written to destination: " + dest);
			Files.copy(src, dest);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IPath newPath = new org.eclipse.core.runtime.Path(dest.toString());
		return JavaCore.newLibraryEntry(newPath, newPath, null);
	}
	
	public Set<File> getCreatedFiles() {
		return files;
	}
	
}
