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

package eclipseplugin.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import eclipseplugin.SubmissionInfo;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.uiuc.ncsa.swamp.api.Platform;

public class PlatformDialog extends TitleAreaDialog {
	private List<Platform> platforms;
	private org.eclipse.swt.widgets.List swtPlatformList;
	private SwampApiWrapper api;
	private SubmissionInfo submissionInfo;
	private Shell shell;
	
	private static final String PLATFORM_TITLE	= "Platform Selection";
	private static final String PLATFORM_HELP 	= "Select one or more platforms to run your assessment on.";
	
	public PlatformDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		shell = parentShell;
		submissionInfo = si;
		api = submissionInfo.getApi();
	}
	
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
		platforms = getPlatforms(submissionInfo.getSelectedToolIDs(), submissionInfo.getSelectedProjectID());
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
	
	private List<Platform> getPlatforms(List<String> toolUUIDs, String prjUUID) {
		Set<Platform> platformSet = new HashSet<Platform>();
		for (String toolUUID : toolUUIDs) {
			List<Platform> list = api.getSupportedPlatforms(toolUUID, prjUUID);
			for (Platform p : list) {
				platformSet.add(p);
			}
		}
		List<Platform> platformList = new ArrayList<Platform>(platformSet.size());
		for (Platform p : platformSet) {
			platformList.add(p);
		}
		return platformList;
	}
	
	private String[] convertPlatformListToStringArray() {
		int numPlatforms = platforms.size();
		String[] platformArray = new String[numPlatforms];
		for (int i = 0; i < numPlatforms; i++) {
			platformArray[i] = platforms.get(i).getName();
		}
		//Arrays.sort(platformArray);
		return platformArray;
	}
	
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
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		Button clear = createButton(parent, IDialogConstants.NO_ID, DialogUtil.CLEAR_CAPTION, false);
		clear.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.BACK_ID, DialogUtil.BACK_CAPTION, false);
		createButton(parent, IDialogConstants.OK_ID, DialogUtil.OK_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	
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
		
	protected void backPressed() {
		submissionInfo.setSelectedPlatformIDs(null);
		super.setReturnCode(IDialogConstants.BACK_ID);
		super.close();
	}

	@Override
	protected void okPressed() {
		if (swtPlatformList.getSelectionCount() < 1) {
			this.setMessage("Select at least one platform.");
		}
		int[] selectedIndices = swtPlatformList.getSelectionIndices();
		List<String> selectedPlatformIDs = new ArrayList<String>(selectedIndices.length);
		for (int i : selectedIndices) {
			Platform platform = platforms.get(i);
			selectedPlatformIDs.add(platform.getUUIDString());
			System.out.println(platform.getUUIDString());
			System.out.println(platform.getName());
			System.out.println(platform.getFilename());
		}
		submissionInfo.setSelectedPlatformIDs(selectedPlatformIDs);
		super.okPressed();
	}

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