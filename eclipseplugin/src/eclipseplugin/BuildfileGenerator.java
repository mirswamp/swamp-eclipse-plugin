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

/* This class is to do the actual generation of the buildfiles */
public class BuildfileGenerator {
	
	private static String 	CLASSPATH_NAME 		= "project.classpath";
	private static String 	SWAMPBIN_NAME 		= "swampbin";
	private static String 	SWAMPBIN_REL_PATH 	= "../.swampbin";//"package/.swampbin";
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
			String outputDir = makeRelative(project.getOutputLocation(), prjName);
			setInitTarget(doc, root, outputDir);
			// build target
			setBuildTarget(doc, root, outputDir, project.getDependentProjects(), project.getSourceClasspath(), prjName);	
			// TODO Fix this output location
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", INDENT_SPACES);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			System.out.println("File written to: " + project.getProjectPath() + "/" + BUILDFILE_NAME);
			StreamResult result = new StreamResult(new File(project.getProjectPath() + "/" + BUILDFILE_NAME));
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
			pathElement.setAttribute("location", swampbin + "/" + name);
			path.appendChild(pathElement);
		}
		root.appendChild(path);
			
		// TODO Add handling for bootclasspath (?)
	}
	
	// TODO Put this into a utility class
	public static String makeRelative(String path, String projectName) {
		System.out.println("Make Relative");
		System.out.println("Original path: " + path);
		System.out.println("Project name: " + projectName);
		String original = path;
		int index = path.indexOf(projectName);
		if (index < 0) {
			return original;
		}
		String relPath = path.substring(index+projectName.length(), path.length());
		if (relPath.charAt(0) == '/') {
			return relPath.substring(1, relPath.length());
		}
		return relPath;
	}
	
	private static void setInitTarget(Document doc, Element root, String relOutputDir) {
		Element target = doc.createElement("target");
		target.setAttribute("name", "init");
		root.appendChild(target);
		
		Element mkdir = doc.createElement("mkdir");
		mkdir.setAttribute("dir", relOutputDir);
		target.appendChild(mkdir);
	}
	
	private static void setBuildTarget(Document doc, Element root, String relOutputDir, List<ClasspathHandler> list, List<IClasspathEntry> srcEntries, String projectName) {
		Element target = doc.createElement("target");
		target.setAttribute("name", "build");
		target.setAttribute("depends", "init");
		root.appendChild(target);
		
		for (ClasspathHandler c : list) {
			String filepath = c.getProjectPath() + "/" + BUILDFILE_NAME;
			if (!new File(filepath).exists()) {
				BuildfileGenerator.generateBuildFile(c);
			}
			String relPath = "../" + c.getProjectName() + "/" + BUILDFILE_NAME;
			Element ant = doc.createElement("ant");
			ant.setAttribute("antfile", relPath); 
			ant.setAttribute("target", "build");
			target.appendChild(ant);
		}
		
		Element javac = doc.createElement("javac");
		javac.setAttribute("destdir", relOutputDir);
		javac.setAttribute("source", "${source}");
		javac.setAttribute("target", "${target}");
		target.appendChild(javac);
		
		// TODO Add handling for inclusion and exclusion patterns
		for (IClasspathEntry entry : srcEntries) {
			Element src = doc.createElement("src");
			IPath absPath = entry.getPath().makeAbsolute();
			//IPath relPath = absPath.removeFirstSegments(absPath.segmentCount()-3);
			// TODO -3 won't be right if it's package/project/main/src or something
			//System.out.println("Rel path: " + relPath.toString());
			String strRelPath = makeRelative(absPath.toString(), projectName);
			System.out.println("Made relative: " + strRelPath);
			src.setAttribute("path", strRelPath);
			javac.appendChild(src);
		}
		
		Element classpath = doc.createElement("classpath");
		classpath.setAttribute("refid", CLASSPATH_NAME);
		
	}
}
