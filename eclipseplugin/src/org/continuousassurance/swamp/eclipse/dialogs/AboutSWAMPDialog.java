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

import java.net.URL;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Implements About SWAMP
 * @author reid-jr
 *
 */
public class AboutSWAMPDialog extends TitleAreaDialog {
	
	/**
	 * Title of the dialog
	 */
	private static final String ABOUT_SWAMP_TITLE = "About SWAMP";
	/**
	 * Name of the plug-in
	 */
	private static final String ABOUT_SWAMP_NAME = "SWAMP Eclipse Plugin\n";

	/**
	 * Text with description and link to information about SWAMP License
	 */
	private static final String ABOUT_SWAMP_LICENSE_LINK = "Please visit <a href=\"https://www.apache.org/licenses/LICENSE-2.0\">Apache License</a> for licensing information.\n";
	/**
	 * Text with description and link to information about SWAMP support
	 */
	private static final String ABOUT_SWAMP_SUPPORT_INFO = "Please visit <a href=\"https://continuousassurance.org/support/\"> Continuous Assurance Support</a> for technical support.\n";
	/**
	 * Text with link to source code
	 */
	private static final String ABOUT_SWAMP_GITHUB_LINK = "Source code is available at the SWAMP Eclipse Plug-in <a href=\"https://github.com/mirswamp/swamp-eclipse-plugin\">GitHub</a> repository.\n"; 
	/**
	 * Plug-in release version number
	 */
	private static final String ABOUT_SWAMP_SEMANTIC_VERSION_NUMBER = "1.0.0";
	/**
	 * Plug-in release date
	 */
	private static final String ABOUT_SWAMP_RELEASE_DATE = "03/30/2017";
	/**
	 * Release date label
	 */
	private static final String RELEASE_DATE_LABEL = "Release Date: ";
	/**
	 * Release version label
	 */
	private static final String RELEASE_VERSION_LABEL = "Release Version: ";
	
	
	/**
	 * Constructor for AboutSWAMPDialog
	 * @param parentShell shell
	 */
	public AboutSWAMPDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	/**
	 * Creates the UI of the AboutSWAMPDialog
	 * @param parent the composite on which this UI is placed
	 * @return Control with AboutSWAMPDialog UI on it
	 */
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);	
		Composite container = new Composite(area, SWT.NONE);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		
		this.setTitle(ABOUT_SWAMP_TITLE);
		
		String relDate = getFormattedReleaseDate();
		String releaseVersion = getReleaseVersion();
		DialogUtil.initializeLabelWidget(ABOUT_SWAMP_NAME + "\n" + RELEASE_DATE_LABEL + relDate + "\n\n" + RELEASE_VERSION_LABEL + releaseVersion + "\n", SWT.NONE, container);
		
		addLink(container, ABOUT_SWAMP_LICENSE_LINK);
		addLink(container, ABOUT_SWAMP_SUPPORT_INFO);
		addLink(container, ABOUT_SWAMP_GITHUB_LINK);
		
		return area;
	}
	
	private static void addLink(Composite container, String text) {
		Link link = new Link(container, SWT.NONE);
		link.setText(text);
		link.addSelectionListener(new LinkSelectionAdapter());
	}
	
	/**
	 * Gets the release date for this version of the plug-in
	 * @return release date
	 */
	private String getFormattedReleaseDate() {
		return ABOUT_SWAMP_RELEASE_DATE; 
	}
	
	/**
	 * Gets the release version for the plug-in
	 * @return release version
	 */
	private String getReleaseVersion() {
		//return org.eclipse.core.runtime.Platform.getBundle("swamp_eclipse_plugin").getHeaders().get("Bundle-Version");
		return ABOUT_SWAMP_SEMANTIC_VERSION_NUMBER;
	}
	
	/**
	 * Listener for link being selected (e.g. clicked) in dialog. Launches an
	 * external browser instance
	 * @author reid-jr
	 *
	 */
	public static class LinkSelectionAdapter extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			try {
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
