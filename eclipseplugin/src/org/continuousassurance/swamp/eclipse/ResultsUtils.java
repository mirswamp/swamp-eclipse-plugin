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

/**
 * Utility functions for results handling and retrieval
 * @author reid-jr
 *
 */
public class ResultsUtils {

	/**
	 * End of SCARF file results name
	 */
	public static final String FILE_SUFFIX = "results.xml";
	
	/**
	 * Name of file storing the mapping from Eclipse project to SWAMP package
	 */
	public static final String ECLIPSE_TO_SWAMP_FILENAME = "eclipse-swamp.txt";
	
	/**
	 * System-defined directory separator (e.g. "/" in Unix, Linux)
	 */
	public static final String SEPARATOR = System.getProperty("file.separator");
	
	/**
	 * Eclipse property name for user's home directory
	 */
	private static final String USER_HOME_PROPERTY = "user.home";
	
	/**
	 * Construct file path for SCARF results for a given assessment
	 * @param projectUUID project UUID for the SWAMP project that the 
	 * assessment submitted
	 * @param pkgUUID packageThing UUID for the SWAMP package that the 
	 * assessment submitted
	 * @param toolUUID tool UUID for the tool that ran the assessment
	 * @param platformUUID platform UUID for the platform that the assessment 
	 * was run on
	 * @return file path that the SCARF results should be downloaded to
	 */
	public static String constructFilepath(String projectUUID, String pkgUUID, String toolUUID, String platformUUID) {
		return constructFilepath(pkgUUID) + SEPARATOR + toolUUID + "-" + platformUUID + "-" + FILE_SUFFIX;
	}
	
	/**
	 * Constructs file path for the directory for SCARF results for assessments
	 * submitted of a given Package Thing
	 * @param pkgThingUUID 
	 * @return file path of the directory in which results for assessments on
	 * the specified Package Thing should be downloaded
	 */
	public static String constructFilepath(String pkgThingUUID) {
		return getTopLevelResultsDirectory() + SEPARATOR + pkgThingUUID;
	}
	
	/**
	 * Constructs file path for the directory for SCARF results for assessments
	 * @return file path of the directory in which SCARF results should be
	 * downloaded
	 */
	public static String getTopLevelResultsDirectory() {
		return System.getProperty(USER_HOME_PROPERTY) + SEPARATOR + SwampSubmitter.SWAMP_RESULTS_DIRNAME;
	}
}