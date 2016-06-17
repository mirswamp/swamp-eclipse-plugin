package eclipseplugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.codec.binary.Hex;

import org.eclipse.core.runtime.IPath;

public class PackageInfo {

	private String shortName;
	private String version;
	// Zip File
	private String zipPath;
	private String md5hash;
	private String sha512hash;
	private String pkgDir;
	private String buildSys;
	private String buildTarget;
	private String parentDir;
	
	public PackageInfo(String dirPath, String outputName) {
		
		pkgDir = dirPath;
		zipPath = zipPackage(dirPath, outputName);
		
		MessageDigest md5 = null;
		MessageDigest sha512 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
			sha512 = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Path to get: " + zipPath);
		Path path = Paths.get(zipPath);
		byte[] zipBytes;
		try {
			zipBytes = Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		md5hash = getDigest(zipBytes, md5);
		sha512hash = getDigest(zipBytes, sha512);
	}
	
	private String getDigest(byte[] bytes, MessageDigest m) {
		byte[] retArray = m.digest(bytes);
		return Hex.encodeHexString(retArray);
	}
	
	public void setPkgShortName(String name) {
		shortName = name;
	}
	
	public void setVersion(String v) {
		version = v;
	}
	
	public void setBuildSys(String build) {
		buildSys = build;
	}
	
	public void setBuildTarget(String target) {
		buildTarget = target;
	}
	
	public String getParentPath() {
		return parentDir;
	}
	
	/* Adapted from example code provided at http://www.oracle.com/technetwork/articles/java/compress-1565076.html */
	private String zipPackage(String dirPath, String outputName) {
		String finalPath = "";
		System.out.println("Writing a zip file of " + dirPath + " to file " + outputName);
		// this needs to zip the directory specified by Path dir and return the path to the zipped file
		try {
			IPath path = new org.eclipse.core.runtime.Path(dirPath);
			IPath parentPath = path.removeLastSegments(1);
			parentDir = parentPath.toString();
			String lastSegment = path.lastSegment();
			System.out.println("Last segment: " + lastSegment);
			//System.out.println("String dirPath: " + dirPath);
			//System.out.println("Parent path: " + parentPath);
			//System.out.println("Child path: " + path);
			finalPath = parentDir + "/" + outputName;
			FileOutputStream fileOS = new FileOutputStream(finalPath);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(fileOS));
			addEntries(dirPath, lastSegment, out);
			out.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return finalPath;
	}
	
	private void addEntries(String pathname, String basePath, ZipOutputStream out) {
		File file = new File(pathname);
		String filename;
		String files[] = file.list();
		System.out.println("Adding entries from: " + basePath);
		for (int i = 0; i < files.length; i++) {
			filename = pathname + "/" + files[i];
			System.out.println("Filename: " + filename);
			File f = new File(pathname + "/" + files[i]);
			System.out.println(f);
			if (f.isDirectory()) {
				addEntries(filename, basePath + "/" + files[i],out);
			}
			else {
				addFileToZip(filename, basePath + "/" + files[i],out);
			}
		}
	}
	
	private void addFileToZip(String pathname, String relPath, ZipOutputStream out) {
		int BUF_SIZE = 2048;
		byte data[] = new byte[BUF_SIZE];
		try {
			System.out.println("Reading input from: " + pathname);
			FileInputStream fi = new FileInputStream(pathname);
			BufferedInputStream in = new BufferedInputStream(fi, 2048);
			ZipEntry entry = new ZipEntry(relPath);
			out.putNextEntry(entry);
			int cnt;
			while ((cnt = in.read(data, 0, BUF_SIZE)) != -1) {
				System.out.println(cnt + " bytes read");
				out.write(data, 0, cnt);
			}
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writePkgConfFile() {
		File pkgConf = new File(parentDir + "/package.conf");
		//System.out.println(pkgConf.getAbsolutePath());
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(pkgConf.getAbsolutePath(), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println("package-short-name=" + shortName);
		writer.println("package-version=" + version);
		writer.println("package-archive=" + zipPath);
		writer.println("package-archive-md5=" + md5hash);
		writer.println("package-archive-sha512=" + sha512hash);
		writer.println("package-dir=" + pkgDir);
		writer.println("package-language=" + "Java");
		writer.println("build-sys=" + buildSys);
		writer.println("build-target=" + buildTarget);
		writer.close();
	}
}
