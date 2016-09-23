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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.MessageConsoleStream;

import eclipseplugin.Activator;
import eclipseplugin.Utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import edu.uiuc.ncsa.swamp.session.HTTPException;
import edu.wisc.cs.swamp.*;
import edu.wisc.cs.swamp.SwampApiWrapper.HostType;

/**
 * This class creates a dialog for SWAMP authentication
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class AuthenticationDialog extends TitleAreaDialog {
	
	private Text hostnameText;
	/**
	 * The Text widget for username
	 */
	private Text usernameText;
	/**
	 * The Text widget for password
	 */
	private Text passwordText;
	/**
	 * The title for this dialog
	 */
	private static final String AUTHENTICATION_TITLE = "SWAMP Authentication";
	/**
	 * The prompting message for this dialog
	 */
	private static final String AUTHENTICATION_PROMPT = "Please enter your authentication information for the SWAMP.";
	/**
	 * Message for invalid username or password
	 */
	private static final String INVALID_MESSAGE = "Invalid username or password.";
	
	private static final String HOSTNAME_HELP = "Enter the SWAMP host that you want to connect to.";
	
	/**
	 * Help message for username Text
	 */
	private static final String USERNAME_HELP = "Enter your SWAMP username.";
	/**
	 * Help message for password Text
	 */
	private static final String PASSWORD_HELP = "Enter your SWAMP password.";
	/**
	 * Caption for login button
	 */
	private static final String LOGIN_CAPTION = "Login";
	
	private static final String DEFAULT_HOST = "https://www.mir-swamp.org";
	
	/**
	 * Reference to SwampApiWrapper object. This facilitates interaction with
	 * the SWAMP
	 */
	private SwampApiWrapper api;
	/**
	 * Stream for writing to end user's console
	 */
	private MessageConsoleStream out;
	/**
	 * Shell object
	 */
	private Shell shell;
	
	/**
	 * Constructor for AuthenticationDialog
	 *
	 * @param shell the shell that this dialog will be in
	 * @param swampApi SwampApiWrapper object for communicating with the SWAMP
	 * @param stream stream for end user's console
	 */
	public AuthenticationDialog(Shell parentShell, SwampApiWrapper swampApi, MessageConsoleStream stream) {
		super(parentShell);
		shell = parentShell;
		api = swampApi;
		out = stream;
	}

	/**
	 * Creates dialog area of window and places widgets on it
	 *
	 * @param parent the parent Composite that the widgets will be placed on 
	 * top of
	 * @return Control with widgets on it
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle(AUTHENTICATION_TITLE);
		this.setMessage(AUTHENTICATION_PROMPT);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		GridData griddata = new GridData();
		griddata.grabExcessHorizontalSpace = true;
		griddata.horizontalAlignment = GridData.FILL;
		
		DialogUtil.initializeLabelWidget("Hostname: ", SWT.NONE, container);
		hostnameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, griddata);
		hostnameText.setText(DEFAULT_HOST);
		hostnameText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, HOSTNAME_HELP));

		DialogUtil.initializeLabelWidget("Username: ", SWT.NONE, container);
		usernameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, griddata);
		usernameText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, USERNAME_HELP));
		
		DialogUtil.initializeLabelWidget("Password: ", SWT.NONE, container);
		passwordText = DialogUtil.initializeTextWidget(SWT.PASSWORD | SWT.BORDER, container, griddata);
		passwordText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PASSWORD_HELP));
		
		return area;
	}
	
	/**
	 * Creates buttons for the window's button bar
	 *
	 * @param parent the parent Composite that the buttons will be placed on 
	 * top of
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, LOGIN_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	
	/**
	 * Sets invalid message for the dialog and clears username and password
	 * prompts
	 */
	private void setInvalidMsgAndClearPrompts() {
		out.println(Utils.getBracketedTimestamp() + "Error: Invalid username and/or password entered.");
		this.setMessage(INVALID_MESSAGE + "\n" + AUTHENTICATION_PROMPT);
		usernameText.setText("");
		usernameText.setFocus();
		passwordText.setText("");
	}
	
	/**
	 * Tries authenticating with the SWAMP
	 */
	@Override
	protected void okPressed() {
		String username = usernameText.getText();
		String password = passwordText.getText();
		String hostname = hostnameText.getText();
		HostType ht;
		String id;
		
		if ((hostname.length() == 0)) {
			out.println(Utils.getBracketedTimestamp() + "Error: No host specified.");
			return;
		}
		
		if (hostname.equals(DEFAULT_HOST)) {
			ht = HostType.PRODUCTION;
		}
		else {
			ht = HostType.CUSTOM;
		}
		
		if ((username.length() == 0) || (password.length() == 0)) {
			setInvalidMsgAndClearPrompts();
			return;
		}
	
		try {
			id = api.login(username, password, ht, hostname);
		}
		catch (HTTPException h) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		if (id == null) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		Activator.setLoggedIn(true);
		super.okPressed();
	}
	
}