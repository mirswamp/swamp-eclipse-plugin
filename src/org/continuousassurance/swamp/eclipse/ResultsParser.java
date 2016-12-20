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

import javaSCARF.ScarfInterface;
import javaSCARF.ScarfXmlReader;

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.continuousassurance.swamp.eclipse.ui.TableView;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.HashSet;

import dataStructures.*;

public class ResultsParser implements ScarfInterface {
	
	public static String RESULTS_INFO = "results_info.txt";
	private InitialInfo info;
	private List<BugInstance> bugs;
	private List<Metric> metrics;
	private List<MetricSummary> metricSummaries;
	private List<BugSummary> bugSummaries;
	private List<String[]> rowElements;
	private String tool;
	private String platform;
	private Map<String, List<BugInstance>> fileBugs;
	
	public ResultsParser(File f) {
		ScarfXmlReader reader = new ScarfXmlReader(this);
		bugs = new ArrayList<>();
		metrics = new ArrayList<>();
		metricSummaries = new ArrayList<>();
		bugSummaries = new ArrayList<>();
		rowElements = new ArrayList<>();
		fileBugs = new HashMap<>();
		reader.parseFromFile(f);
	}
	
	@Override
	public void initialCallback(InitialInfo initial) {
		info = initial;
		tool = info.getToolName() + " " + info.getToolVersion();
		platform = "Platform"; // TODO: Find the actual platform
	}
	
	@Override
	public void bugCallback(BugInstance bug) {
		bugs.add(bug);
		for (Location l : bug.getLocations()) {
			String[] elements = new String[TableView.COLUMN_NAMES.length];
			String filename = l.getSourceFile();
			List<BugInstance> bugs;
			if (fileBugs.containsKey(filename)) {
				bugs = fileBugs.get(filename);
			}
			else {
				bugs = new ArrayList<>();
				System.out.println("Filename used: " + filename);
			}
			bugs.add(bug);
			fileBugs.put(filename, bugs);
			elements[0] = filename;
			elements[1] = Integer.toString(l.getStartLine()); 
			elements[2] = Integer.toString(l.getEndLine());
			elements[3] = bug.getBugCode(); // TODO: Is bug type, bug code??
			elements[4] = tool;
			elements[5] = platform;
			rowElements.add(elements);
		}
	}
	
	@Override
	public void metricCallback(Metric metric) {
		metrics.add(metric);
	}
	
	@Override
	public void metricSummaryCallback(MetricSummary summary) {
		metricSummaries.add(summary);
	}
	
	@Override
	public void bugSummaryCallback(BugSummary summary) {
		bugSummaries.add(summary);
	}
	
	public List<String[]> getRows() {
		return rowElements;
	}
	
	public List<BugInstance> getFileBugs(String filename) {
		System.out.println("Filename queried: " + filename);
		if (fileBugs.containsKey(filename)) {
			return fileBugs.get(filename);
		}
		else {
			return new ArrayList<BugInstance>();
		}
	}
	
}
