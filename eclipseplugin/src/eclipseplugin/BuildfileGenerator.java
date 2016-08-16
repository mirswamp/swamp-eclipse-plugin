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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.eclipse.core.runtime.Path.SEPARATOR;

/**
 * This class does the actual generation of the build files.
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class BuildfileGenerator {
	
	/**
	 * The name used for referencing the classpath for a project.
	 */
	private static String 	CLASSPATH_NAME 		= "project.classpath";
	
	/**
	 * The name used for referencing the SWAMP binary directory.
	 */
	private static String 	SWAMPBIN_NAME 		= "swampbin";
	
	/**
	 * The relative path of the SWAMP binary directory.
	 */
	private static String 	SWAMPBIN_REL_PATH 	= ".." + SEPARATOR + ".swampbin";
	
	/**
	 * The name of the generated build file.
	 */
	private static String 	BUILDFILE_NAME		= "build.xml";
	
	/**
	 * The number of spaces to indent per level in the generated XML 
	 * file.
	 */
	private static int 		INDENT_SPACES 		= 4;

	/**
	 * Generates build files for the set of projects passed in, 
	 * and saves each build file in the project's top-level directory
	 *
	 * @param projects a set of ClasspathHandler objects
	 */
	public static void generateBuildFiles(Set<ClasspathHandler> projects) {
		for (ClasspathHandler c : projects) {
			generateBuildFile(c);
		}
	}
	
	/**
	 * Generates a build file for the passed in project and saves the
	 * build file in the project's top-level directory 
	 *
	 * @param project a ClasspathHandler object for a project
	 */
	public static void generateBuildFile(ClasspathHandler project) {
		// Here's how to write XML in Java
		/* Adapted from www.mkyong.com/java/how-to-create-xml-file-in-java-dom */
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			Document doc = docBuilder.newDocument();
			
			// project (root)
			Element root = generateRoot(doc, project.getProjectName());
			doc.appendChild(root);
			// properties
			setProperties(doc, root, SWAMPBIN_REL_PATH, project.getSourceVersion(), project.getTargetVersion()); 
			// classpath
			setLibraryClasspath(doc, root, project.getLibraryClasspath(), project.getProjectName());
			// init target (as of now just creating output directory)
			String prjName = project.getProjectName();
			// build target
			setBuildTarget(doc, root, project.getOutputLocation(), project.getDependentProjects(), project.getSourceClasspath(), prjName, project.isRoot());	
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", INDENT_SPACES);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			System.out.println("File written to: " + project.getProjectPath() + SEPARATOR + BUILDFILE_NAME);
			StreamResult result = new StreamResult(new File(project.getProjectPath() + SEPARATOR + BUILDFILE_NAME));
			transformer.transform(source, result);
			
		} catch (ParserConfigurationException p) {
			p.printStackTrace();
		} catch (TransformerException t) {
			t.printStackTrace();
		}
		
	}
	
	/**
	 * Utility method that returns an element for this project, 
	 * attached to the doc. This is the root element for the XML doc
	 *
	 * @param doc the document
	 * @param projectName the name of the project
	 * @return the root element attached to the doc for this project
	 */
	private static Element generateRoot(Document doc, String projectName) {
		// project
		Element root = doc.createElement("project");
		Attr defaultTask = doc.createAttribute("default");
		defaultTask.setValue("build");
		root.setAttributeNode(defaultTask);
		Attr prjName = doc.createAttribute("name");
		prjName.setValue(projectName);
		root.setAttributeNode(prjName);
		return root;
	}
	
	/**
	 * Utility method for creating properties and then setting them
	 * on the root
	 *
	 * @param doc the document
	 * @param binPath the path to the SWAMP binary directory
	 * @param srcVersion the Java source version
	 * @param targetVersion the Java target version
	 */
	private static void setProperties(Document doc, Element root, String binPath, String srcVersion, String targetVersion) {
		Element swampbinProperty = getProperty(doc, SWAMPBIN_NAME, binPath);
		root.appendChild(swampbinProperty);
		
		Element srcVersionProperty = getProperty(doc, "source", srcVersion); 
		root.appendChild(srcVersionProperty);
		
		Element targetVersionProperty = getProperty(doc, "target", targetVersion);
		root.appendChild(targetVersionProperty);
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	private static Element getProperty(Document doc, String name, String value) {
		Element property = doc.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("value", value);
		return property;
	}
	
	/**
	 * Adds library entries to the classpath
	 *
	 * @param doc the document
	 * @param root the root element
	 * @param entries list of library classpath entries (either in 
	 * SWAMP binary directory or within the project directory)
	 * @param projectName the projectName
	 */
	private static void setLibraryClasspath(Document doc, Element root, List<IClasspathEntry> entries, String projectName) {
		Element path = doc.createElement("path");
		path.setAttribute("id", CLASSPATH_NAME);
		// TODO Maybe change name of classpath doc for different projects (?)
		//Element pathElement = doc.createElement("pathelement");
		String swampbin = "${" + SWAMPBIN_NAME + "}";
		for (IClasspathEntry entry : entries) {
			System.out.println("Library entry path: " + entry);
			// TODO get the actual filename (I don't think entry.toString() will do it
			Element pathElement = doc.createElement("pathelement");
			if (entry.getPath().toOSString().contains(SWAMPBIN_NAME)) {
				String name = entry.getPath().lastSegment();
				pathElement.setAttribute("location", swampbin + SEPARATOR + name);
			}
			else { // not in swampbin
				String relDir = relativizeDirectory(entry.getPath().toOSString(), projectName);
				System.out.println("Non-SWAMPBIN directory location: " + relDir);
				pathElement.setAttribute("location", relDir);
			}
			
			path.appendChild(pathElement);
		}
		root.appendChild(path);
			
		// TODO Add handling for bootclasspath (?)
	}
	
	/**
	 * Sets the output directory as a child of the root and creates
	 * an element for making that directory in case it doesn't exist 
	 *
	 * @param doc the document
	 * @param root the root
	 * @param relOutputDir the output directory, relative to this
	 * project's directory
	 * @return the element for "mkdir" of the output directory
	 */
	private static Element setInitTarget(Document doc, Element root, String relOutputDir) {
		Element target = doc.createElement("target");
		target.setAttribute("name", "init");
		root.appendChild(target);
		
		Element mkdir = doc.createElement("mkdir");
		mkdir.setAttribute("dir", relOutputDir);
		target.appendChild(mkdir);
		return target;
	}
	
	/**
	 * Returns the relative path of a directory. 
	 *
	 * @param dir the path of the directory. The directory will
	 * either be within the project (in which case getting the
	 * relative path is trivial) or outside of the project (in which
	 * case we just need to move up one level)
	 * @param projectName the name of the project that we are
	 * currently generating a build file for
	 * @return the relative path of the directory
	 */
	private static String relativizeDirectory(String dir, String projectName) {
		System.out.println("\n\nRelativizeDirectory");
		System.out.println("Original path: " + dir);
		System.out.println("Project name: " + projectName);

		int index = dir.indexOf(projectName);
		if (index > -1) {
			System.out.println("Relative path: " + dir.substring(index+1));
			return dir.substring(index + projectName.length() + 1);
		}
		index = dir.indexOf("package" + SEPARATOR);
		if (index > -1) {
			// this is an absolute path
			System.out.println(".." + dir.substring(index + "package".length()));
			return ".." + dir.substring(index + "package".length());
		}

		System.out.println("Relative path: " + ".." + dir);
		return ".." + dir;
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param doc the document
	 * @param root the root element
	 * @param prjOutputDirectory the output directory
	 * @param list the list of projects that the calling project 
	 * depends on
	 * @param projectName the name of the calling project
	 * @param isRoot is this project the root  
	 * @return the image descriptor
	 */
	private static void setBuildTarget(Document doc, Element root, String prjOutputDirectory, List<ClasspathHandler> list, List<IClasspathEntry> srcEntries, String projectName, boolean isRoot) {
		
		prjOutputDirectory = relativizeDirectory(prjOutputDirectory, projectName);
		System.out.println("Relative output directory: " + prjOutputDirectory);
		Element target = doc.createElement("target");
		target.setAttribute("name", "build");
		//if (isRoot) {
			Element init = setInitTarget(doc, root, prjOutputDirectory);
			target.setAttribute("depends", "init");
		//}
		
		root.appendChild(target);
		
		for (ClasspathHandler c : list) {
			String filepath = c.getProjectPath() + SEPARATOR + BUILDFILE_NAME;
			if (!new File(filepath).exists()) {
				BuildfileGenerator.generateBuildFile(c);
			}
			String relPath = ".." + SEPARATOR + c.getProjectName() + SEPARATOR + BUILDFILE_NAME;
			Element ant = doc.createElement("ant");
			ant.setAttribute("antfile", relPath); 
			ant.setAttribute("dir", ".." + SEPARATOR + c.getProjectName());
			ant.setAttribute("target", "build");
			target.appendChild(ant);
		}
		
		// includesfile and excludesfile attributes - http://ant.apache.org/manual/Tasks/javac.html
		for (IClasspathEntry entry : srcEntries) {
			Element javac = doc.createElement("javac");
			javac.setAttribute("includeantruntime", "false");
			javac.setAttribute("classpathref", CLASSPATH_NAME);
			// Source entries may have a specific output location associated with them, otherwise, we use the project's default location
			IPath destPath = entry.getOutputLocation();
			String destDir = null;
			if (destPath != null) {
				destDir = relativizeDirectory(destPath.toString(), projectName);
			}
			else {
				destDir = prjOutputDirectory;
			}
			Element mkdir = doc.createElement("mkdir");
			mkdir.setAttribute("dir", destDir);
			init.appendChild(mkdir);
			javac.setAttribute("destdir", destDir);
			javac.setAttribute("source", "${source}");
			javac.setAttribute("target", "${target}");
			
			Element src = doc.createElement("src");
			String absPath = entry.getPath().makeAbsolute().toOSString();

			addInclusionExclusionPatterns(doc, javac, "include", entry.getInclusionPatterns());
			addInclusionExclusionPatterns(doc, javac, "exclude", entry.getExclusionPatterns());
	
			String strRelPath = relativizeDirectory(absPath, projectName);
			System.out.println("Made relative: " + strRelPath);
			src.setAttribute("path", strRelPath);
			javac.appendChild(src);
			target.appendChild(javac);
		}
		
	}
	
	/**
	 * Adds inclusion and exclusion patterns to the passed in element
	 *
	 * @param doc the document
	 * @param parent the parent element
	 * @param taskName the name of the Ant task (either include or exclude)
	 * @param patterns the array of inclusion or exclusion patterns
	 */
	private static void addInclusionExclusionPatterns(Document doc, Element parent, String taskName, IPath[] patterns) {
		if ((patterns == null) || (patterns.length == 0)) {
			return;
		}
		for (IPath pattern : patterns) {
			Element include = doc.createElement(taskName);
			include.setAttribute("name", pattern.toString());
			parent.appendChild(include);
		}
	}
	
}