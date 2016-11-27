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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.Utils;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import edu.wisc.cs.swamp.*;
import edu.wisc.cs.swamp.SwampApiWrapper.HostType;

/**
 * This class creates a dialog for SWAMP authentication
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class AuthenticationDialog extends TitleAreaDialog {
	
	private Combo hostnameCombo;
	
	private Text otherHostnameText;
	//private Text hostnameText;
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
	
	private static final String HOST_COMBO_HELP = "Select the SWAMP host that you want to connect to.";
		
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
	
	private static final String MIR_SWAMP_DESCRIPTION = "MIR";
	
	private static final String JSON_UNLISTED_HOSTS_KEY = "canSpecifyUnlistedHosts";
	
	private static final String JSON_HOSTS_LIST_KEY = "hosts";
	
	private static final String JSON_HOST_NAME_KEY = "name";
	
	private static final String JSON_HOST_DESCRIPTION_KEY = "description";
	
	private static final String OTHER_OPTION = "Other";
	
	private static final String NO_HOST_SPECIFIED = "Invalid hostname specified.";
	
	private static final String JSON_CONFIG_FILENAME = "SWAMP_hosts.json";
	
	private JsonObject info;
	
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
	public AuthenticationDialog(Shell parentShell, MessageConsoleStream stream) {
		super(parentShell);
		shell = parentShell;
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

		
		DialogUtil.initializeLabelWidget("Hostname: ", SWT.NONE, container);
		hostnameCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), getAvailableHostnames());
		hostnameCombo.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, HOST_COMBO_HELP));
		hostnameCombo.select(0);
		hostnameCombo.addSelectionListener(new HostComboSelectionListener(hostnameCombo));
		
		DialogUtil.initializeLabelWidget("Other hostname:", SWT.NONE, container);
		otherHostnameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false)); 
		otherHostnameText.setEnabled(false);
		
		DialogUtil.initializeLabelWidget("Username: ", SWT.NONE, container);
		usernameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		usernameText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, USERNAME_HELP));
		usernameText.setFocus();
		
		DialogUtil.initializeLabelWidget("Password: ", SWT.NONE, container);
		passwordText = DialogUtil.initializeTextWidget(SWT.PASSWORD | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		passwordText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PASSWORD_HELP));
		
		return area;
	}
	
	private String[] getAvailableHostnames() {
		Location configLoc = Platform.getInstallLocation();
		
		String filePath = configLoc.getURL().getPath() + JSON_CONFIG_FILENAME;
		File f = new File(filePath);
		java.util.List<String> hosts = new ArrayList<>();
		if (f.exists()) {
			InputStream is = null;
			JsonReader reader = null;
			try {
				is = new FileInputStream(f);
				reader = Json.createReader(is);
				info = reader.readObject();
				JsonArray hostArray = info.getJsonArray(JSON_HOSTS_LIST_KEY);
				for (int i = 0; i < hostArray.size(); i++) {
					JsonObject o = hostArray.getJsonObject(i);
					String host = o.getString(JSON_HOST_NAME_KEY);
					String description = o.getString(JSON_HOST_DESCRIPTION_KEY);
					hosts.add(host + " (" + description + ")");
				}
				if (info.getBoolean(JSON_UNLISTED_HOSTS_KEY)) {
					hosts.add(OTHER_OPTION);
				}
				return hosts.toArray(new String[0]);
			}
			catch (Exception e) {
				e.printStackTrace();
				return getDefaultHostnames();
			}
			finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (reader != null) {
					reader.close();
				}
			}

		}
		else {
			return getDefaultHostnames();
		}
	}
	
	private String[] getDefaultHostnames() {
		String[] array = {SwampApiWrapper.SWAMP_HOST_NAMES_MAP.get(HostType.PRODUCTION) + " (" + MIR_SWAMP_DESCRIPTION + ")"};
		return array;
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
	
	public SwampApiWrapper getSwampApiWrapper() {
		return api;
	}
	
	/**
	 * Tries authenticating with the SWAMP
	 */
	@Override
	protected void okPressed() {
		String username = usernameText.getText();
		String password = passwordText.getText();
		//String hostname = hostnameText.getText();
		int index = hostnameCombo.getSelectionIndex();
		if (index < 0) {
			out.println(Utils.getBracketedTimestamp() + "Error: Invalid hostname specified.");
			this.setMessage(NO_HOST_SPECIFIED);
			usernameText.setText("");
			usernameText.setFocus();
			passwordText.setText("");
			return;
		}
		String hostname = hostnameCombo.getItem(index);
		if (hostname.equals(OTHER_OPTION)) {
			hostname = otherHostnameText.getText();
		}
		else {
			hostname = hostname.split(" ")[0];
		}
		String id;
		System.out.println("Hostname: " + hostname);
		
		if ((hostname.length() == 0)) {
			out.println(Utils.getBracketedTimestamp() + "Error: No host specified.");
			return;
		}
		
		if ((username.length() == 0) || (password.length() == 0)) {
			setInvalidMsgAndClearPrompts();
			return;
		}
	
		try {
			api = new SwampApiWrapper(HostType.CUSTOM, hostname);
			id = api.login(username, password, HostType.CUSTOM, hostname);
			api.saveSession();
		}
		catch (Exception h) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		if (id == null) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		Activator.setLoggedIn(true);
		Activator.setHostname(hostname);
		super.okPressed();
	}
	
	private class HostComboSelectionListener implements SelectionListener {
		Combo combo;
		public HostComboSelectionListener(Combo c) {
			combo = c;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			if (combo.getItem(selection).equals(OTHER_OPTION)) {
				otherHostnameText.setEnabled(true);
			}
			else {
				otherHostnameText.setEnabled(false);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
	
}