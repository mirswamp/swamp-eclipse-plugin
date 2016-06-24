package eclipseplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class ClasspathHandler {

	Set<IPath> paths;
	Set<File> files;
	IClasspathEntry oldEntries[]; // old classpath entries
	IClasspathEntry newEntries[];
	int index;
	File targetDir;
	
	public ClasspathHandler(IJavaProject root, String path) {
		index = 0;
		paths = new HashSet<IPath>();
		IClasspathEntry entries[] = null;
		
		try {
			oldEntries = root.getRawClasspath();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		targetDir = new File(path + "/.eclipsepluginbin");
		for (IClasspathEntry entry : entries) {
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
				newEntries[index++] = entry;
			}
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				handleLibrary(entry);

			}
			else if (kind == IClasspathEntry.CPE_PROJECT) {
				// TODO handle project function
			}
			else if (kind == IClasspathEntry.CPE_VARIABLE) {
				// handle variable function
				handleVariable(entry);
			}
			else { // kind == IClasspathEntry.CPE_CONTAINER
				// TODO handle container function
			}
			
		}
	}
	
	private void handleVariable(IClasspathEntry entry) {
		IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(entry);
		if (resolvedEntry == null) {
			return;
		}
		if (resolvedEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
			handleProject(resolvedEntry);
		}
		else {
			handleLibrary(entry);
		}
	}
	
	private void handleProject(IClasspathEntry entry) {
		
	}
	
	private void handleLibrary(IClasspathEntry entry) {
		IClasspathEntry newEntry;
		if (isInRootDirectory(entry)) {
			newEntry = entry;
		}
		else {
			newEntry = copyIntoDirectory(entry);
			if (newEntry == null) {
				return;
			}
		}
		newEntries[index++] = newEntry;
	}

	private void handleContainer(IClasspathEntry entry) {
		
	}
	
	private boolean isInRootDirectory(IClasspathEntry entry) {
		Path p = ClasspathHandler.getPathFromIPath(entry.getPath());
		Path dirPath;
		try {
			dirPath = targetDir.toPath().toRealPath(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return dirPath.toString().contains(p.toString());
	}
	
	private static Path getPathFromIPath(IPath p) {
		return p.toFile().toPath();
	}
	
	public void revertClasspath() {
		for (IClasspathEntry e : newEntries) {
			// delete this entry
		}
		targetDir.delete();
	}
	
	private IClasspathEntry copyIntoDirectory(IClasspathEntry entry) {
		// add to new entries, files
		// get path from the entry
		IPath path = entry.getPath();
		String lastSegment = path.lastSegment();
		Path src = ClasspathHandler.getPathFromIPath(path);
		File f = new File(targetDir + "/" + lastSegment);
		Path dest = f.toPath();
		try {
			Files.copy(src, dest);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IPath newPath = new org.eclipse.core.runtime.Path(dest.toString());
		return JavaCore.newLibraryEntry(path, newPath, null);
	}
	
	public Set<File> getCreatedFiles() {
		return files;
	}
	
}
