package org.continuousassurance.swamp.eclipse;

import java.io.*; 
import java.util.*; 
import org.apache.commons.compress.archivers.tar.*; 
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

public class TarUtils {
	
	static public int getFilePermissions(File file) {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			return getWindowsFilePermissions(file);
		}else {
			return getPosixFilePermissions(file);
		}
	}
	
	static public int getWindowsFilePermissions(File file) {
		int mode = 0;
		
		if (file.canExecute()) {
			mode = mode | 0100;
			mode = mode | 0010;
		}
		
		if (file.canRead()) {
			mode = mode | 0400;
			mode = mode | 0040;
		}
		
		if (file.canWrite()) {
			mode = mode | 0200;
			mode = mode | 0020;
		}
		
		return mode;
	}
	
	
	static public int getPosixFilePermissions(File file) {
		int mode = 0;
		
		try {
			for (PosixFilePermission perm: Files.getPosixFilePermissions(file.toPath())) {
				switch (perm){
				case OWNER_READ:
					mode = mode | 0400;
					break;
				case OWNER_WRITE:
					mode = mode | 0200;
					break;
				case OWNER_EXECUTE:
					mode = mode | 0100;
					break;
				case GROUP_READ:
					mode = mode | 0040;
					break;
				case GROUP_WRITE:
					mode = mode | 0020;
					break;
				case GROUP_EXECUTE:
					mode = mode | 0010;
					break;
				case OTHERS_READ:
					mode = mode | 004;
					break;
				case OTHERS_WRITE:
					mode = mode | 002;
					break;
				case OTHERS_EXECUTE:
					mode = mode | 001;
					break;
				default:
					break;
				}
			}
		} catch (IOException e) {
			return (0400 | 0200 | 0040 | 0020);
		}
		
		return mode;
	}
	
	
	//static public void createTarGzip(File inputDirectory,  File outputFile) throws IOException {
	static public java.nio.file.Path createTarGzip(Set<String> fileSet, 
			String zipFilePath, 
			String zipFileName) throws IOException  {
		String outputFile = zipFilePath + File.separator + zipFileName;
		
		System.out.println(outputFile);
		
	    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
	            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
	            GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(bufferedOutputStream);
	            TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(gzipOutputStream)) {

	        tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
	        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
	        
	        for (String file_name : fileSet) {
	        
		        	File file = new File(file_name);
		        	
		        	if (file.isDirectory()) {
				        List<File> files = new ArrayList<>(FileUtils.listFilesAndDirs(file,
				        		FileFilterHelper.createDefaultFilter(),
				        		FileFilterHelper.createDefaultFilter()));
			
				        for (int i = 0; i < files.size(); i++) {
				            File currentFile = files.get(i);
				            
				            Path relativeFilePath = file.toPath().getParent().relativize(currentFile.toPath().toAbsolutePath());
			
				            TarArchiveEntry tarEntry = new TarArchiveEntry(currentFile, relativeFilePath.toString());
				            tarEntry.setSize(currentFile.length());
				            tarEntry.setMode(getFilePermissions(currentFile));
				            tarArchiveOutputStream.putArchiveEntry(tarEntry);
				            if (currentFile.isFile()) {
				            		tarArchiveOutputStream.write(IOUtils.toByteArray(new FileInputStream(currentFile)));
				            }
				            tarArchiveOutputStream.closeArchiveEntry();
				        }
		        	}else{
		        		File currentFile = file;
			            Path relativeFilePath = file.toPath().getParent().relativize(currentFile.toPath().toAbsolutePath());
			            TarArchiveEntry tarEntry = new TarArchiveEntry(currentFile, relativeFilePath.toString());
			            tarEntry.setSize(currentFile.length());
			            tarEntry.setMode(getFilePermissions(currentFile));
			            tarArchiveOutputStream.putArchiveEntry(tarEntry);
			            tarArchiveOutputStream.write(IOUtils.toByteArray(new FileInputStream(currentFile)));
			            tarArchiveOutputStream.closeArchiveEntry();
		        	}
	        }
	        
	        tarArchiveOutputStream.close();
	    }
	    return FileUtils.getFile(outputFile).toPath();
	}
}