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

import eclipseplugin.SubmissionInfo;
import eclipseplugin.exceptions.*;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.api.Tool;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.uiuc.ncsa.swamp.api.Platform; 

public class SelectionDialog extends TitleAreaDialog {
	
	private org.eclipse.swt.widgets.List platformList;
	private org.eclipse.swt.widgets.List toolList;
	private List<Platform> platforms;
	private List<Tool> tools;
	private SwampApiWrapper api;
	private SubmissionInfo submissionInfo;
	private String packageType;
	private String prjUUID;

	private enum Type {
		PLATFORM, TOOL
	}
	
	public SelectionDialog(Shell parentShell, SubmissionInfo si, SwampApiWrapper api) {
		super(parentShell);
		submissionInfo = si;
		packageType = si.getPackageType();
		prjUUID = si.getSelectedProjectID();
		tools = api.getTools(packageType, prjUUID);
		platforms = getSupportedPlatforms(tools);
	}
	
	private List<Platform> getSupportedPlatforms(List<Tool> tools) {
		Set<Platform> supportedPlatformSet = new HashSet<Platform>();
		List<Platform> supportedList = null;
		for (Tool tool : tools) {
			supportedList = api.getSupportedPlatforms(tool.getUUIDString(), prjUUID);
			for (Platform p : supportedList) {
				supportedPlatformSet.add(p);
			}
		}
		supportedList = new ArrayList<Platform>(supportedPlatformSet.size());
		for (Platform p : supportedPlatformSet) {
			supportedList.add(p);
		}
		return supportedList;
	}
	
	private String[] getSelectionElements(Type type) {
		if (type == Type.PLATFORM) {
			getPlatformList();
		}
		return getToolList();
	}
	
	private String[] getToolList() {
		assert(tools!= null);
		int size = tools.size();
		String[] array = new String[size];
		for (int i = 0; i < size; i++) {
			array[i] = tools.get(i).getName();
		}
		return array;
	}
	
	private String[] getPlatformList() {
		assert(platforms != null);
		int size = platforms.size();
		String[] array = new String[size];
		for (int i = 0; i < size; i++) {
			array[i] = platforms.get(i).getName();
		}
		return array;
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
		// TODO Test stronger checks in SubmissionInfo to check platform, tool compatibility
		submissionInfo.setPlatforms(platformAry);
		submissionInfo.setTools(toolAry);
		
		if (!submissionInfo.validPlatformToolPairsExist()) {
			this.setMessage("There are no compatible platform-tool pairs for your selections.");
			return;
		}
		
		submissionInfo.setProject(comboSelection);
		super.okPressed();
	}
	
	private void resetWidgets() {
		//projCombo.deselectAll();
		platformList.deselectAll();
		toolList.deselectAll();
		setDefaults();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		// TODO Remove this
		submissionInfo.printPackageTypes();
		
		
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

		platformList = addList(container, "Platform: ", Type.PLATFORM, new GridData(GridData.FILL_BOTH));
		platformList.setEnabled(false);
		toolList = addList(container, "Tool: ", Type.TOOL, new GridData(GridData.FILL_BOTH));
		
		if (submissionInfo.isSelectionInitialized()) {
			List<String> platformUUIDs = submissionInfo.getSelectedPlatformIDs();
			setSelectedPlatforms(platformUUIDs);
			List<String> toolUUIDs = submissionInfo.getSelectedToolIDs();
			setSelectedTools(toolUUIDs);
		}
		else {
			setDefaults();
		}
			
		return area;
		
	}
	
	private void setSelectedPlatforms(List<String> platformIDs) {
		Map<String, Platform> platformMap = new HashMap<String, Platform>;
		for (int i = 0; i < platforms.size(); i++) 
			
		for (int i = 0; i < platforms.size(); i++) {
			if ();
		}
	}
	
	private void setSelectedTools(List<String> toolIDs) {
		Set<String> toolIDSet = new HashSet<String>();
		for (String toolID : toolIDs) {
			toolIDSet.add(toolID);
		}
		for (int i = 0; i < tools.size(); i++) {
			
		}
	 }
	
	private void setDefaults() {
		setPlatformDefault();
		setToolDefault();
	}
	
	private void setPlatformDefault() {
		if (platformList.getItemCount() == 1) {
			platformList.select(0);
		}
	}
	
	private void setToolDefault() {
		if (toolList.getItemCount() == 1) {
			toolList.select(0);
		}
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
	
	private void refreshTools() {
		String[] toolOptions = getSelectionElements(Type.TOOL);
		toolList.setItems(toolOptions);
		setToolDefault();
	}
	
	private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			resetWidgets();

		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
	
	private class ProjectSelectionListener implements SelectionListener {
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			int index = projCombo.getSelectionIndex();
			submissionInfo.setProject(index);
			refreshTools();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
		
	}
	
}
