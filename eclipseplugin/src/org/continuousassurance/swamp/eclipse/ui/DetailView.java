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
package org.continuousassurance.swamp.eclipse.ui;

import java.util.List;

import org.continuousassurance.swamp.eclipse.BugDetail;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import dataStructures.BugInstance;

/**
 * This class shows detailed information about a bug selected in the TableView.
 * We use an embedded browser widget to render HTML for more control of the
 * styling of this view's content.
 * @author reid-jr
 */
public class DetailView extends ViewPart {
	/**
	 * Label for bug message
	 */
	private static final String MESSAGE_LABEL = "Message: ";
	/**
	 * Label for bug resolution suggestion
	 */
	private static final String RESOLUTION_SUGGESTION_LABEL = "Resolution: ";
	/**
	 * Label for line number or line number range
	 */
	private static final String LINE_NUMBER_LABEL = "Line number: ";
	/**
	 * Label for source file name
	 */
	private static final String FILENAME_LABEL = "File name: ";
	
	/**
	 * Label for weakness group
	 */
	private static final String GROUP_LABEL = "Weakness group: ";
	
	/**
	 * Label for bug type
	 */
	private static final String CODE_LABEL = "Weakness code: ";
	/**
	 * Label for tool name
	 */
	private static final String TOOL_LABEL = "Tool: ";
	/**
	 * Label for platform name
	 */
	private static final String PLATFORM_LABEL = "Platform: ";
	
	/**
	 * Label for flow
	 */
	private static final String FLOW_LABEL = "Flow: ";
	
	/**
	 * Composite that this is built on
	 */
	private Composite composite;
	/**
	 * Embedded browser to render the HTML
	 */
	private Browser browser;
	
	/**
	 * ID for this view (matches id in plugin.xml)
	 */
	public static final String ID = 
			"org.continuousassurance.swamp.eclipse.ui.views.detailview";
	
	/**
	 * User-viewable name for this view
	 */
	private static final String VIEW_NAME = "Weakness Details";
	
	/**
	 * Default message for this view. This is displayed if no weakness has been
	 * selected
	 */
	private static final String VIEW_DEFAULT_MESSAGE = 
			"Select a weakness from the list of weaknesses";
	
	@Override
	/**
	 * Creates view part
	 * @param parent composite on which this is placed and positioned
	 */
	public void createPartControl(Composite parent) {
		composite = parent;
		browser = new Browser(composite, SWT.NONE);
		setDefaultHtml();
	}

	@Override
	/**
	 * Gives the composite focus
	 */
	public void setFocus() {
		composite.setFocus();
	}
	
	/**
	 * Sets browser HTML for when no bug is selected
	 */
	private void setDefaultHtml() {
		String html = getHeaderAndTitle();
		html += getDefaultBody();
		System.out.println(html);
		browser.setText(html, true);
	}
	
	/**
	 * Sets browser HTML for a selected bug
	 * @param bugInfo BugDetail object for the selected bug
	 */
	private void setHtml(BugDetail bugInfo) {
		String html = getHeaderAndTitle();
		html += getBody(bugInfo);
		System.out.println(html);
		
		browser.setText(html, true);
	}
	
	/**
	 * Returns HTML header
	 * @return String for start of HTML, title, and header
	 */
	private String getHeaderAndTitle() {
		return "<html><header><title>" + VIEW_NAME + "</title></header>";
	}
	
	/**
	 * Returns HTML body when no bug is selected
	 * @return HTML string
	 */
	private String getDefaultBody() {
		return "<body>" + VIEW_DEFAULT_MESSAGE + "</body></html>";
	}
	
	/**
	 * Returns HTML body when a bug is selected
	 * @param bugInfo BugDetail object for the selected bug
	 * @return HTML string
	 */
	private String getBody(BugDetail bugInfo) {
		StringBuffer sb = new StringBuffer("<body>");
		BugInstance bug = bugInfo.getBugInstance();
		sb.append(constructParagraph(fmtBold(MESSAGE_LABEL), bug.getBugMessage()));
		String resolution = bug.getResolutionSuggestion();
		if (!("".equals(resolution))) {
			sb.append(constructParagraph(fmtBold(RESOLUTION_SUGGESTION_LABEL), resolution));
		}
		sb.append(constructParagraph(fmtBold(LINE_NUMBER_LABEL), bugInfo.getPrimaryLineNumber()));
		sb.append(constructParagraph(fmtBold(FILENAME_LABEL), bugInfo.getPrimaryFilename()));
		sb.append(constructParagraph(fmtBold(GROUP_LABEL), bug.getBugGroup()));
		sb.append(constructParagraph(fmtBold(CODE_LABEL), bug.getBugCode()));
		sb.append(constructParagraph(fmtBold(TOOL_LABEL), bugInfo.getTool()));
		sb.append(constructParagraph(fmtBold(PLATFORM_LABEL), bugInfo.getPlatform()));
		sb.append(constructFlowParagraphs(fmtBold(FLOW_LABEL), bugInfo.getFlow()));
		sb.append("</body></html>");
		return sb.toString();
	}
	
	/**
	 * Makes a paragraph element with label and value
	 * @param label label for the field
	 * @param value field's value
	 * @return HTML paragraph element string
	 */
	private StringBuffer constructParagraph(String label, String value) {
		StringBuffer sb = new StringBuffer("<p>");
		sb.append(label);
		sb.append(value);
		sb.append("</p>");
		return sb;
	}
	
	/**
	 * Makes HTML for displaying the flow
	 * @param flow list of locations
	 * @return HTML string
	 */
	private StringBuffer constructFlowParagraphs(String label, List<String> flow) {
		StringBuffer sb = new StringBuffer("");
		if (flow.isEmpty()) {
			return sb;
		}
		sb.append(label);
		for (String location : flow) {
			sb.append("<p>");
			sb.append(location);
			sb.append("</p>");
		}
		return sb;
	}
	
	/**
	 * Resets the browser to no bug being selected
	 */
	public void reset() {
		setDefaultHtml();
	}
	
	/**
	 * Updates the view to show information on the currently selected bug
	 * @param details BugDetail object for the currently selected bug
	 */
	public void update(BugDetail details) {
		if (details == null) {
			setDefaultHtml();
		}
		else {
			setHtml(details);
		}
	}
	
	/**
	 * Utility method for bolding some text in HTML
	 * @param text the text to be emphasized
	 * @return HTML String for bolding the given text
	 */
	private static String fmtBold(String text) {
		return "<b>" + text + "</b>";
	}

}
