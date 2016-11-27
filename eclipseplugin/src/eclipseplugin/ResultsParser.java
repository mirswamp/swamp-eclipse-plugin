package eclipseplugin;

import javaSCARF.ScarfInterface;
import javaSCARF.ScarfXmlReader;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.swt.widgets.TableItem;

import java.util.HashSet;

import dataStructures.*;
import eclipseplugin.ui.TableView;

public class ResultsParser implements ScarfInterface {
	
	private InitialInfo info;
	private List<BugInstance> bugs;
	private List<Metric> metrics;
	private List<MetricSummary> metricSummaries;
	private List<BugSummary> bugSummaries;
	private Set<TableItem> tableRows;
	private List<String[]> rowElements;
	private String tool;
	private String platform;
	
	public ResultsParser(File f) {
		ScarfXmlReader reader = new ScarfXmlReader(this);
		bugs = new ArrayList<>();
		metrics = new ArrayList<>();
		metricSummaries = new ArrayList<>();
		bugSummaries = new ArrayList<>();
		tableRows = new HashSet<>();
		rowElements = new ArrayList<>();
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
			elements[0] = l.getSourceFile();
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
	
	
	
	
}
