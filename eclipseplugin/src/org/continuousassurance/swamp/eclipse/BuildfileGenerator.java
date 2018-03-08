/*
 * Copyright 2016-2017 Malcolm Reid Jr.
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


/**
 * This class does the actual generation of the build files.
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class BuildfileGenerator {
	
	/**
	 * The name used for referencing the classpath for a project.
	 */
	private static final String 	CLASSPATH_NAME 		= "project.classpath";
	
	/**
	 * The name used for referencing the bootclasspath for a project.
	 */	
	private static final String 	BOOTCLASSPATH_NAME	= "project.bootclasspath";
	
	/**
	 * The name used for referencing the sourcepath for a project.
	 */
	private static final String	SOURCEPATH_NAME		= "project.sourcepath";
	
	/**
	 * The name used for referencing the SWAMP binary directory.
	 */
	private static final String 	SWAMPBIN_NAME 		= "swampbin";
	
	/**
	 * The relative path of the SWAMP binary directory.
	 */
	private static final String 	SWAMPBIN_REL_PATH 	= "swampbin";
	
	/**
	 * The name of the generated build file.
	 */
	public static final String 	BUILDFILE_EXT		= ".xml";
	
	/**
	 * The number of spaces to indent per level in the generated XML 
	 * file.
	 */
	private static final int 	INDENT_SPACES 		= 4;
	
	/*
	 * Variables for ant attributes
	 */
	private static final String ANT_INDENT_NUMBER = "indent-number";
	private static final String ANT_YES = "yes";
	private static final String ANT_PROJECT = "project";
	private static final String ANT_DEFAULT = "default";
	private static final String ANT_BUILD = "build";
	private static final String ANT_NAME = "name";
	private static final String ANT_SOURCE = "source";
	private static final String ANT_TARGET = "target";
	private static final String ANT_VALUE = "value";
	private static final String ANT_PROPERTY = "property";
	private static final String ANT_PATH = "path";
	private static final String ANT_ID = "id";
	private static final String ANT_PATH_ELEMENT = "pathelement";
	private static final String ANT_LOCATION = "location";
	private static final String ANT_INIT = "init";
	private static final String ANT_MKDIR = "mkdir";
	private static final String ANT_DIR = "dir";
	private static final String ANT_DEPENDS = "depends";
	private static final String ANT_ANT = "ant";
	private static final String ANT_ANTFILE = "antfile";
	private static final String ANT_JAVAC = "javac";
	private static final String ANT_INCLUDE_ANT_RUNTIME = "includeantruntime";
	private static final String ANT_FALSE = "false";
	private static final String ANT_CLASSPATHREF = "classpathref";
	private static final String ANT_BOOTCLASSPATHREF = "bootclasspathref";
	private static final String ANT_SOURCEPATHREF = "sourcepathref";
	private static final String ANT_SOURCEPATH = "sourcepath";
	private static final String ANT_ENCODING = "encoding";
	private static final String ANT_DESTDIR = "destdir";
	private static final String ANT_INCLUDE = "include";
	private static final String ANT_EXCLUDE = "exclude";
	private static final String ANT_SRC = "src";
	
	/**
	 * Private constructor so this class is not subclassed
	 */
	private BuildfileGenerator() {
	}

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
			setBuildTarget(doc, root, project.getDefaultOutputLocation().toOSString(), project.getDependentProjects(), project.getSourceClasspath(), prjName, filePaths, project.getEncoding());	
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute(ANT_INDENT_NUMBER, INDENT_SPACES);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, ANT_YES);
			DOMSource source = new DOMSource(doc);
			String buildFilePath = project.getRootProjectPluginLocation() + File.separator + project.getProjectName() + BUILDFILE_EXT;
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
		Element root = doc.createElement(ANT_PROJECT);
		Attr defaultTask = doc.createAttribute(ANT_DEFAULT);
		defaultTask.setValue(ANT_BUILD);
		root.setAttributeNode(defaultTask);
		Attr prjName = doc.createAttribute(ANT_NAME);
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
		
		Element srcVersionProperty = getProperty(doc, ANT_SOURCE, srcVersion); 
		root.appendChild(srcVersionProperty);
		
		Element targetVersionProperty = getProperty(doc, ANT_TARGET, targetVersion);
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
		Element property = doc.createElement(ANT_PROPERTY);
		property.setAttribute(ANT_NAME, name);
		property.setAttribute(ANT_VALUE, value);
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
		Element path = doc.createElement(ANT_PATH);
		path.setAttribute(ANT_ID, CLASSPATH_NAME);
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
		if (srcEntries != null) {
			Set<String> dirs = new HashSet<>();
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
		Element path = doc.createElement(ANT_PATH);
		path.setAttribute(ANT_ID, BOOTCLASSPATH_NAME);
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
		Element path = doc.createElement(ANT_PATH);
		path.setAttribute(ANT_ID, SOURCEPATH_NAME);
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
		Element pe = doc.createElement(ANT_PATH_ELEMENT);
		pe.setAttribute(ANT_LOCATION, strPath);
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
		Element target = doc.createElement(ANT_TARGET);
		target.setAttribute(ANT_NAME, ANT_INIT);
		root.appendChild(target);
		
		Element mkdir = doc.createElement(ANT_MKDIR);
		mkdir.setAttribute(ANT_DIR, relOutputDir);
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
	private static void setBuildTarget(Document doc, Element root, String outputDirectory, List<ImprovedClasspathHandler> dependentProjects, List<IClasspathEntry> srcEntries, String projectName, Set<String> filePaths, String prjEncoding) {
		
		String prjOutputDirectory = outputDirectory.substring(1); // unroot it
		System.out.println("Relative output directory: " + prjOutputDirectory);
		Element target = doc.createElement(ANT_TARGET);
		target.setAttribute(ANT_NAME, ANT_BUILD);
		//if (isRoot) {
			Element init = setInitTarget(doc, root, prjOutputDirectory);
			target.setAttribute(ANT_DEPENDS, ANT_INIT);
		//}
		
		root.appendChild(target);
		
		for (ImprovedClasspathHandler ich : dependentProjects) {
			BuildfileGenerator.generateBuildFile(ich, filePaths);
			String buildFilePath = ich.getProjectName() + BUILDFILE_EXT; // this will be in the same directory
			Element ant = doc.createElement(ANT_ANT);
			ant.setAttribute(ANT_ANTFILE, buildFilePath); 
			ant.setAttribute(ANT_TARGET, ANT_BUILD);
			target.appendChild(ant);
		}
		
		// includesfile and excludesfile attributes - http://ant.apache.org/manual/Tasks/javac.html
		for (IClasspathEntry entry : srcEntries) {
			IPath[] inclusionPatterns = entry.getInclusionPatterns();
			IPath[] exclusionPatterns = entry.getExclusionPatterns();
			Element javac = doc.createElement(ANT_JAVAC);
			javac.setAttribute(ANT_INCLUDE_ANT_RUNTIME, ANT_FALSE);
			javac.setAttribute(ANT_CLASSPATHREF, CLASSPATH_NAME);
			javac.setAttribute(ANT_BOOTCLASSPATHREF, BOOTCLASSPATH_NAME);
			if (inclusionPatterns.length == 0 && exclusionPatterns.length == 0) {
				javac.setAttribute(ANT_SOURCEPATHREF, SOURCEPATH_NAME);
			}
			else {
				javac.setAttribute(ANT_SOURCEPATH, getModifiedSourcePath(entry, srcEntries));
			}
			String srcEncoding = getEncodingAttribute(entry);
			if (!("").equals(srcEncoding)) {
				javac.setAttribute(ANT_ENCODING, srcEncoding);
			}
			else if (!("".equals(prjEncoding)) && prjEncoding != null) {
				System.out.println("Project encoding: " + prjEncoding);
				javac.setAttribute(ANT_ENCODING, prjEncoding);
			}
			// Source entries may have a specific output location associated with them, otherwise, we use the project's default location
			IPath destPath = entry.getOutputLocation();
			String destDir;
			if (destPath != null) {
				String strPath = destPath.toOSString();
				destDir = strPath.substring(1);
			}
			else {
				destDir = prjOutputDirectory;
			}
			Element mkdir = doc.createElement(ANT_MKDIR);
			mkdir.setAttribute(ANT_DIR, destDir); // TODO Become smarter about when we actually need this
			init.appendChild(mkdir);
			javac.setAttribute(ANT_DESTDIR, destDir);
			javac.setAttribute(ANT_SOURCE, "${source}");
			javac.setAttribute(ANT_TARGET, "${target}");
			
			Element src = doc.createElement(ANT_SRC);
			addInclusionExclusionPatterns(doc, javac, ANT_INCLUDE, entry.getInclusionPatterns());
			addInclusionExclusionPatterns(doc, javac, ANT_EXCLUDE, entry.getExclusionPatterns());
			
			String absPath = entry.getPath().toOSString();
			String strRelPath = absPath.substring(1);
			System.out.println("Relative path: " + strRelPath);
			src.setAttribute(ANT_PATH, strRelPath);
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
			include.setAttribute(ANT_NAME, pattern.toString());
			parent.appendChild(include);
		}
	}
	
	/**
	 * Gets the encoding of a classpath entry. Falls back to project encoding
	 * if none found
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