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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.continuousassurance.swamp.eclipse.ui.SortListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import static org.eclipse.core.runtime.IPath.SEPARATOR;

/**
 * This class provides static utility methods that provide basic functionality 
 * for the SWAMP plug-in.
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class Utils {
	
	/**
	 * Name of int type for table column
	 */
	public static String INT_TYPE = "INT";
	
	/**
	 * Name of string type for table column 
	 */
	public static String STR_TYPE = "STR";
	
	/**
	 * Private constructor to prevent subclassing
	 */
	private Utils() {
	}

	/**
	 * Generates a timestamp without spaces for the current time 
	 *
	 * @return a timestamp for the current time
	 */
	public static String getCurrentTimestamp() {
		Timestamp timestamp = new Timestamp(new Date().getTime());
		String timeString = timestamp.toString();
		timeString = timeString.substring(0, timeString.lastIndexOf('.'));
		timeString = timeString.replace(" ","-");
		return timeString;
	}
	
	/**
	 * Generates a bracketed timestamp for the current time 
	 *
	 * @return a bracketed timestamp for the current time
	 */
	public static String getBracketedTimestamp() {
		return "[" + getCurrentTimestamp() + "] ";
	}
	
	/**
	 * Converts a List of Strings to a delimited String
	 *
	 * @param list the list of Strings
	 * @param delimiter the delimiter to be used between elements of
	 * the list
	 * @return the delimited String
	 */
	public static String convertListToDelimitedString(List<String> list, String delimiter) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < list.size()-1; i++) {
			sb.append(list.get(i));
			sb.append(delimiter);
		}
		sb.append(list.get(list.size()-1));
		return sb.toString();
	}
	
	/**
	 * Converts a delimited String into a List of Strings
	 *
	 * @param delimitedString the delimited String
	 * @param delimiter the String that delimits separate elements in the delimited String
	 * @return list of Strings
	 */
	public static List<String> convertDelimitedStringToList(String delimitedString, String delimiter) {
		String[] array = delimitedString.split(delimiter);
		List<String> list = new ArrayList<String>(array.length);
		list = Arrays.asList(array);
		return list;
	}
	
	/**
	 * Converts a List of Integers into an array of ints
	 *
	 * @param list the list of Integer objects
	 * @return an array of ints
	 */
	public static int[] convertIntListToArray(List<Integer> list) {
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	/**
	 * Converts an array of ints into a List of Integers
	 *
	 * @param array the array of ints
	 * @return the List of Integer objects
	 */
	public static List<Integer> convertIntArrayToList(int[] array) {
		int size = array.length;
		List<Integer> list = new ArrayList<Integer>(size);
		for (int i = 0; i < size; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	/**
	 * Converts a List of Strings into an array of Strings
	 *
	 * @param list the list of String objects
	 * @return the array of String objects
	 */
	public static String[] convertStringListToArray(List<String> list) {
		String[] ary = null;
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
	
	/**
	 * Zips files and writes them
	 * @param files set of paths of files to zip
	 * @param zipFilePath path of directory where the newly created .zip will be written
	 * @param zipFileName name of the .zip file
	 * @return path of the .zip file
	 */
	public static java.nio.file.Path zipFiles(Set<String> files, String zipFilePath, String zipFileName) {
		String finalPath = zipFilePath + SEPARATOR + zipFileName;
		FileOutputStream fileOS = null;
		try {
			fileOS = new FileOutputStream(finalPath);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(fileOS));
			// All of these files should be top level in the generated archive
			File f;
			String lastSegment;
			for (String file : files) {
				lastSegment = new Path(file).lastSegment();
				f = new File(file);
				System.out.println(file);
				if (f.isDirectory()) {
					addEntries(file, lastSegment, out);
				}
				else {
					addFileToZip(file, lastSegment, out);
					
				}
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Paths.get(finalPath);
	}
	
	/**
	 * Method for adding entries from a directory to the zip output stream
	 *
	 * @param pathname the path for the directory
	 * @param basePath the segment that this is relative to
	 * @param out the output stream for the zip file being built
	 */
	private static void addEntries(String pathname, String basePath, ZipOutputStream out) {
		File file = new File(pathname);
		String filename;
		String files[] = file.list();
		
		if (files == null || files.length == 0) {
			return;
		}
		
		File f;
		for (int i = 0; i < files.length; i++) {
			filename = pathname + SEPARATOR + files[i];
			f = new File(filename);
			if (f.isDirectory()) {
				addEntries(filename, "".equals(basePath) ? files[i]:basePath + SEPARATOR + files[i], out);
			}
			else {
				System.out.println("File name: " + filename);
				addFileToZip(filename, basePath + SEPARATOR + files[i], out);
			}
		}
	}
	
	/**
	 * Private method to add file to the zip file being built
	 *
	 * @param pathname the pathname of the file
	 * @param relPath the relative path of the entry
	 * @param out the output stream for the zip file being built
	 */
	private static void addFileToZip(String pathname, String relPath, ZipOutputStream out) {
		int BUF_SIZE = 2048;
		byte data[] = new byte[BUF_SIZE];
		try {
			FileInputStream fi = new FileInputStream(pathname);
			BufferedInputStream in = new BufferedInputStream(fi, 2048);
			ZipEntry entry = new ZipEntry(relPath);
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
	
	/**
	 * Utility method for getting the directory of a project
	 * @param p project
	 * @return directory of the project
	 */
	public static String getProjectDirectory(IProject p) {
		IPath path = p.getLocation();
		return path.lastSegment();
	}
	
	/**
	 * Utility method for generating a comparator that orders rows by integer value
	 * @param col the number of the column
	 * @return a comparator that orders rows of a table (i.e. TableItems) by the integer value found in the specified column
	 */
	public static Comparator<TableItem> INT_CMP(int col) {
		Comparator<TableItem> cmp = new Comparator<TableItem>() {
			@Override
			public int compare(TableItem t1, TableItem t2) {
				int i1 = getIntValue(t1.getText(col));
				int i2 = getIntValue(t2.getText(col));
				return Integer.compare(i1, i2);
			}
		};
		return cmp;
	}
	
	/**
	 * Utility method for getting the int value of a string that may or may not
	 * be numeric
	 * @param str string to be converted
	 * @return int value of the string, -1 if non-numeric
	 */
	private static int getIntValue(String str) {
		int val = -1;
		try {
			val = Integer.parseInt(str);
		}
		catch (NumberFormatException e) {
		}
		return val;
	}
	
	/**
	 * Utility method for generating a comparator that orders rows by string value
	 * @param col the number of the column
	 * @return a comparator that orders rows of a table (i.e. TableItems) by the string value found in the specified column
	 */
	public static Comparator<TableItem> STR_CMP(int col) {
		Comparator<TableItem> cmp = new Comparator<TableItem>() {
			@Override
			public int compare(TableItem t1, TableItem t2) {
				String t1Caps = t1.getText(col).toUpperCase();
				String t2Caps = t2.getText(col).toUpperCase();
				return t1Caps.compareTo(t2Caps);
			}
		};
		return cmp;
	}
	
	/**
	 * Utility method for constructing a table to be used in a view
	 * @param parent composite that the table will be placed on
	 * @return SWT table
	 */
	public static Table constructTable(Composite parent) {
		Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(gd);
		return table;
	}
	
	/**
	 * Utility method for constructing a column and adding it to the specified table
	 * @param table SWT table
	 * @param name name of the column
	 * @param width width of column
	 * @param index column index in table
	 * @param type whether this column should be string or int sorted
	 * @param dataKeys keys for arbitrary data objects associated with this column
	 * @return sortable table column
	 */
	public static TableColumn addTableColumn(Table table, String name, int width, int index, String type, String... dataKeys) {		
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(name);
		col.setWidth(width);
		if (type.equals(INT_TYPE)) {
			col.setData(Utils.INT_CMP(index));
		}
		else {
			col.setData(Utils.STR_CMP(index));
		}
		col.addListener(SWT.Selection, new SortListener(dataKeys));
		return col;
		
	}

}