package eclipseplugin.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import edu.uiuc.ncsa.swamp.api.Tool;
import edu.wisc.cs.swamp.SwampApiWrapper;

public class ToolDialog extends TitleAreaDialog {
	private List<Tool> tools;
	private org.eclipse.swt.widgets.List swtToolList;
	private SwampApiWrapper api;
	private SubmissionInfo submissionInfo;

	public ToolDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		submissionInfo = si;
		api = submissionInfo.getApi();
	}
	
	@Override protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Tool Selection");
		
		/* Note: From GridData JavaDoc, "Do not reuse GridData objects. Every control in a composite
		 * that is managed by a GridLayout must have a unique GridData object.
		 */
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));

		DialogUtil.initializeLabelWidget("Tools: ", SWT.NONE, container);
		tools = api.getTools(submissionInfo.getPackageType(), submissionInfo.getSelectedProjectID());
		swtToolList = DialogUtil.initializeListWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), convertToolListToStringArray());
		
		if (submissionInfo.toolsInitialized()) {
			List<String> toolUUIDs = submissionInfo.getSelectedToolIDs();
			setSelectedTools(toolUUIDs);
		}
		else {
			// select all tools by default
			swtToolList.selectAll();
		}
			
		return area;
	}
	
	private String[] convertToolListToStringArray() {
		int numTools = tools.size();
		String[] toolArray = new String[numTools];
		for (int i = 0; i < numTools; i++) {
			toolArray[i] = tools.get(i).getName();
		}
		return toolArray;
	}
	
	private void setSelectedTools(List<String> toolUUIDs) {
		int count = 0;
		int numIDs = toolUUIDs.size();
		for (int i = 0; (i < tools.size()) && (count < numIDs); i++) {
			String id = tools.get(i).getUUIDString();
			if (toolUUIDs.contains(id)) {
				swtToolList.select(i);
				count++;
			}
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		Button button = createButton(parent, IDialogConstants.NO_ID, "Clear All", false);
		button.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	@Override
	protected void okPressed() {
		if (swtToolList.getSelectionCount() < 1) {
			this.setMessage("Select at least one tool.");
		}
		int[] selectedIndices = swtToolList.getSelectionIndices();
		List<String> selectedToolIDs = new ArrayList<String>(selectedIndices.length);
		for (int i : selectedIndices) {
			Tool tool = tools.get(i);
			selectedToolIDs.add(tool.getUUIDString());
		}
		submissionInfo.setSelectedToolIDs(selectedToolIDs);
		super.okPressed();
	}
	
private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			swtToolList.deselectAll();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}
