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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import edu.uiuc.ncsa.swamp.session.HTTPException;
import edu.wisc.cs.swamp.*;

public class AuthenticationDialog extends TitleAreaDialog {
	private Text usernameText;
	private Text passwordText;
	private static final String AUTHENTICATION_TITLE = "SWAMP Authentication";
	private static final String AUTHENTICATION_PROMPT = "Please enter your authentication information for the SWAMP.";
	private static final String INVALID_MESSAGE = "Invalid username or password.";
	private static final String USERNAME_HELP = "Enter your SWAMP username.";
	private static final String PASSWORD_HELP = "Enter your SWAMP password.";
	private static final String LOGIN_CAPTION = "Login";
	private SwampApiWrapper api;
	private String id;
	private MessageConsoleStream out;
	private Shell shell;
	
	public AuthenticationDialog(Shell parentShell, SwampApiWrapper swampApi, MessageConsoleStream stream) {
		super(parentShell);
		shell = parentShell;
		api = swampApi;
		out = stream;
	}
	
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

		DialogUtil.initializeLabelWidget("Username: ", SWT.NONE, container);
		usernameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, griddata);
		usernameText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, USERNAME_HELP));
		
		DialogUtil.initializeLabelWidget("Password: ", SWT.NONE, container);
		passwordText = DialogUtil.initializeTextWidget(SWT.PASSWORD | SWT.BORDER, container, griddata);
		passwordText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PASSWORD_HELP));
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, LOGIN_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	
	void setInvalidMsgAndClearPrompts() {
		out.println("Error: Invalid username and/or password entered.");
		this.setMessage(INVALID_MESSAGE + "\n" + AUTHENTICATION_PROMPT);
		usernameText.setText("");
		usernameText.setFocus();
		passwordText.setText("");
	}
	
	@Override
	protected void okPressed() {
		String username = usernameText.getText();
		String password = passwordText.getText();
		
		if ((username.length() == 0) || (password.length() == 0)) {
			setInvalidMsgAndClearPrompts();
			return;
		}
	
		try {
			id = api.login(username, password);
		}
		catch (HTTPException h) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		if (id == null) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		super.okPressed();
	}
	
}