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

import java.util.ArrayList;
import java.util.List;

import org.continuousassurance.scarf.datastructures.BugInstance;
import org.continuousassurance.scarf.datastructures.Location;

/** This is an object to store detailed information about a bug and its source
 * for display in the BugDetail view
 */
public class BugDetail {
	/**
	 * The BugInstance this BugDetail object is showing information about
	 */
	private BugInstance bug;
	/**
	 * Name of the platform on which the assessment that found this bug was run
	 */
	private final String platform;
	/**
	 * Name of the tool that ran the assessment that found this bug
	 */
	private final String tool;
	/**
	 * Name of the file in which the primary location of the bug takes place
	 */
	private String filename;
	/**
	 * Line number or range of line numbers
	 */
	private String lineNumber;
	/**
	 * List of locations for "flow" information
	 */
	private List<String> flow;
	
	/**
	 * Filler for when a piece of data is not yet known
	 */
	private static String UNSPECIFIED = "?";
	
	/**
	 * Constructor for BugDetail object
	 * @param bug the BugInstance that this object is storing detailed info about
	 * @param platform the name of the platform
	 * @param tool the name of the tool
	 */
	public BugDetail(BugInstance bug, String tool, String platform) {
		this.bug = bug;
		this.platform = platform;
		this.tool = tool;
		filename = UNSPECIFIED;
		lineNumber = UNSPECIFIED;
		flow = new ArrayList<String>();
	}
	
	/**
	 * Formats line number either as a single number (i.e. if start and end
	 * line are same) or as a range
	 * @param l location of the bug instance
	 * @return formatted line number string
	 */
	private static String formatLineNumber(Location l) {
		int start = l.getStartLine();
		int end = l.getEndLine();
		if (start <= 0) {
			return "";
		}
		if (start == end) {
			return Integer.toString(start);
		}
		return Integer.toString(start) + "-" + Integer.toString(end);
	}
	
	/**
	 * Formats single line number
	 * @param ln line number of bug instance
	 * @return formatted line number string
	 */
	public static String formatSingleLineNumber(int ln) {
		if (ln <= 0) {
			return "";
		}
		return Integer.toString(ln);
	}
	
	/**
	 * Getter for bug instance
	 * @return bug instance
	 */
	public BugInstance getBugInstance() {
		return bug;
	}
	
	/**
	 * Getter for platform name
	 * @return platform name
	 */
	public String getPlatform() {
		return platform;
	}
	
	/**
	 * Getter for tool name
	 * @return tool name
	 */
	public String getTool() {
		return tool;
	}
	
	/**
	 * Adds a location to the flow
	 * @param l location to be added
	 */
	public void addLocation(Location l) {
		String fileLocation = l.getSourceFile() + ":" + formatLineNumber(l);
		flow.add(fileLocation);
	}
	
	/**
	 * Setter for filename
	 * @param filename name of the source code file where primary bug location is
	 */
	public void setPrimaryFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Setter for line number
	 * @param l location of primary bug
	 */
	public void setPrimaryLineNumber(Location l) {
		this.lineNumber = formatLineNumber(l);
	}
	
	/**
	 * Getter for flow
	 * @return list of locations for the bug (ordered however the SCARF file is ordered)
	 */
	public List<String> getFlow() {
		return new ArrayList<>(flow);
	}
	
	/**
	 * Getter for file name
	 * @return file name
	 */
	public String getPrimaryFilename() {
		return filename;
	}
	
	/**
	 * Getter for line number
	 * @return line number
	 */
	public String getPrimaryLineNumber() {
		return lineNumber;
	}
}