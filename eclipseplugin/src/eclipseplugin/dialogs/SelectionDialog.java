/* Malcolm Reid Jr.
 * 06/10/2016
 * UW SWAMP
 * SelectionDialog.java
 * Code to implement a dialog box for getting package, tool, and platform from end user
 */

package eclipseplugin.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.*;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import java.util.ArrayList;
import java.util.List;

import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.api.Tool;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.uiuc.ncsa.swamp.api.Platform; 

public class SelectionDialog extends TitleAreaDialog {
	
	private Combo projCombo;
	private org.eclipse.swt.widgets.List platformList;
	private org.eclipse.swt.widgets.List toolList;
	private List<? extends Project> projects;
	private List<? extends Tool> tools;
	private List<? extends Platform> platforms;
	private int prjIndex;
	private int platformIndices[];
	private int toolIndices[];
	private SwampApiWrapper api;

	private enum Type {
		PROJECT, PLATFORM, TOOL
	}
	
	public SelectionDialog(Shell parentShell) {
		super(parentShell);
		resetState();
	}
	
	public void setSwampApiWrapper(SwampApiWrapper w) {
		api = w;
	}

	private String[] getSelectionElements(Type type) {
		ArrayList<String> stringList = new ArrayList<String>();
		if (type == Type.PROJECT) {
			projects = api.getAllProjects();
			for (Project p : projects) {
				stringList.add(p.getFullName());
			}
		}
		else if (type == Type.PLATFORM) {
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
		return getElementList(stringList);
	}
	
	private String[] getElementList(ArrayList<String> list) {
		String[] ary = null;
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
	
	public int getProjectIndex() {
		return prjIndex;
	}
	
	public void setProjectIndex(int index) {
		prjIndex = index;
	}
	
	public int[] getPlatformIndices() {
		return platformIndices;
	}
	
	public void setPlatformIndices(int[] array) {
		platformIndices = array;
	}
	
	public int[] getToolIndices() {
		return toolIndices;
	}
	
	public void setToolIndices(int[] array) {
		toolIndices = array;
	}
	
	public String getProjectUUID() {
		if (prjIndex < 0) { 
			return null;
		}
		return projects.get(prjIndex).getUUIDString();
	}
	
	public List<String> getPlatformUUIDs() {
		List<String> uuidList = new ArrayList<String>();
		for (int i : platformIndices) {
			uuidList.add(platforms.get(i).getUUIDString());
		}
		return uuidList;
	}
	
	public List<String> getToolUUIDs() {
		List<String> uuidList = new ArrayList<String>();
		for (int i : toolIndices) {
			uuidList.add(tools.get(i).getUUIDString());
		}
		return uuidList;
	}
	
	@Override
	protected void okPressed() {
		// Here we do some checks to make sure that everything has actually been populated
		// We also set things appropriately
		int comboSelection = projCombo.getSelectionIndex();
		if (comboSelection < 0) {
			this.setMessage("Invalid project selected.");
			return;
		}
		int platformAry[] = platformList.getSelectionIndices();
		if (platformAry == null || platformAry.length < 1) {
			this.setMessage("No platform selected.");
			return;
		}
		int toolAry[] = toolList.getSelectionIndices();
		if (toolAry == null || toolAry.length < 1) {
			this.setMessage("No tool selected.");
			return;
		}
		prjIndex = comboSelection;
		platformIndices = platformAry;
		toolIndices = toolAry;
		super.okPressed();
	}
	
	public void resetState() {
		prjIndex = -1;
		platformIndices = null;
		toolIndices = null;
	}
	
	private void resetWidgets() {
		projCombo.deselectAll();
		platformList.deselectAll();
		toolList.deselectAll();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Project Configuration");
		
		/* Note: From GridData JavaDoc, "Do not reuse GridData objects. Every control in a composite
		 * that is managed by a GridLayout must have a unique GridData object.
		 */
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));

		GridData comboGridData = new GridData();
		comboGridData.grabExcessHorizontalSpace = true;
		comboGridData.horizontalAlignment = GridData.FILL;

		projCombo = addCombo(container, "Project: ", Type.PROJECT, new GridData(SWT.FILL, SWT.NONE, true, false));
		platformList = addList(container, "Platform: ", Type.PLATFORM, new GridData(GridData.FILL_BOTH));
		toolList = addList(container, "Tool: ", Type.TOOL, new GridData(GridData.FILL_BOTH));
		
		if (prjIndex > -1) {
			projCombo.select(prjIndex);
		}
		if (platformIndices != null) {
			for (int i : platformIndices) {
				platformList.select(i);
			}
		}
		if (toolIndices != null) {
			for (int i : toolIndices) {
				toolList.select(i);
			}
		}

		return area;
		
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		
		Button button = createButton(parent, IDialogConstants.NO_ID, "Clear All", false);
		button.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	private org.eclipse.swt.widgets.List addList(Composite container, String labelText, Type type, GridData listGriddata) {
		DialogUtil.initializeLabelWidget(labelText, SWT.NONE, container);
		String[] listOptions = getSelectionElements(type);
		org.eclipse.swt.widgets.List list = DialogUtil.initializeListWidget(container, listGriddata, listOptions);
		return list;
	}
	
	private Combo addCombo(Composite container, String labelText, Type type, GridData comboGridData) {
		DialogUtil.initializeLabelWidget(labelText, SWT.NONE, container);
		String[] comboOptions = getSelectionElements(type);
		Combo c = DialogUtil.initializeComboWidget(container, comboGridData, comboOptions);
		return c;
	}
	
	private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			resetState();
			resetWidgets();

		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
	
}
