package eclipseplugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileSerializer {
	
	private static String DELIMITER = ",";
	private static String NEEDS_BUILD_FILE = "NEEDSBUILDFILE";
	private static String HAS_BUILD_FILE = "HASBUILDFILE";
	private static String LINE_SEPARATOR = System.lineSeparator();
	
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

			if (!deserializeConfigInfo(reader, si)) {
				return false;
			}
			si.setConfigInitialized(true);
			deserializeSelectionInfo(reader, si);

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
	
	private static boolean deserializeSelectionInfo(BufferedReader reader, SubmissionInfo si) throws IOException {
		String str = null;
		/*
		// Project UUID
		String str = reader.readLine();
		if (str == null || str.equals(LINE_SEPARATOR)) {
			// TODO Throw an exception here
			//return false;
		}
		si.setSelectedProjectID(str);
		*/
		
		// List of Platform UUIDs
		str = reader.readLine();
		if (str == null || str.equals(LINE_SEPARATOR)) {
			return false;
		}
		List<String> platformList =  Utils.convertDelimitedStringToList(str, DELIMITER);
		si.setSelectedPlatformIDs(platformList);

		// List of Tool UUIDs
		str = reader.readLine();
		if (str == null || str.equals(LINE_SEPARATOR)) {
			return false;
		}
		List<String> toolList =  Utils.convertDelimitedStringToList(str, DELIMITER);
		si.setSelectedToolIDs(toolList);
		return true;
	}

	private static boolean deserializeConfigInfo(BufferedReader reader, SubmissionInfo si) throws IOException {
		String str = null;

		// Project UUID
		str = reader.readLine();
		if (str == null || str.equals(LINE_SEPARATOR)) {
			// TODO Throw an exception here
			return false;
		}
		si.setSelectedProjectID(str);
		
		// Package Type
		str = reader.readLine();
		if (str != null && !str.equals(LINE_SEPARATOR)) {
			si.setPackageType(str);
		}
		
		// Eclipse Project Name
		String prjName = null, prjPath = null;
		str = reader.readLine();
		if (str != null && !str.equals(LINE_SEPARATOR)) {
			prjName = str;
		}
		else {
			return false;
		}
		// Eclipse Project Path
		str = reader.readLine();
		if (str != null && !str.equals(LINE_SEPARATOR)) {
			prjPath = str;
		}
		else {
			return false;
		}
		if (!si.initializeProject(prjName, prjPath)) {
			// TODO Throw an exception here for failing to find project
			// This right here is a big problem!, particularly if we're running in background
			// Need custom exception here
			reader.close();
			System.out.println("We were unable to find the last selected project");
			return false;
		}
		
		// SWAMP Package UUID
		String pkgUUID = null;
		
		str = reader.readLine();
		if (str != null && !str.equals(LINE_SEPARATOR)) {
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
					return false;
				}
			}
		}

		// Build system 
		String buildSys = reader.readLine();
		
		str =  reader.readLine();
		boolean needsBuildFile = false;
		if (str != null && !str.equals(LINE_SEPARATOR)) {
			needsBuildFile = str.equals(NEEDS_BUILD_FILE);
		}
		else {
			return false;
		}
		
		// Build File
		String buildFile = reader.readLine();
		
		// Build Dir
		String buildDir = reader.readLine();
		
		// Build Target
		String buildTarget = reader.readLine();
		
		// Package system libraries
		String pkgSysLibsStr = reader.readLine();
		boolean pkgSysLibs = Boolean.parseBoolean(pkgSysLibsStr);
		
		si.setBuildInfo(buildSys, needsBuildFile, buildDir, buildFile, buildTarget, pkgSysLibs);
		return true;
	}
	
	public static void serializeSelectionInfo(BufferedWriter writer, SubmissionInfo si) throws IOException {
		/*
		// Project UUID
		writer.write(si.getSelectedProjectID());
		writer.write(LINE_SEPARATOR);
		*/
				
		// Platform UUIDs
		List<String> platformIDList = si.getSelectedPlatformIDs();
		String platformIDString = Utils.convertListToDelimitedString(platformIDList, DELIMITER);
		writer.write(platformIDString);
		writer.write(LINE_SEPARATOR);
				
		// Tool UUIDs
		List<String> toolIDList = si.getSelectedToolIDs();
		String toolIDString = Utils.convertListToDelimitedString(toolIDList, DELIMITER);
		writer.write(toolIDString);
		writer.write(LINE_SEPARATOR);
	}
	
	private static void serializeConfigInfo(BufferedWriter writer, SubmissionInfo si) throws IOException {
		// Project UUID
		String projectUUID = si.getSelectedProjectID();
		writer.write(projectUUID);
		writer.write(LINE_SEPARATOR);
		
		// Package Type
		String packageType = si.getPackageType();
		writer.write(packageType);
		writer.write(LINE_SEPARATOR);
		
		// Project name
		String projectName = si.getProjectName();
		writer.write(projectName);
		writer.write(LINE_SEPARATOR);
		
		// Project path
		String projectPath = si.getProjectPath();
		writer.write(projectPath);
		writer.write(LINE_SEPARATOR);
		
		// Package UUID or Package Name
		if (si.isNewPackage()) {
			writer.write(si.getPackageName());
		}
		else {
			writer.write("\t" + si.getSelectedPackageID());
		}
		writer.write(LINE_SEPARATOR);
		
		// Build system
		String buildSystem = si.getBuildSystem();
		writer.write(buildSystem);
		writer.write(LINE_SEPARATOR);
		
		// Make new build file
		if (si.needsBuildFile()) {
			writer.write(NEEDS_BUILD_FILE);
		}
		else {
			writer.write(HAS_BUILD_FILE);
		}
		writer.write(LINE_SEPARATOR);
		
		// Build file
		String buildFile = si.getBuildFile();
		writer.write(buildFile);
		writer.write(LINE_SEPARATOR);
		
		// Build directory
		String buildDirectory = si.getBuildDirectory();
		writer.write(buildDirectory);
		writer.write(LINE_SEPARATOR);
		
		// Build Target
		String buildTarget = si.getBuildTarget();
		writer.write(buildTarget);
		writer.write(LINE_SEPARATOR);
		
		// Package system libraries?
		boolean pkgSysLibs = si.packageSystemLibraries();
		writer.write(Boolean.toString(pkgSysLibs));
		writer.write(LINE_SEPARATOR);
	}
	
	private static void deleteFile(String filepath) {
		File file = new File(filepath);
		file.delete();
	}
	
}
