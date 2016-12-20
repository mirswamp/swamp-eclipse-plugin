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
package org.continuousassurance.swamp.eclipse.ui;

import org.continuousassurance.swamp.eclipse.dialogs.DialogUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class DetailView extends ViewPart {
	Composite composite;
	Label messageLabel;
	Label filenameLabel;
	Label lineNumLabel;
	Label typeLabel;
	Label toolLabel;
	Label platformLabel;
	
	@Override
	public void createPartControl(Composite parent) {
		composite = parent;
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		messageLabel = DialogUtil.initializeLabelWidget("Message: <message>", SWT.NONE, composite);
		 filenameLabel = DialogUtil.initializeLabelWidget("Filename: <filename>", SWT.NONE, composite);
		 lineNumLabel = DialogUtil.initializeLabelWidget("Line Number: <line number>", SWT.NONE, composite);
		 typeLabel = DialogUtil.initializeLabelWidget("Type: <type>", SWT.NONE, composite);
		 toolLabel = DialogUtil.initializeLabelWidget("Tool: <tool>", SWT.NONE, composite);
		 platformLabel = DialogUtil.initializeLabelWidget("Platform: <platform>", SWT.NONE, composite);

		
		// TODO: Add from SCARF data
		/*
		DialogUtil.initializeLabelWidget("Bug ID: <bug ID>", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Bug Group/Code: <bug group>", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Location: <filename>:<line number>", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Bug Message: <bug message>", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Bug Resolution: <bug resolution>", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Bug Severity: <bug severity>", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("List of methods", SWT.NONE, parent);
		*/
	}

	@Override
	public void setFocus() {
		composite.setFocus();
	}
	
	public void redrawPartControl(String message, String filename, String ln, String type, String tool, String platform) {
		messageLabel.setText("Message: " + message);
		filenameLabel.setText("Filename: " + filename);
		lineNumLabel.setText("Line Number: " + ln);
		typeLabel.setText("Type: " + type);
		toolLabel.setText("Tool: " + tool);
		platformLabel.setText("Platform: " + platform);
	}

}
