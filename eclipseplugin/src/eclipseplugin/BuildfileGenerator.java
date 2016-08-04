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

/* This class is to do the actual generation of the buildfiles */
public class BuildfileGenerator {
	
	private static String 	CLASSPATH_NAME 		= "project.classpath";
	private static String 	SWAMPBIN_NAME 		= "swampbin";
	private static String 	SWAMPBIN_REL_PATH 	= ".." + SEPARATOR + ".swampbin";
	private static String 	BUILDFILE_NAME		= "build.xml";
	private static int 		INDENT_SPACES 		= 4;

	public static void generateBuildFiles(Set<ClasspathHandler> projects) {
		for (ClasspathHandler c : projects) {
			generateBuildFile(c);
		}
	}
	
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
			setProperties(doc, root, SWAMPBIN_REL_PATH, "1.7", "1.7"); // TODO Set this based off of config dialog
			// classpath
			setLibraryClasspath(doc, root, project.getLibraryClasspath());
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
	
	private static void setProperties(Document doc, Element root, String binPath, String srcVersion, String targetVersion) {
		Element swampbinProperty = getProperty(doc, SWAMPBIN_NAME, binPath);
		root.appendChild(swampbinProperty);
		
		Element srcVersionProperty = getProperty(doc, "source", srcVersion); 
		root.appendChild(srcVersionProperty);
		
		Element targetVersionProperty = getProperty(doc, "target", targetVersion);
		root.appendChild(targetVersionProperty);
	}
	
	private static Element getProperty(Document doc, String name, String value) {
		Element property = doc.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("value", value);
		return property;
	}
	
	private static void setLibraryClasspath(Document doc, Element root, List<IClasspathEntry> entries) {
		Element path = doc.createElement("path");
		path.setAttribute("id", CLASSPATH_NAME);
		// TODO Maybe change name of classpath doc for different projects (?)
		//Element pathElement = doc.createElement("pathelement");
		String swampbin = "${" + SWAMPBIN_NAME + "}";
		for (IClasspathEntry entry : entries) {
			// TODO get the actual filename (I don't think entry.toString() will do it
			Element pathElement = doc.createElement("pathelement");
			String name = entry.getPath().lastSegment();
			pathElement.setAttribute("location", swampbin + SEPARATOR + name);
			path.appendChild(pathElement);
		}
		root.appendChild(path);
			
		// TODO Add handling for bootclasspath (?)
	}
	
	private static Element setInitTarget(Document doc, Element root, String relOutputDir) {
		Element target = doc.createElement("target");
		target.setAttribute("name", "init");
		root.appendChild(target);
		
		Element mkdir = doc.createElement("mkdir");
		mkdir.setAttribute("dir", relOutputDir);
		target.appendChild(mkdir);
		return target;
	}
	
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