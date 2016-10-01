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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eclipseplugin.SubmissionInfo;

public class AdvancedSettingsDialog extends TitleAreaDialog {
	private Text buildOptText;
	private Text configOptText;
	private Text configScriptText;
	private Button selectFileButton;

	private static String ADVANCED_SETTINGS_TITLE = "Advanced Settings";
	private static String BUILD_OPTIONS_HELP = "Add flags to be used when your package is built.";		
	private static String CONFIG_COMMAND_HELP = "Write the command to be used prior to building your package";
	private static String CONFIG_OPTIONS_HELP = "Add flags to be used when your package is configured.";
	private static String SELECT_FILE_HELP = "Select your configure script";
	
	private Shell shell;
	private ConfigDialog parentDialog;
	
	public AdvancedSettingsDialog(Shell parentShell, ConfigDialog cd) {
		super(parentShell);
		shell = new Shell(parentShell);
		parentDialog = cd;
	}
	
	private void resetWidgets() {
		buildOptText.setText("");
		configOptText.setText("");
		configScriptText.setText("");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		int horizontalSpan = 2;
		
		setTitle(ADVANCED_SETTINGS_TITLE);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(4, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget("Build Options: ", SWT.NONE, container, horizontalSpan);
		buildOptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		buildOptText.setText(parentDialog.getBuildOpts());
		buildOptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, BUILD_OPTIONS_HELP));
		
		DialogUtil.initializeLabelWidget("Configuration Script: ", SWT.NONE, container, horizontalSpan);
		configScriptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		configScriptText.setText(parentDialog.getConfigScriptPath());
		configScriptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_COMMAND_HELP));
		
		selectFileButton = DialogUtil.initializeButtonWidget(container, " ...", new GridData(SWT.FILL, SWT.NONE, false, false), 1);
		selectFileButton.addSelectionListener(new FileSelectionListener());
		selectFileButton.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, SELECT_FILE_HELP));

		
		DialogUtil.initializeLabelWidget("Configuration Options: ", SWT.NONE, container, horizontalSpan);
		configOptText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		configOptText.setText(parentDialog.getConfigOpts());
		configOptText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, CONFIG_OPTIONS_HELP));

		return area;
	}
	
	@Override
	protected void okPressed() {
		parentDialog.setBuildOpts(buildOptText.getText());
		parentDialog.setConfigOpts(configOptText.getText());
		parentDialog.setConfigScriptPath(configScriptText.getText());
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
	
	private class FileSelectionListener implements SelectionListener {
		public FileSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			FileDialog dialog = new FileDialog(shell);
			String rc = dialog.open();
			if (rc != null) {
				configScriptText.setText(rc);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}
