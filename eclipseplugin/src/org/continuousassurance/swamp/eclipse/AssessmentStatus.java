/*
 * Copyright 2017 Malcolm Reid Jr.
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

public class AssessmentStatus {
	private String packageName;
	private String versionName;
	private String eclipseProjectName;
	private String submissionTime;
	private String status;
	private String numBugs;
	
	private static final String SUBMITTED_STATUS = "Submitted";
	
	public AssessmentStatus(String pkgName, String pkgVersion, String prjName, String time) {
		packageName = pkgName;
		versionName = pkgVersion;
		eclipseProjectName = prjName;
		submissionTime = time;
		status = SUBMITTED_STATUS;
		numBugs = "";
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public String getVersionName() {
		return versionName;
	}
	
	public String getEclipseProjectName() {
		return eclipseProjectName;
	}
	
	public String getSubmissionTime() {
		return submissionTime;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setNumBugs(int count) {
		numBugs = Integer.toString(count);
	}
	
	public String getNumBugs() {
		return numBugs;
	}
	
}
