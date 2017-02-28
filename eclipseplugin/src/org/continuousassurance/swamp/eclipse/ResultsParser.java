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

import javaSCARF.ScarfInterface;
import javaSCARF.ScarfXmlReader;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import dataStructures.*;

public class ResultsParser implements ScarfInterface {
	
	/**
	 * Initial info from SCARF file
	 */
	private InitialInfo info;
	/**
	 * List of bugs in the SCARF file
	 */
	private List<BugInstance> bugs;
	/**
	 * List of metrics in the SCARF file
	 */
	private List<Metric> metrics;
	/**
	 * List of metric summaries in the SCARF file
	 */
	private List<MetricSummary> metricSummaries;
	/**
	 * List of bug summaries in the SCARF file
	 */
	private List<BugSummary> bugSummaries;
	/**
	 * Name of tool that ran assessment
	 */
	private String tool;
	/**
	 * Name of platform on which assessment was run
	 */
	private String platform;
	/**
	 * Map from source file name to a list of bugs found in that file
	 */
	private Map<String, List<BugInstance>> fileBugs;
	
	/**
	 * Constructor for ResultsParser
	 * @param f SCARF file being read
	 */
	public ResultsParser(File f) {
		ScarfXmlReader reader = new ScarfXmlReader(this);
		bugs = new ArrayList<>();
		metrics = new ArrayList<>();
		metricSummaries = new ArrayList<>();
		bugSummaries = new ArrayList<>();
		fileBugs = new HashMap<>();
		reader.parseFromFile(f);
	}
	
	@Override
	/**
	 * Callback for initial info in SCARF
	 * @param initial InitialInfo object
	 */
	public void initialCallback(InitialInfo initial) {
		info = initial;
		tool = info.getToolName() + " " + info.getToolVersion();
		platform = "?"; // TODO: Get the actual Platform once SCARF is updated
	}
	
	@Override
	/**
	 * Callback for bug in SCARF
	 * @param bug BugInstance that was just parsed from SCARF file
	 */
	public void bugCallback(BugInstance bug) {
		for (Location l : bug.getLocations()) {
			if (l.isPrimary()) {
				String filename = l.getSourceFile();
				if (fileBugs.containsKey(filename)) {
					bugs = fileBugs.get(filename);
				}
				else {
					bugs = new ArrayList<>();
					//System.out.println("Filename used: " + filename);
				}
				bugs.add(bug);
				fileBugs.put(filename, bugs);
				break;
			}
		}
	}
	
	@Override
	/**
	 * Callback for metric in SCARF
	 * @param metric Metric that was just parsed from SCARF file
	 */
	public void metricCallback(Metric metric) {
		metrics.add(metric);
	}
	
	@Override
	/**
	 * Callback for metric summary in SCARF
	 * @param summary MetricSummary that was just parsed from SCARF file
	 */
	public void metricSummaryCallback(MetricSummary summary) {
		metricSummaries.add(summary);
	}
	
	@Override
	public void bugSummaryCallback(BugSummary summary) {
		bugSummaries.add(summary);
	}
	
	/**
	 * Given the name of a file, returns the bugs found in that file
	 * @param filename source code file name
	 * @return list of bugs found in that file
	 */
	public List<BugInstance> getFileBugs(String filename) {
		//System.out.println("Filename queried: " + filename);
		if (fileBugs.containsKey(filename)) {
			return fileBugs.get(filename);
		}
		else {
			return new ArrayList<BugInstance>();
		}
	}
	
	/**
	 * Getter for tool name
	 * @return name of tool that ran the assessment
	 */
	public String getToolName() {
		return tool;
	}
	
	/**
	 * Getter for platform name
	 * @return name of platform assessment was run on
	 */
	public String getPlatformName() {
		return platform;
	}
}