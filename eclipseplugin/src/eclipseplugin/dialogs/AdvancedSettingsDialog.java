package eclipseplugin.dialogs;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eclipseplugin.SubmissionInfo;

public class AdvancedSettingsDialog extends TitleAreaDialog {
	private Text buildOptText;
	private Text configOptText;
	private Text configCmdText;
	private Text configDirText;

	private static String ADVANCED_SETTINGS_TITLE = "Advanced Settings";
	private static String BUILD_OPTIONS_HELP = "Add flags to be used when your package is built.";		
	private static String CONFIG_COMMAND_HELP = "Write the command to be used prior to building your package";
	private static String CONFIG_OPTIONS_HELP = "Add flags to be used when your package is configured.";
	private static String CONFIG_DIR_HELP = "Specify the directory in which your configure script is located.";
	
	private SubmissionInfo submissionInfo;
	private Shell shell;
	
	public AdvancedSettingsDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		shell = new Shell(parentShell);
		submissionInfo = si;
	}
	
	private void resetWidgets() {
		buildOptText.setText("");
		configOptText.setText("");
		configCmdText.setText("");
		configDirText.setText("");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		int horizontalSpan = 2;
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle(ADVANCED_SETTINGS_TITLE);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget("Build options: ", SWT.NONE, container);
		buildOptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		buildOptText.setText(submissionInfo.getBuildOpts());
		buildOptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, BUILD_OPTIONS_HELP));
		
		// Add config dir and config cmd
		DialogUtil.initializeLabelWidget("Configuration directory: ", SWT.NONE, container);
		configDirText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		configDirText.setText(submissionInfo.getConfigDir());
		configDirText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_DIR_HELP));
		
		
		DialogUtil.initializeLabelWidget("Coniguration command: ", SWT.NONE, container);
		configCmdText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		configCmdText.setText(submissionInfo.getConfigCmd());
		configCmdText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_COMMAND_HELP));
		
		DialogUtil.initializeLabelWidget("Configuration options: ", SWT.NONE, container);
		configOptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		configOptText.setText(submissionInfo.getConfigOpts());
		configOptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_OPTIONS_HELP));

		return area;
	}
	
	@Override
	protected void okPressed() {
		submissionInfo.setBuildOpts(buildOptText.getText());
		submissionInfo.setConfigOpts(configOptText.getText());
		submissionInfo.setConfigCmd(configCmdText.getText());
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		
		Button button = createButton(parent, IDialogConstants.NO_ID, DialogUtil.CLEAR_CAPTION, false);
		button.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, DialogUtil.OK_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	
	private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			resetWidgets();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}
