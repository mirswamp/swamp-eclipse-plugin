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

package org.continuousassurance.swamp.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.eclipse.jdt.core.IClasspathAttribute;
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
	 * The name used for referencing the bootclasspath for a project.
	 */	
	private static String 	BOOTCLASSPATH_NAME	= "project.bootclasspath";
	
	/**
	 * The name used for referencing the sourcepath for a project.
	 */
	private static String	SOURCEPATH_NAME		= "project.sourcepath";
	
	/**
	 * The name used for referencing the SWAMP binary directory.
	 */
	private static String 	SWAMPBIN_NAME 		= "swampbin";
	
	/**
	 * The relative path of the SWAMP binary directory.
	 */
	private static String 	SWAMPBIN_REL_PATH 	= "swampbin";
	
	/**
	 * The name of the generated build file.
	 */
	public static String 	BUILDFILE_EXT		= ".xml";
	
	/**
	 * The number of spaces to indent per level in the generated XML 
	 * file.
	 */
	private static int 		INDENT_SPACES 		= 4;

	/**
	 * Generates a build file for the passed in project and saves the
	 * build file in the project's top-level directory 
	 *
	 * @param project a ClasspathHandler object for a project
	 */
	public static void generateBuildFile(ImprovedClasspathHandler project, Set<String> filePaths) {
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
			setLibraryClasspath(doc, root, project.getLibraryClasspath(), project.getExportedEntries(), project.getSourceClasspath(), project.getDependentProjects(), project.getDefaultOutputLocation().toOSString());
			setBootClasspath(doc, root, project.getSystemLibraryClasspath());
			setSourcepath(doc, root, project.getSourceClasspath(), project.getDependentProjects());
			// init target (as of now just creating output directory)
			String prjName = project.getProjectName();
			// build target
			setBuildTarget(doc, root, project.getDefaultOutputLocation().toOSString(), project.getDependentProjects(), project.getSourceClasspath(), prjName, filePaths);	
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", INDENT_SPACES);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			String buildFilePath = project.getProjectPluginLocation() + SEPARATOR + project.getProjectName() + BUILDFILE_EXT;
			File buildFile = new File(buildFilePath);
			if (buildFile.exists()) {
				System.out.println("Build file exists");
				buildFile.delete();
				try {
					buildFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("File written to: " + buildFilePath);
			StreamResult result = new StreamResult(buildFile);
			transformer.transform(source, result);
			filePaths.add(buildFilePath);
			
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
	 * Sets the library classpath for the build file
	 *
	 * @param doc the document
	 * @param root the root element
	 * @param entries list of library classpath entries (either in 
	 * SWAMP binary directory or within the project directory)
	 * @param projectName the projectName
	 */
	private static void setLibraryClasspath(Document doc, Element root, List<IClasspathEntry> libEntries, List<IClasspathEntry> dependentProjectExports, List<IClasspathEntry> srcEntries, List<ImprovedClasspathHandler> dependentProjects, String defaultOutputDir) {
		Element path = doc.createElement("path");
		path.setAttribute("id", CLASSPATH_NAME);
		addLibraryEntries(doc, path, libEntries);
		addLibraryEntries(doc, path, dependentProjectExports);

		String relPath = defaultOutputDir.substring(1);
		addPathElement(doc, path, relPath);
		addSourceOutputEntries(doc, root, srcEntries);
		// add dependent project sources to this
		for (ImprovedClasspathHandler i : dependentProjects) {
			relPath = i.getDefaultOutputLocation().toOSString().substring(1);
			addPathElement(doc, path, relPath);
			addSourceOutputEntries(doc, root, i.getSourceClasspath());
		}
		root.appendChild(path);
	}
	
	/**
	 * Adds library entries to the classpath
	 * 
	 * @param doc the document
	 * @param path the path element
	 * @param entries list of entries to be added
	 */
	private static void addLibraryEntries(Document doc, Element path, List<IClasspathEntry> entries) {
		for (IClasspathEntry entry : entries) {
			String rootedPath = entry.getPath().toOSString();
			String relativePath = rootedPath.substring(1);
			addPathElement(doc, path, relativePath);
		}
	}
	
	/**
	 * Adds source output entries to the classpath
	 * 
	 * @param doc the document
	 * @param path the path element that the entries are being added under
	 * @param srcEntries list of source entries to be added
	 */
	private static void addSourceOutputEntries(Document doc, Element path, List<IClasspathEntry> srcEntries) {
		Set<String> dirs = new HashSet<>();
		if (srcEntries != null) {
			for (IClasspathEntry entry : srcEntries) {
				IPath outputPath = entry.getOutputLocation();
				if (outputPath != null) {
					String relativePath = outputPath.toOSString().substring(1);
					if (!dirs.contains(relativePath)) {
						dirs.add(relativePath);
						addPathElement(doc, path, relativePath);
					}
				}
			}
		}
	}
	
	/**
	 * Sets bootclasspath for the build file
	 * 
	 * @param doc the document
	 * @param root the root element of the XML document
	 * @param entries list of classpath entries
	 */
	private static void setBootClasspath(Document doc, Element root, List<IClasspathEntry> entries) {
		Element path = doc.createElement("path");
		path.setAttribute("id", BOOTCLASSPATH_NAME);
		for (IClasspathEntry entry : entries) {
			String rootedPath = entry.getPath().toOSString();
			String relativePath = rootedPath.substring(1);
			addPathElement(doc, path, relativePath);
		}
		root.appendChild(path);
	}

	/**
	 * Sets sourcepath for the build file
	 * 
	 * @param doc the document
	 * @param root the root element of the XML document
	 * @param entries list of the main project's sourcepath entries
	 * @param dependentProjects list of dependent projects, so we can add their source classpath entries, as well
	 */
	private static void setSourcepath(Document doc, Element root, List<IClasspathEntry> entries, List<ImprovedClasspathHandler> dependentProjects) {
		// TODO Get rid of duplicate code
		Element path = doc.createElement("path");
		path.setAttribute("id", SOURCEPATH_NAME);
		addSourceEntries(doc, path, entries);
		for (ImprovedClasspathHandler i : dependentProjects) {
			addSourceEntries(doc, path, i.getSourceClasspath());
		}
		root.appendChild(path);
	}
	
	/**
	 * Sets bootclasspath for the build file
	 * 
	 * @param doc the document
	 * @param root the root element of the XML document
	 * @param entries list of classpath entries
	 */
	private static void addSourceEntries(Document doc, Element path, List<IClasspathEntry> entries) {
		for (IClasspathEntry entry : entries) {
			String rootedPath = entry.getPath().toOSString();
			String relativePath = rootedPath.substring(1);
			addPathElement(doc, path, relativePath);
		}
	}
	
	/**
	 * Add "pathelement" to the XML tree
	 * @param doc the document
	 * @param path the path element that this will be beneath
	 * @param strPath the actual path
	 */
	private static void addPathElement(Document doc, Element path, String strPath) {
		Element pe = doc.createElement("pathelement");
		pe.setAttribute("location", strPath);
		path.appendChild(pe);
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
	private static void setBuildTarget(Document doc, Element root, String prjOutputDirectory, List<ImprovedClasspathHandler> dependentProjects, List<IClasspathEntry> srcEntries, String projectName, Set<String> filePaths) {
		
		prjOutputDirectory = prjOutputDirectory.substring(1); // unroot it
		System.out.println("Relative output directory: " + prjOutputDirectory);
		Element target = doc.createElement("target");
		target.setAttribute("name", "build");
		//if (isRoot) {
			Element init = setInitTarget(doc, root, prjOutputDirectory);
			target.setAttribute("depends", "init");
		//}
		
		root.appendChild(target);
		
		for (ImprovedClasspathHandler ich : dependentProjects) {
			BuildfileGenerator.generateBuildFile(ich, filePaths);
			String buildFilePath = ich.getProjectName() + BUILDFILE_EXT; // this will be in the same directory
			Element ant = doc.createElement("ant");
			ant.setAttribute("antfile", buildFilePath); 
			ant.setAttribute("target", "build");
			target.appendChild(ant);
		}
		
		// includesfile and excludesfile attributes - http://ant.apache.org/manual/Tasks/javac.html
		for (IClasspathEntry entry : srcEntries) {
			IPath[] inclusionPatterns = entry.getInclusionPatterns();
			IPath[] exclusionPatterns = entry.getExclusionPatterns();
			Element javac = doc.createElement("javac");
			javac.setAttribute("includeantruntime", "false");
			javac.setAttribute("classpathref", CLASSPATH_NAME);
			javac.setAttribute("bootclasspathref", BOOTCLASSPATH_NAME);
			if (inclusionPatterns.length == 0 && exclusionPatterns.length == 0) {
				javac.setAttribute("sourcepathref", SOURCEPATH_NAME);
			}
			else {
				javac.setAttribute("sourcepath", getModifiedSourcePath(entry, srcEntries));
			}
			String encoding = getEncodingAttribute(entry);
			if (!encoding.equals("")) {
				javac.setAttribute("encoding", encoding);
			}
			// Source entries may have a specific output location associated with them, otherwise, we use the project's default location
			IPath destPath = entry.getOutputLocation();
			String destDir = null;
			if (destPath != null) {
				String strPath = destPath.toOSString();
				destDir = strPath.substring(1);
			}
			else {
				destDir = prjOutputDirectory;
			}
			Element mkdir = doc.createElement("mkdir");
			mkdir.setAttribute("dir", destDir); // TODO Become smarter about when we actually need this
			init.appendChild(mkdir);
			javac.setAttribute("destdir", destDir);
			javac.setAttribute("source", "${source}");
			javac.setAttribute("target", "${target}");
			
			Element src = doc.createElement("src");
			addInclusionExclusionPatterns(doc, javac, "include", entry.getInclusionPatterns());
			addInclusionExclusionPatterns(doc, javac, "exclude", entry.getExclusionPatterns());
			
			String absPath = entry.getPath().toOSString();
			String strRelPath = absPath.substring(1);
			System.out.println("Relative path: " + strRelPath);
			src.setAttribute("path", strRelPath);
			javac.appendChild(src);
			target.appendChild(javac);
		}
		
	}
	
	// We don't want to include an entry in its own sourcepath or we'll have problems
	/**
	 * Builds sourcepath with relative paths for an entry
	 * @param currentEntry the source entry
	 * @param entries the source entries
	 * @return comma-delimited source path
	 */
	private static String getModifiedSourcePath(IClasspathEntry currentEntry, List<IClasspathEntry> entries) {
		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < entries.size(); i++) {
			IClasspathEntry entry = entries.get(i);
			if (currentEntry.equals(entry)) {
				continue;
			}
			String rootedPath = entry.getPath().toOSString();
			String relativePath = rootedPath.substring(1);
			String str = i == 0 ? relativePath : "," + relativePath;
			sb.append(str);
		}
		return sb.toString();
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
	
	/**
	 * Gets the encoding of a classpath entry
	 * @param entry the entry
	 * @return the encoding or empty string if unspecified
	 */
	private static String getEncodingAttribute(IClasspathEntry entry) {
		IClasspathAttribute[] attributes = entry.getExtraAttributes();
		for (IClasspathAttribute attr : attributes) {
			if (attr.getName().equals(IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING)) {
				return attr.getValue();
			}
		}
		return "";
	}
	
}