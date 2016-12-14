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

package org.continuousassurance.swamp.eclipse;

public class ResultsUtils {

	public static String FILE_SUFFIX = "results.xml";
	
	public static String ECLIPSE_TO_SWAMP_FILENAME = "eclipse-swamp.txt";
	
	private static String SEPARATOR = System.getProperty("file.separator");
	
	public static String constructFilepath(String projectUUID, String pkgUUID, String toolUUID, String platformUUID) {
		// TODO: get plugin location + SEPARATOR + projectID + "-" + assessID + ".results";
		System.out.println("Project UUID: " + projectUUID);
		System.out.println("Package UUID: " + pkgUUID);
		System.out.println("Tool UUID: " + toolUUID);
		System.out.println("Platform UUID: " + platformUUID);
		// return System.getProperty("user.home") + SEPARATOR + SwampSubmitter.SWAMP_RESULTS_DIRNAME + SEPARATOR + projectUUID + SEPARATOR + pkgUUID + SEPARATOR + toolUUID + "-" + platformUUID + "-" + "results.xml";
		return constructFilepath(pkgUUID) + SEPARATOR + toolUUID + "-" + platformUUID + "-" + FILE_SUFFIX;
	}
	
	public static String constructFilepath(String pkgThingUUID) {
		return System.getProperty("user.home") + SEPARATOR + SwampSubmitter.SWAMP_RESULTS_DIRNAME + SEPARATOR + pkgThingUUID;
	}
}