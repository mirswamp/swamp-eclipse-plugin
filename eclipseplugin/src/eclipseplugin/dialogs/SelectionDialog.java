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
import java.util.List;

import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.api.Tool;
import edu.uiuc.ncsa.swamp.api.Platform; 

public class SelectionDialog extends TitleAreaDialog {
	
	private Combo projCombo;
	private org.eclipse.swt.widgets.List platformList;
	private org.eclipse.swt.widgets.List toolList;
	private SubmissionInfo submissionInfo;

	private enum Type {
		PROJECT, PLATFORM, TOOL
	}
	
	public SelectionDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		submissionInfo = si;
	}
	
	private String[] getSelectionElements(Type type) {
		if (type == Type.PROJECT) {
			 return submissionInfo.getProjectList();
		}
		if (type == Type.PLATFORM) {
			 return submissionInfo.getPlatformList();
		}
		return submissionInfo.getToolList();
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
		projCombo.deselectAll();
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

		projCombo = addCombo(container, "Project: ", Type.PROJECT, new GridData(SWT.FILL, SWT.NONE, true, false));
		projCombo.addSelectionListener(new ProjectSelectionListener());
		platformList = addList(container, "Platform: ", Type.PLATFORM, new GridData(GridData.FILL_BOTH));
		toolList = addList(container, "Tool: ", Type.TOOL, new GridData(GridData.FILL_BOTH));
		
		if (submissionInfo.isSelectionInitialized()) {
			int prjIndex = submissionInfo.getProjectIndex();
			if (prjIndex > -1) {
				projCombo.select(prjIndex);
			}
			int[] platformIndices = submissionInfo.getPlatformIndices();
			if (platformIndices != null) {
				platformList.select(platformIndices);
			}
			int[] toolIndices = submissionInfo.getToolIndices();
			if (toolIndices != null) {
				toolList.select(toolIndices);
			}
		}
		else {
			setDefaults();
		}
			
		return area;
		
	}
	
	private void setDefaults() {
		setProjectDefault();
		setPlatformDefault();
		setToolDefault();
	}
	
	private void setProjectDefault() {
		if (projCombo.getItemCount() == 1) {
			projCombo.select(0);
		}
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
