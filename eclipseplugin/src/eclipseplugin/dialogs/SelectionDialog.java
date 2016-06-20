/* Malcolm Reid Jr.
 * 06/10/2016
 * UW SWAMP
 * SelectionDialog.java
 * Code to implement a dialog box for getting package, tool, and platform from end user
 */

package eclipseplugin.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
//import org.eclipse.jface.viewers.ILabelProvider;
//import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.widgets.*;
//import org.eclipse.ui.dialogs.FilteredList;
//import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import java.util.ArrayList;
import java.util.List;

import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.api.Tool;
//import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.uiuc.ncsa.swamp.api.Platform;

public class SelectionDialog extends TitleAreaDialog {
	
	//private ArrayList<String> projectList;
	//private ArrayList<String> platformList;
	//private ArrayList<String> toolList;
	private Combo projCombo;
	private Combo platCombo;
	private Combo toolCombo;
	private List<? extends Project> projects;
	private List<? extends Tool> tools;
	private List<? extends Platform> platforms;
	private int prjIndex;
	private int pltIndex;
	private int toolIndex;
	private SwampApiWrapper api;

	private enum Type {
		PROJECT, PLATFORM, TOOL
	}
	
	public SelectionDialog(Shell parentShell) {
		super(parentShell);
		//projectList = new ArrayList<String>();
		//platformList = new ArrayList<String>();
		//toolList = new ArrayList<String>();
	}
	
	public void setSwampApiWrapper(SwampApiWrapper w) {
		api = w;
	}
	private String[] getComboElements(Type type) {
		ArrayList<String> stringList = new ArrayList<String>();
		if (type == Type.PROJECT) {
			
			//list = ParseCommandLine.getProjectList(handler);
			projects = api.getAllProjects();
			for (Project p : projects) {
				stringList.add(p.getFullName());
			}
		}
		else if (type == Type.PLATFORM) {
			//list = ParseCommandLine.getPlatformList(handler);
			platforms = api.getAllPlatforms();
			for (Platform p : platforms) {
				stringList.add(p.getName());
			}
		}
		else {
			tools = api.getAllTools();
			for (Tool t : tools) {
				stringList.add(t.getName());
			}
		}
		return getComboList(stringList);
	}
	
	private String[] getComboList(ArrayList<String> list) {
		String[] ary = null;
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
	
	public String getProjectUUID() {
		if (prjIndex < 0) { 
			return null;
		}
		return projects.get(prjIndex).getUUIDString();
	}
	
	public String getPlatformUUID() {
		if (pltIndex < 0) {
			return null;
		}
		return platforms.get(pltIndex).getUUIDString();
	}
	
	public String getToolUUID() {
		if (toolIndex < 0) {
			return null;
		}
		return tools.get(toolIndex).getUUIDString();
	}
	
	@Override
	protected void okPressed() {
		// Here we do some checks to make sure that everything has actually been populated
		if (projCombo.getSelectionIndex() < 0) {
			this.setMessage("Invalid project selected.");
		}
		else if (platCombo.getSelectionIndex() < 0) {
			this.setMessage("Invalid platform selected.");
		}
		else if (toolCombo.getSelectionIndex() < 0) {
			this.setMessage("Invalid tool selected.");
		}
		else {
			super.okPressed();
		}
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Project Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		GridData lblGridData = new GridData();
		lblGridData.horizontalAlignment = SWT.FILL;
		lblGridData.grabExcessHorizontalSpace = true;
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		
		projCombo = addCombo(container, "Project: ", Type.PROJECT, lblGridData, gd);
		platCombo = addCombo(container, "Platform: ", Type.PLATFORM, lblGridData, gd);
		toolCombo = addCombo(container, "Tool: ", Type.TOOL, lblGridData, gd);

		return area;
		
	}
	
	private Combo addCombo(Composite container, String labelText, Type type, GridData lblGridData, GridData comboGridData) {
		DialogUtil.initializeLabelWidget(labelText, SWT.NONE, container, lblGridData);
		String[] comboOptions = getComboElements(type);
		Combo c = DialogUtil.initializeComboWidget(container, comboGridData, comboOptions);
		c.addSelectionListener(new ComboSelectionListener(c, type));
		return c;
	}
	
	private class ComboSelectionListener implements SelectionListener {
		Combo combo;
		Type type;
		public ComboSelectionListener(Combo c, Type t) {
			combo = c;
			type = t;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
			if (type == Type.PROJECT) {
				prjIndex = selection;
			}
			else if (type == Type.PLATFORM) {
				pltIndex = selection;
			}
			else {
				toolIndex = selection;
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
		}
	}

}
