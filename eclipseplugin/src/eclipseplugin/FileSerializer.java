package eclipseplugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IProject;

import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.SelectionDialog;
import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;

public class FileSerializer {
	
	private static String DELIMITER = ",";
	private static String NEEDS_BUILD_FILE = "NEEDSBUILDFILE";
	private static String HAS_BUILD_FILE = "HASBUILDFILE";
	
	public static boolean deserializeSubmissionInfo(String filepath, SubmissionInfo si) {
		File file = new File(filepath);
		if (!file.exists()) {
			System.out.println("File does not exist");
			return false;
		}
		
		FileReader filereader = null;
		BufferedReader reader = null;
		
		try {
			filereader = new FileReader(file);
			reader = new BufferedReader(filereader);

			deserializeConfigInfo(reader, si);
			si.setConfigInitialized(true);
			deserializeSelectionInfo(reader, si);
			si.setSelectionInitialized(true);

			/*
			deserializeSelectionInfo(reader, si);
			si.setSelectionInitialized(true);
			deserializeConfigInfo(reader, si);
			si.setConfigInitialized(true);
			*/
			reader.close();
		}
		catch (Exception e) {
			System.err.println("Unable to deserialize file");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void serializeSubmissionInfo(String filepath, SubmissionInfo si) {
		File file = new File(filepath);
		if (file.exists()) {
			deleteFile(filepath);
		}
		
		FileWriter filewriter = null;
		BufferedWriter writer = null;
		
		try {
			filewriter = new FileWriter(file);
			writer = new BufferedWriter(filewriter);
			
			serializeConfigInfo(writer, si);
			serializeSelectionInfo(writer, si);
			
			/*
			serializeSelectionInfo(writer, si);
			serializeConfigInfo(writer, si);
			*/
			writer.close();
		}
		catch (Exception e) {
			System.err.println("Unable to serialize file");
			e.printStackTrace();
		}
	}
	
	private static void deserializeSelectionInfo(BufferedReader reader, SubmissionInfo si) throws IOException {
		String str = null;
		/*
		// Project UUID
		String str = reader.readLine();
		if (str == null || str.equals("\n")) {
			// TODO Throw an exception here
			//return false;
		}
		si.setSelectedProjectID(str);
		*/
		
		// List of Platform UUIDs
		str = reader.readLine();
		if (str == null || str.equals("\n")) {
			//return false;
		}
		List<String> platformList =  Utils.convertDelimitedStringToList(str, DELIMITER);
		si.setSelectedPlatformIDs(platformList);

		// List of Tool UUIDs
		str = reader.readLine();
		if (str == null || str.equals("\n")) {
			//return false;
		}
		List<String> toolList =  Utils.convertDelimitedStringToList(str, DELIMITER);
		si.setSelectedToolIDs(toolList);
		si.setSelectionInitialized(true);
	}

	private static void deserializeConfigInfo(BufferedReader reader, SubmissionInfo si) throws IOException {
		String str = null;

		// Project UUID
		str = reader.readLine();
		if (str == null || str.equals("\n")) {
			// TODO Throw an exception here
			//return false;
		}
		si.setSelectedProjectID(str);
		
		// Package Type
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			si.setPackageType(str);
		}
		
		// Eclipse Project Name
		String prjName = null, prjPath = null;
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			prjName = str;
		}
		// Eclipse Project Path
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			prjPath = str;
		}
		if (!si.initializeProject(prjName, prjPath)) {
			// TODO Throw an exception here for failing to find project
			// This right here is a big problem!, particularly if we're running in background
			// Need custom exception here
			reader.close();
			System.out.println("We were unable to find the last selected project");
			//return false;
		}
		
		// SWAMP Package UUID
		String pkgUUID = null;
		
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			char c = str.charAt(0);
			if (c == '\t') {
				pkgUUID = str.substring(1, str.length());
				si.setSelectedPackageID(pkgUUID);
			}
			else {
				String name = str;
				if (!si.setPackageIDFromName(name)) {
					reader.close();
					System.out.println("We were unable to find the last SWAMP Package");
					// return or throw exception here
				}
			}
		}

		// Build system 
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			si.setBuildSystem(str);
		}
		
		str =  reader.readLine();
		if (str != null && !str.equals("\n")) {
			if (str.equals(NEEDS_BUILD_FILE)) {
				si.setNeedsBuildFile(true);
			}
			else if (str.equals(HAS_BUILD_FILE)) {
				si.setNeedsBuildFile(false);
			}
			else {
				// TODO Handle ERROR
			}
		}
		
		// Build File
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			si.setBuildFile(str);
		}
		// Build Dir
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			si.setBuildDirectory(str);
		}
		// Build Target
		str = reader.readLine();
		if (str != null && !str.equals("\n")) {
			si.setBuildTarget(str);
		}
	}
	
	public static void serializeSelectionInfo(BufferedWriter writer, SubmissionInfo si) throws IOException {
		/*
		// Project UUID
		writer.write(si.getSelectedProjectID());
		writer.write("\n");
		*/
				
		// Platform UUIDs
		List<String> platformIDList = si.getSelectedPlatformIDs();
		String platformIDString = Utils.convertListToDelimitedString(platformIDList, DELIMITER);
		writer.write(platformIDString);
		writer.write("\n");
				
		// Tool UUIDs
		List<String> toolIDList = si.getSelectedToolIDs();
		String toolIDString = Utils.convertListToDelimitedString(toolIDList, DELIMITER);
		writer.write(toolIDString);
		writer.write("\n");
	}
	
	private static void serializeConfigInfo(BufferedWriter writer, SubmissionInfo si) throws IOException {
		// Project UUID
		String projectUUID = si.getSelectedProjectID();
		writer.write(projectUUID);
		writer.write("\n");
		
		// Package Type
		String packageType = si.getPackageType();
		writer.write(packageType);
		writer.write("\n");
		
		// Project name
		String projectName = si.getProjectName();
		writer.write(projectName);
		writer.write("\n");
		
		// Project path
		String projectPath = si.getProjectPath();
		writer.write(projectPath);
		writer.write("\n");
		
		// Package UUID or Package Name
		if (si.isNewPackage()) {
			writer.write(si.getPackageName());
		}
		else {
			writer.write("\t" + si.getSelectedPackageID());
		}
		writer.write("\n");
		
		// Build system
		String buildSystem = si.getBuildSystem();
		writer.write(buildSystem);
		writer.write("\n");
		
		// Make new build file
		if (si.needsBuildFile()) {
			writer.write(NEEDS_BUILD_FILE);
		}
		else {
			writer.write(HAS_BUILD_FILE);
		}
		writer.write("\n");
		
		// Build file
		String buildFile = si.getBuildFile();
		writer.write(buildFile);
		writer.write("\n");
		
		// Build directory
		String buildDirectory = si.getBuildDirectory();
		writer.write(buildDirectory);
		writer.write("\n");
		
		// Build Target
		String buildTarget = si.getBuildTarget();
		writer.write(buildTarget);
		writer.write("\n");
	}
	
	private static void deleteFile(String filepath) {
		File file = new File(filepath);
		file.delete();
	}
	
}
