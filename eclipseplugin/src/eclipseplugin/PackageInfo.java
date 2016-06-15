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

public class PackageInfo {

	private String shortName;
	private String version;
	// Zip File
	private String zipPath;
	private String pkgPath;
	private String md5hash;
	private String sha512hash;
	private String pkgDir;
	private String buildSys;
	private String buildTarget;
	
	public PackageInfo(String dirPath, String outputName) {
		
		zipPackage(dirPath, outputName);
		
		MessageDigest md5 = null;
		MessageDigest sha512 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
			sha512 = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Path path = Paths.get(outputName);
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
		return retArray.toString();
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
	
	/* Adapted from example code provided at http://www.oracle.com/technetwork/articles/java/compress-1565076.html */
	private void zipPackage(String dirPath, String outputName) {
		// this needs to zip the directory specified by Path dir and return the path to the zipped file
		try {
			FileOutputStream fileOS = new FileOutputStream(outputName);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(fileOS));
			addEntries(dirPath, out);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void addEntries(String pathname, ZipOutputStream out) {
		File file = new File(pathname);
		String files[] = file.list();
		for (int i = 0; i < files.length; i++) {
			File f = new File(files[i]);
			if (f.isDirectory()) {
				addEntries(files[i], out);
			}
			else {
				addFileToZip(files[i], out);
			}
		}
	}
	
	private void addFileToZip(String pathname, ZipOutputStream out) {
		int BUF_SIZE = 2048;
		byte data[] = new byte[BUF_SIZE];
		try {
			FileInputStream fi = new FileInputStream(pathname);
			BufferedInputStream in = new BufferedInputStream(fi, 2048);
			ZipEntry entry = new ZipEntry(pathname);
			out.putNextEntry(entry);
			int cnt;
			while ((cnt = in.read(data, 0, BUF_SIZE)) != -1) {
				out.write(data, 0, cnt);
			}
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writePkgConfFile() {
		File pkgConf = new File("package.conf");
		System.out.println(pkgConf.getAbsolutePath());
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
		writer.println("package-dir=" + pkgPath);
		writer.println("package-language=" + "Java");
		writer.println("build-sys=" + buildSys);
		writer.println("build-target=" + buildTarget);
		writer.close();
	}
}
