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

package org.continuousassurance.swamp.eclipse.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.continuousassurance.swamp.eclipse.SubmissionInfo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import edu.uiuc.ncsa.swamp.api.Tool;
import edu.wisc.cs.swamp.SwampApiWrapper;

/**
 * This class creates a dialog for SWAMP Tool selection
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class ToolDialog extends TitleAreaDialog {
	/**
	 * List of SWAMP Tools
	 */
	private List<Tool> tools;
	/**
	 * Widget for selecting tools
	 */
	private org.eclipse.swt.widgets.List swtToolList;
	/**
	 * Reference to SwampApiWrapper for communicating with SWAMP
	 */
	private SwampApiWrapper api;
	/**
	 * Back-end SubmissionInfo object that stores state
	 */
	private SubmissionInfo submissionInfo;
	/**
	 * Shell object
	 */
	private Shell shell;
	/**
	 * Title of the dialog
	 */
	private static final String TOOL_TITLE 	= "Tool Selection";
	/**
	 * Help text for the tool List
	 */
	private static final String TOOL_HELP 	= "Select one or more tools to run your assessment on.";

	/**
	 * Constructor for ToolDialog
	 *
	 * @param shell the shell that this dialog will be in
	 * @param si the SubmissionInfo object backing this display 
	 */
	public ToolDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		shell = parentShell;
		submissionInfo = si;
		api = submissionInfo.getApi();
	}
	
	/**
	 * Creates dialog area of window and places widgets on it
	 *
	 * @param parent the parent Composite that the widgets will be placed on 
	 * top of
	 * @return Control with widgets on it
	 */
	@Override protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		this.setTitle(TOOL_TITLE);
		
		/* Note: From GridData JavaDoc, "Do not reuse GridData objects. Every control in a composite
		 * that is managed by a GridLayout must have a unique GridData object.
		 */
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));

		DialogUtil.initializeLabelWidget("Tools: ", SWT.NONE, container);
		tools = api.getTools(submissionInfo.getPackageType(), submissionInfo.getSelectedProjectID());
		swtToolList = DialogUtil.initializeListWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), convertToolListToStringArray());
		swtToolList.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, TOOL_HELP));
		
		if (submissionInfo.toolsInitialized()) {
			System.out.println("Tools initialized");
			List<String> toolUUIDs = submissionInfo.getSelectedToolIDs();
			setSelectedTools(toolUUIDs);
		}
		else {
			// select all tools by default
			swtToolList.selectAll();
		}
			
		return area;
	}
	
	/**
	 * Converts the list of SWAMP Tools to an array of their names
	 *
	 * @return array of String names of the tools
	 */
	private String[] convertToolListToStringArray() {
		int numTools = tools.size();
		String[] toolArray = new String[numTools];
		for (int i = 0; i < numTools; i++) {
			toolArray[i] = tools.get(i).getName();
		}
		return toolArray;
	}
	
	/**
	 * Sets selections in the tool List (widget) based on a list of selected
	 * SWAMP Tool identifiers
	 *
	 * @param toolUUIDs a List (java.util.List) with unique universal IDs of
	 * SWAMP Tools that had been previously selected
	 */
	private void setSelectedTools(List<String> toolUUIDs) {
		int count = 0;
		int numIDs = toolUUIDs.size();
		for (int i = 0; (i < tools.size()) && (count < numIDs); i++) {
			String id = tools.get(i).getUUIDString();
			if (toolUUIDs.contains(id)) {
				swtToolList.select(i);
				count++;
			}
		}
	}
	
	/**
	 * Creates buttons for the window's button bar
	 *
	 * @param parent the parent Composite that the buttons will be placed on 
	 * top of
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		Button clear = createButton(parent, IDialogConstants.NO_ID, DialogUtil.CLEAR_CAPTION, false);
		clear.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.BACK_ID, DialogUtil.BACK_CAPTION, false);
		createButton(parent, IDialogConstants.OK_ID, DialogUtil.OK_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	
	/**
	 * Method for handling button presses on this dialog
	 *
	 * @param buttonID the ID of the button that was selected
	 */
	@Override
	public void buttonPressed(int buttonID) {
		switch (buttonID) {
		case IDialogConstants.OK_ID:
			okPressed();
			break;
		case IDialogConstants.BACK_ID:
			backPressed();
			break;
		case IDialogConstants.CANCEL_ID:
			super.cancelPressed();
		}
	}
		
	/**
	 * Method for handling back button being pressed 
	 */
	private void backPressed() {
		//submissionInfo.setSelectedToolIDs(null);
		submissionInfo.setSelectedToolIDs(getSelectedIDs());
		super.setReturnCode(IDialogConstants.BACK_ID);
		super.close();
	}
	
	/**
	 * Method for handling OK button being pressed. Tests that at least one
	 * tool was selected and then advances
	 */
	@Override
	protected void okPressed() {
		if (swtToolList.getSelectionCount() < 1) {
			this.setMessage("Select at least one tool.");
		}
		submissionInfo.setSelectedToolIDs(getSelectedIDs());
		super.okPressed();
	}
	
	/**
	 * Helper method for converting the widget's selected elements to a list
	 * of their UUIDs
	 */
	private List<String> getSelectedIDs() {
		int[] selectedIndices = swtToolList.getSelectionIndices();
		List<String> selectedToolIDs = new ArrayList<String>(selectedIndices.length);
		for (int i : selectedIndices) {
			Tool tool = tools.get(i);
			selectedToolIDs.add(tool.getUUIDString());
		}
		return selectedToolIDs;
	}

/**
* This class clears the List (widget) of Tools when the button this listener is 
* added to is selected
* @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
* @since 07/2016 
*/
private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			swtToolList.deselectAll();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}
