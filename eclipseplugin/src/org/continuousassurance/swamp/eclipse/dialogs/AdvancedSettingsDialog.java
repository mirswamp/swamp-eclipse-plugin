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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for advanced settings including config options and build options
 * @author reid-jr
 *
 */
public class AdvancedSettingsDialog extends TitleAreaDialog {
	/**
	 * Text widget for entering build options
	 */
	private Text buildOptText;
	/**
	 * Text widget for entering configuration options
	 */
	private Text configOptText;
	/**
	 * Text widget for showing the file path of the configuration script
	 */
	private Text configScriptText;
	/**
	 * Button widget for selecting a file
	 */
	private Button selectFileButton;

	/**
	 * Title of the dialog
	 */
	private static final String ADVANCED_SETTINGS_TITLE = "Advanced Settings";
	/**
	 * Help info for build options
	 */
	private static final String BUILD_OPTIONS_HELP = "Add flags to be used when your package is built.";		
	/**
	 * Help info for configuration command
	 */
	private static final String CONFIG_COMMAND_HELP = "Write the command to be used prior to building your package";
	/**
	 * Help info for configuration options
	 */
	private static final String CONFIG_OPTIONS_HELP = "Add flags to be used when your package is configured.";
	/**
	 * Help information for selecting a configure script
	 */
	private static final String SELECT_FILE_HELP = "Select your configure script";
	/**
	 * Label for build options
	 */
	private static final String BUILD_OPTIONS_LABEL = "Build Options: ";
	/**
	 * Label for configuration script
	 */
	private static final String CONFIG_SCRIPT_LABEL = "Configuration Script: ";
	/**
	 * Label for browse
	 */
	private static final String BROWSE_LABEL = "...";
	/**
	 * Label for configuration options
	 */
	private static final String CONFIG_OPTIONS_LABEL = "Configuration Options: ";
	/**
	 * Shell widget that this dialog is placed on
	 */
	private final Shell shell;
	/**
	 * ConfigDialog object that launched this AdvancedSettings dialog
	 */
	private final ConfigDialog parentDialog;
	
	/**
	 * Constructor for AdvancedSettingsDialog
	 * @param parentShell shell
	 * @param cd parent ConfigDialog that launched this
	 */
	public AdvancedSettingsDialog(Shell parentShell, ConfigDialog cd) {
		super(parentShell);
		shell = new Shell(parentShell);
		parentDialog = cd;
	}
	
	/**
	 * Resets widgets in this dialog
	 */
	private void resetWidgets() {
		buildOptText.setText("");
		configOptText.setText("");
		configScriptText.setText("");
	}
	
	@Override
	/**
	 * This method creates all of the UI for the AdvancedSettingsDialog
	 * @param parent the parent Composite on which the UI will be placed
	 * @return Control with the AdvancedSettingsDialog UI on it
	 */
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		int horizontalSpan = 2;
		
		setTitle(ADVANCED_SETTINGS_TITLE);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(4, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget(BUILD_OPTIONS_LABEL, SWT.NONE, container, horizontalSpan);
		buildOptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		buildOptText.setText(parentDialog.getBuildOpts());
		buildOptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, BUILD_OPTIONS_HELP));
		
		DialogUtil.initializeLabelWidget(CONFIG_SCRIPT_LABEL, SWT.NONE, container, horizontalSpan);
		configScriptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		configScriptText.setText(parentDialog.getConfigScriptPath());
		configScriptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_COMMAND_HELP));
		
		selectFileButton = DialogUtil.initializeButtonWidget(container, BROWSE_LABEL, new GridData(SWT.FILL, SWT.NONE, false, false), 1);
		selectFileButton.addSelectionListener(new FileSelectionListener());
		selectFileButton.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, SELECT_FILE_HELP));

		
		DialogUtil.initializeLabelWidget(CONFIG_OPTIONS_LABEL, SWT.NONE, container, horizontalSpan);
		configOptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		configOptText.setText(parentDialog.getConfigOpts());
		configOptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_OPTIONS_HELP));

		return area;
	}
	
	@Override
	/**
	 * Sets settings in the parent ConfigDialog when ok is pressed. User is
	 * also returned to that dialog
	 */
	protected void okPressed() {
		parentDialog.setBuildOpts(buildOptText.getText());
		parentDialog.setConfigOpts(configOptText.getText());
		parentDialog.setConfigScriptPath(configScriptText.getText());
		super.okPressed();
	}
	
	@Override
	/**
	 * Adds buttons to the buttons bar
	 * @param parent Composite on which these buttons will be placed
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		
		Button button = createButton(parent, IDialogConstants.NO_ID, DialogUtil.CLEAR_CAPTION, false);
		button.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, DialogUtil.OK_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	
	/**
	 * Listener for clear button
	 * @author reid-jr
	 *
	 */
	private class ClearButtonSelectionListener implements SelectionListener {
		
		@Override
		/**
		 * Resets widgets when user clicks "Clear"
		 * @param e click event
		 */
		public void widgetSelected(SelectionEvent e) {
			resetWidgets();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
	
	/**
	 * Listener for file selection
	 * @author reid-jr
	 *
	 */
	private class FileSelectionListener implements SelectionListener {
		
		@Override
		/**
		 * This method causes a FileDialog to pop-up when the user selects
		 * browse button and then sets other widgets appropriately based on
		 * what the user selected in that FileDialog
		 * @param e click event on browse button
		 */
		public void widgetSelected(SelectionEvent e) {
			FileDialog dialog = new FileDialog(shell);
			String rc = dialog.open();
			if (rc != null) {
				configScriptText.setText(rc);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}
