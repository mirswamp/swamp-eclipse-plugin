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

package org.continuousassurance.swamp.eclipse.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.uiuc.ncsa.swamp.api.Platform;

/**
 * This class creates a dialog for SWAMP Platform selection
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class PlatformDialog extends TitleAreaDialog {
	/**
	 * The List of SWAMP Platforms
	 */
	private List<Platform> platforms;
	/**
	 * The List widget for selecting Platforms
	 */
	private org.eclipse.swt.widgets.List swtPlatformList;
	/**
	 * Reference to SwampApiWrapper for communicating with the SWAMP
	 */
	private SwampApiWrapper api;
	/**
	 * Reference to SubmissionInfo object for storing the selections
	 */
	private SubmissionInfo submissionInfo;
	/**
	 * Reference to Shell object
	 */
	private Shell shell;
	
	/**
	 * The title for the dialog
	 */
	private static final String PLATFORM_TITLE	= "Platform Selection";
	/**
	 * The help text for the List widget
	 */
	private static final String PLATFORM_HELP 	= "Select one or more platforms to run your assessment on.";
	
	/**
	 * Constructor for PlatformDialog
	 *
	 * @param shell the shell that this dialog will be in
	 * @param si the SubmissionInfo object that stores the selections on the
	 * back-end
	 */
	public PlatformDialog(Shell parentShell, SubmissionInfo si) {
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
		System.out.println("We're redrawing the platform dialog");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle(PLATFORM_TITLE);
		
		/* Note: From GridData JavaDoc, "Do not reuse GridData objects. Every control in a composite
		 * that is managed by a GridLayout must have a unique GridData object."
		 */
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));

		DialogUtil.initializeLabelWidget("Platforms: ", SWT.NONE, container);
		platforms = getPlatforms(submissionInfo.getPackageType());
		swtPlatformList = DialogUtil.initializeListWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), convertPlatformListToStringArray());
		swtPlatformList.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PLATFORM_HELP));
		
		if (submissionInfo.platformsInitialized()) {
			System.out.println("Platforms Initialized");
			List<String> platformUUIDs = submissionInfo.getSelectedPlatformIDs();
			setSelectedPlatforms(platformUUIDs);
		}
		else {
			// select all platforms by default
			swtPlatformList.selectAll();
		}
			
		return area;
	}
	
	/**
	 * Gets java.util.List of Platforms from the SWAMP given the selected Tools
	 * and SWAMP project
	 *
	 * @param pkgType the package type of the package being assessed
	 * @return List of SWAMP Platforms
	 */
	private List<Platform> getPlatforms(String pkgType) {
		
		Platform p = api.getDefaultPlatform(pkgType);
		List<Platform> platformList = new ArrayList<Platform>();
		platformList.add(p);
		return platformList;
	}
	
	/**
	 * Converts the list of platforms into an array of platform names
	 *
	 * @return String array of SWAMP Platform names
	 */
	private String[] convertPlatformListToStringArray() {
		int numPlatforms = platforms.size();
		String[] platformArray = new String[numPlatforms];
		for (int i = 0; i < numPlatforms; i++) {
			platformArray[i] = platforms.get(i).getName();
		}
		return platformArray;
	}
	
	/**
	 * This selects items in the dialog's List widget based on the IDs of
	 * platforms that should be selected by default
	 *
	 * @param platformUUIDs list of the UUIDs of the selected SWAMP Platforms
	 */
	private void setSelectedPlatforms(List<String> platformUUIDs) {
		int count = 0;
		int numIDs = platformUUIDs.size();
		for (int i = 0; (i < platforms.size()) && (count < numIDs); i++) {
			String id = platforms.get(i).getUUIDString();
			if (platformUUIDs.contains(id)) {
				swtPlatformList.select(i);
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
	protected void backPressed() {
		//submissionInfo.setSelectedPlatformIDs(null);
		submissionInfo.setSelectedPlatformIDs(getSelectedIDs());
		super.setReturnCode(IDialogConstants.BACK_ID);
		super.close();
	}

	/**
	 * Method for handling OK button being pressed. Tests that at least one
	 * tool was selected and then advances
	 */
	@Override
	protected void okPressed() {
		if (swtPlatformList.getSelectionCount() < 1) {
			this.setMessage("Select at least one platform.");
		}
		submissionInfo.setSelectedPlatformIDs(getSelectedIDs());
		super.okPressed();
	}
	
	/**
	 * Helper method for converting the widget's selected elements to a list
	 * of their UUIDs
	 */
	private List<String> getSelectedIDs() {
		int[] selectedIndices = swtPlatformList.getSelectionIndices();
		List<String> selectedPlatformIDs = new ArrayList<String>(selectedIndices.length);
		for (int i : selectedIndices) {
			Platform platform = platforms.get(i);
			selectedPlatformIDs.add(platform.getUUIDString());
		}
		return selectedPlatformIDs;
	}

/**
* This class clears the List (widget) of Platforms when the button this 
* listener is added to is selected
* @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
* @since 07/2016 
*/
private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			swtPlatformList.deselectAll();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}