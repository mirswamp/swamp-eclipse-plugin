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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.continuousassurance.swamp.cli.SwampApiWrapper;
import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.Utils;
import org.continuousassurance.swamp.session.util.Proxy;
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
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * This class creates a dialog for SWAMP authentication
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class AuthenticationDialog extends TitleAreaDialog {
	
	/**
	 * Combo for selecting SWAMP host
	 */
	private Combo hostnameCombo;
	/**
	 * Text widget for specifying a hostname other than those listed in the combo
	 */
	private Text otherHostnameText;
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
	/**
	 * Help message for host selection
	 */
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
	/**
	 * Abbreviation for Morgridge Institute for Research SWAMP instance
	 */
	private static final String MIR_SWAMP_DESCRIPTION = "MIR";
	/**
	 * Key for being able to specify other (unlisted) SWAMP hosts
	 */
	private static final String JSON_UNLISTED_HOSTS_KEY = "canSpecifyUnlistedHosts";
	/**
	 * Key for list of available hosts
	 */
	private static final String JSON_HOSTS_LIST_KEY = "hosts";
	/**
	 * Key for host name
	 */
	private static final String JSON_HOST_NAME_KEY = "name";
	/**
	 * Key for host description
	 */
	private static final String JSON_HOST_DESCRIPTION_KEY = "description";
	/**
	 * Label for "Other" option
	 */
	private static final String OTHER_OPTION = "Other";
	/**
	 * Message for invalid host specified
	 */
	private static final String NO_HOST_SPECIFIED = "Invalid hostname specified.";
	/**
	 * Name of JSON host configuration file
	 */
	private static final String JSON_CONFIG_FILENAME = "SWAMP_hosts.json";
	
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
	
	/**
	 * Gets list of available hostings based on the install's host
	 * configuration options. Fallbacks to default list if none are found.
	 * @return available hostnames
	 */
	private static String[] getAvailableHostnames() {
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
				JsonObject info = reader.readObject();
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
	
	/**
	 * Gets default hostnames
	 * @return default hostnames
	 */
	private static String[] getDefaultHostnames() {
		String[] array = {SwampApiWrapper.SWAMP_HOST_NAME + " (" + MIR_SWAMP_DESCRIPTION + ")"};
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
	
	/**
	 * Getter for SwampApiWrapper object
	 * @return SwampApiWrapper object
	 */
	public SwampApiWrapper getSwampApiWrapper() {
		return api;
	}
	
	protected Proxy getProxy(String swamp_host) {
	    Proxy proxy = new Proxy();

        IProxyService service = Activator.getDefault().getProxyService();
        if (service != null && service.isProxiesEnabled()) {
            
            for (String host : service.getNonProxiedHosts()) {
                if (host.equalsIgnoreCase(swamp_host)) {
                    return proxy;
                }
            }
            
            IProxyData iproxy_data = null;
            
            if (service.getProxyData(IProxyData.HTTPS_PROXY_TYPE).getHost() != null && 
                    service.getProxyData(IProxyData.HTTPS_PROXY_TYPE).getPort() != -1) {
                iproxy_data = service.getProxyData(IProxyData.HTTPS_PROXY_TYPE);
            }else if (service.getProxyData(IProxyData.HTTP_PROXY_TYPE).getHost() != null && 
                    service.getProxyData(IProxyData.HTTP_PROXY_TYPE).getPort() != -1){
                iproxy_data = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
            }
            
            if (iproxy_data != null) {
                proxy.setHost(iproxy_data.getHost());
                proxy.setPort(iproxy_data.getPort());
                
                if (iproxy_data.getUserId() != null && iproxy_data.getPassword() != null) {
                    proxy.setUsername(iproxy_data.getUserId());
                    proxy.setPassword(iproxy_data.getPassword());
                }
                proxy.setScheme(iproxy_data.getType().toLowerCase());
                proxy.setConfigured(true);
            }
        }
        
        return proxy;
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
			api = new SwampApiWrapper();
			System.out.println("Hostname: " + hostname);
			id = api.login(username, password, hostname, getProxy((new URL(hostname)).getHost()), null);
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
	
	/**
	 * Listener for host change combo widget
	 * @author reid-jr
	 *
	 */
	private class HostComboSelectionListener implements SelectionListener {
		/**
		 * Combo that this listener is listening for selection in
		 */
		private Combo combo;
		/**
		 * Constructor for HostComboSelectionListener
		 * @param c Combo widget
		 */
		public HostComboSelectionListener(Combo c) {
			combo = c;
		}
		
		@Override
		/**
		 * Handles selection in the Combo widget by enabling/disabling
		 * text widget for entering another host option
		 * @param e click event
		 */
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