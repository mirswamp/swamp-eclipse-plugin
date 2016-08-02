/* Malcolm Reid Jr.
 * 06/07/2016
 * UW SWAMP
 * AuthenticationDialog.java
 * Code to implement a dialog box for getting SWAMP Authentication information
 */

package eclipseplugin.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
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
	private static final String AUTHENTICATION_PROMPT = "Please enter your authentication information for the SWAMP.";
	private static final String INVALID_MESSAGE = "Invalid username or password.";
	private SwampApiWrapper api;
	private String id;
	private MessageConsoleStream out;
	
	public AuthenticationDialog(Shell parentShell, SwampApiWrapper swampApi, MessageConsoleStream stream) {
		super(parentShell);
		api = swampApi;
		out = stream;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("SWAMP Authentication");
		this.setMessage(AUTHENTICATION_PROMPT);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		GridData griddata = new GridData();
		griddata.grabExcessHorizontalSpace = true;
		griddata.horizontalAlignment = GridData.FILL;

		DialogUtil.initializeLabelWidget("Username: ", SWT.NONE, container);
		usernameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, griddata);
		
		DialogUtil.initializeLabelWidget("Password: ", SWT.NONE, container);
		passwordText = DialogUtil.initializeTextWidget(SWT.PASSWORD | SWT.BORDER, container, griddata);
				
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Login", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
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