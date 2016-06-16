/* Malcolm Reid Jr.
 * 06/07/2016
 * UW SWAMP
 * AuthenticationDialog.java
 * Code to implement a dialog box for getting SWAMP Authentication information
 */

package eclipseplugin.dialogs;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
//import src.main.java.edu.wisc.cs.swamp.ParseCommandLine;
//import src.main.java.edu.wisc.cs.swamp.*;
import edu.wisc.cs.swamp.*;
//import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.uiuc.ncsa.swamp.session.*;


public class AuthenticationDialog extends TitleAreaDialog {
	private Text usernameText;
	private Text passwordText;
	private static final String AUTHENTICATION_PROMPT = "Please enter your authentication information for the SWAMP.";
	private static final String INVALID_MESSAGE = "Invalid username or password.";
	private SwampApiWrapper api;
	private String id;
	
	public AuthenticationDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public void setSwampApiWrapper(SwampApiWrapper w) {
		api = w;
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
		
		Label usernameLabel = new Label(container, SWT.NONE);
		usernameLabel.setText("Username: ");
		// Grid data stuff
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = false;
		usernameLabel.setLayoutData(gd);
		usernameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData griddata = new GridData();
		griddata.grabExcessHorizontalSpace = true;
		griddata.horizontalAlignment = GridData.FILL;
		usernameText.setLayoutData(griddata);
		
		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("Password: ");
		passwordLabel.setLayoutData(gd);
		passwordText = new Text(container, SWT.PASSWORD | SWT.BORDER);
		passwordText.setLayoutData(griddata);
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Login", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	void setInvalidMsgAndClearPrompts() {
		// TODO: Add some formatting to the invalid message (e.g. bold)
		this.setMessage(INVALID_MESSAGE + "\n" + AUTHENTICATION_PROMPT);
		usernameText.setText("");
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
		System.out.println("Username: " + username + "\tPassword: " + password);
		// do the validation with the API using the things in the text fields
		
		id = api.login(username, password);
		if (id == null) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		super.okPressed();
	}
	
}