package eclipseplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class CycleDetector {
	
	public boolean foundCycle;
	
	// Generate adjacency list from root IJavaProject and then look for cycles
	public boolean projectHasCycle(IJavaProject root) {
		List<ArrayList<Integer>> adjList = new ArrayList<ArrayList<Integer>>();
		Set<Integer> visitedVertices = new HashSet<Integer>();
		visitedVertices.add(0);
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(root.getPath().makeAbsolute().toString(), 0);
		generateDigraphFromIJavaProject(root, 0, 1, map, adjList, visitedVertices);
		return digraphHasCycle(adjList, visitedVertices.size()); 
	}
	
	private boolean digraphHasCycle(List<ArrayList<Integer>> adjacencyList, int numVertices) {
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
	
	public static void generateDigraphFromIJavaProject(IJavaProject root, int vertex, int vertexCount, Map<String, Integer> map, List<ArrayList<Integer>> adjacencyList, Set<Integer> visited) {
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
				String path = proj.getFullPath().makeAbsolute().toString();
				System.out.println("Key: " + path);
				//IPath path = proj.getFullPath();
				int v;
				if (map.containsKey(path)) {
					v = map.get(path);
				}
				else {
					v = vertexCount++;
					map.put(path, v);
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
}
