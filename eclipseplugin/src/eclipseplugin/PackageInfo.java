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
import static org.eclipse.core.runtime.Path.SEPARATOR;

import org.eclipse.core.runtime.IPath;

/**
 * This class process information and generates the package.conf
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 06/2016 
 */
public class PackageInfo {
	
	public static final String PACKAGE_CONF_NAME = "package.conf";

	
	private static String getDigest(byte[] bytes, MessageDigest m) {
		byte[] retArray = m.digest(bytes);
		return Hex.encodeHexString(retArray);
	}
	
	public static File generatePkgConfFile(Path archivePath, String outputDir, String shortName, String version, String pkgDir, String pkgType, String buildSys, String buildDir, String buildFile, String buildTarget) {
		MessageDigest md5 = null;
		MessageDigest sha512 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
			sha512 = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Path path = Paths.get(archivePath.toString());
		String archiveName = path.getFileName().toString();
		byte[] zipBytes;
		try {
			zipBytes = Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String md5hash = getDigest(zipBytes, md5);
		String sha512hash = getDigest(zipBytes, sha512);
		
		File pkgConf = new File(outputDir + SEPARATOR + PACKAGE_CONF_NAME);
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
		writer.println("package-archive=" + archiveName);
		writer.println("package-archive-md5=" + md5hash);
		writer.println("package-archive-sha512=" + sha512hash);
		writer.println("package-dir=" + pkgDir);
		writer.println("package-language=" + "Java");
		writer.println("package-language-version=" + pkgType);
		writer.println("build-sys=" + buildSys);
		if (!buildDir.equals("")) {
			System.out.println("Build dir: " + buildDir);
			System.out.println("Build file: " + buildFile);
			System.out.println("Build target: " + buildTarget);
			writer.println("build-dir=" + buildDir);
			writer.println("build-file=" + buildFile);
			writer.println("build-target=" + buildTarget);
		}
		writer.close();
		return pkgConf;
	}
}