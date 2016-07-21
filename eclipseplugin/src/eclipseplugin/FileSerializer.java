package eclipseplugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.eclipse.core.resources.IProject;

import eclipseplugin.dialogs.ConfigDialog;
import eclipseplugin.dialogs.SelectionDialog;
import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;

public class FileSerializer {
	
	private static String DELIMITER = ",";
	public static boolean serialize(String filepath, SelectionDialog sd, ConfigDialog cd, String prjUUID) {
		File file = new File(filepath);
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		FileWriter filewriter;
		BufferedWriter writer = null;
		try {
			filewriter = new FileWriter(file.getAbsoluteFile());
			writer = new BufferedWriter(filewriter);
			// Project Index
			writer.write(Integer.toString(sd.getProjectIndex()));
			writer.write("\n");
			// Platform Indices
			int[] platformAry = sd.getPlatformIndices();
			if (platformAry != null) {
				for (int i : platformAry) {
					writer.write(Integer.toString(i));
					writer.write(DELIMITER);
				}
			}
			writer.write("\n");
			// Tool Indices
			int[] toolAry = sd.getToolIndices();
			if (toolAry != null) {
				for (int i : toolAry) {
					writer.write(Integer.toString(i));
					writer.write(DELIMITER);
				}
			}
			writer.write("\n");
		}
		catch (Exception e) {
			System.err.println("Unable to serialize file");
			e.printStackTrace();
			sd.resetState();
			return false;
		}
		
		try {
			// Package (IProject) name
			IProject project = cd.getProject();
			writer.write(project.getName());
			writer.write("\n");
			// Project (IProject) filepath
			writer.write(project.getLocation().toString());
			writer.write("\n");
			// SWAMP Project
			writer.write(prjUUID);
			writer.write("\n");
			// SWAMP Package
			String pkgUUID = cd.getPkgThingUUID();
			if (pkgUUID != null) {
				writer.write("\t" + pkgUUID);
				writer.write("\n");
			}
			else {
				String pkgName = cd.getPkgName();
				writer.write(pkgName);
				writer.write("\n");
			}
			/*
			// Package Version
			writer.write(cd.getPkgVersion());
			writer.write("\n");
			*/
			// Build system index
			writer.write(Integer.toString(cd.getBuildSysIndex()));
			writer.write("\n");
			// Build dir
			writer.write(cd.getBuildDir());
			writer.write("\n");
			// Build file
			writer.write(cd.getBuildFile());
			writer.write("\n");
			// Build target
			writer.write(cd.getBuildTarget());
			writer.write("\n");
			writer.close();
		} catch (Exception e) {
			System.err.println("Unable to deserialize file");
			e.printStackTrace();
			return false;
		}
		return true;
		
	}
	
	public static boolean deserialize(String filepath, SelectionDialog sd, ConfigDialog cd) {
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
			String str = "";
			// Project Index
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				sd.setProjectIndex(Integer.parseInt(str));
			}
			// Platform Indices
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				int[] intAry = FileSerializer.readIntArray(str);
				sd.setPlatformIndices(intAry);
			}
			// Tool Indices
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				int[] intAry = FileSerializer.readIntArray(str);
				sd.setToolIndices(intAry);
			}
			
			// Eclipse Project Name
			String pkgName = null, pkgPath = null;
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				pkgName = str;
			}
			// Eclipse Project Path
			str = reader.readLine();
			if (str != null & !str.equals("\n")) {
				pkgPath = str;
			}
			if (!cd.initializeProject(pkgName, pkgPath)) {
				reader.close();
				System.out.println("We were unable to find the last selected project");
				return false;
			}
			
			// SWAMP Package UUID
			String pkgUUID = null;
			String prjUUID = null;
			
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				prjUUID = str;
			}
			
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				char c = str.charAt(0);
				if (c == '\t') {
					pkgUUID = str.substring(1, str.length());
					if (!cd.initializePackageWithID(pkgUUID, prjUUID)) {
						reader.close();
						System.out.println("We were unable to find the last SWAMP Package");
						return false;
					}
				}
				else {
					String name = str;
					if (!cd.initializePackageWithName(name, prjUUID)) {
						reader.close();
						System.out.println("We were unable to find the last SWAMP Package");
						return false;
					}
				}
			}

			// Package Version
			//str = reader.readLine();
			/*
			if (str != null && !str.equals("\n")) {
				cd.setPkgVersion(str);
			}
			*/
			// initialize version with date/time stamp
			//String timestamp = new Date().toString();
			//System.out.println("Timestamp: " + timestamp);
			//cd.setPkgVersion(timestamp);
			
			int buildSysIndex = -1;
			String buildFile = null;
			String buildDir = null;
			String buildTarget = null;
			// Build system index
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				buildSysIndex = Integer.parseInt(str);
			}
			// Build File
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				buildFile = str;
			}
			// Build Dir
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				buildDir = str;
			}
			// Build Target
			str = reader.readLine();
			if (str != null && !str.equals("\n")) {
				buildTarget = str;
			}
			cd.initializeBuild(buildSysIndex, buildFile, buildDir, buildTarget);
			reader.close();
		}
		catch (Exception e) {
			System.err.println("Unable to deserialize file");
			cd.resetState();
			e.printStackTrace();
			return false;
		}
		return true;
		
	}

	public static void deleteFile(String filepath) {
		File file = new File(filepath);
		file.delete();
	}
	
	private static int[] readIntArray(String line) {
		String[] ary = line.split(DELIMITER);
		int[] intAry = new int[ary.length];
		for (int i = 0; i < ary.length; i++) {
			intAry[i] = Integer.parseInt(ary[i]);
		}
		return intAry;
	}
}
