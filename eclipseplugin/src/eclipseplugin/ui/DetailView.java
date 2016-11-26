package eclipseplugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import eclipseplugin.dialogs.DialogUtil;

public class DetailView extends ViewPart {
	Composite composite;
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
	
	public void redrawPartControl(String filename, String ln, String type, String tool, String platform) {
		filenameLabel.setText("Filename: " + filename);
		lineNumLabel.setText("Line Number: " + ln);
		typeLabel.setText("Type: " + type);
		toolLabel.setText("Tool: " + tool);
		platformLabel.setText("Platform: " + platform);
	}

}
