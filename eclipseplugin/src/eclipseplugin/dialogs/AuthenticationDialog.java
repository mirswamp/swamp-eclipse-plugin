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
import edu.wisc.cs.swamp.ParseCommandLine;
import edu.wisc.cs.swamp.*;
import edu.uiuc.ncsa.swamp.session.*;

public class AuthenticationDialog extends TitleAreaDialog {
	private Text usernameText;
	private Text passwordText;
	private String username;
	private String password;
	private static final String AUTHENTICATION_PROMPT = "Please enter your authentication information for the SWAMP.";
	private static final String INVALID_MESSAGE = "Invalid username or password.";
	
	public AuthenticationDialog(Shell parentShell) {
		super(parentShell);
	}
		
	@Override
	protected Control createDialogArea(Composite parent) {
		// TODO Increase width of username and password text fields
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Swamp Authentication");
		this.setMessage(AUTHENTICATION_PROMPT);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		Label usernameLabel = new Label(container, SWT.NONE);
		usernameLabel.setText("Username: ");
		usernameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData griddata = new GridData();
		griddata.grabExcessHorizontalSpace = true;
		griddata.horizontalAlignment = GridData.FILL;
		container.setLayoutData(griddata);
		
		Label passwordLabel = new Label(container, SWT.NONE);
		passwordLabel.setText("Password: ");
		passwordText = new Text(container, SWT.PASSWORD | SWT.BORDER);
		container.setLayoutData(griddata);
		
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
		username = usernameText.getText();
		password = passwordText.getText();
		if ((username.length() == 0) || (password.length() == 0)) {
			setInvalidMsgAndClearPrompts();
			return;
		}
		System.out.println("Username: " + username + "\tPassword: " + password);
		// do the validation with the API using the things in the text fields
		ArrayList<Session> sessions;
		//int val = ParseCommandLine.testAPI(3);
		//System.out.println(val);
		try {
		sessions = ParseCommandLine.getSessions(username, password);
		}
		catch (HTTPException h) {
			System.err.println("Error: " + h.getMessage());
			sessions = null;
		}
		finally {
			
		}
		System.out.println("Is sessions null? " + sessions == null);
		if (sessions == null) {
			System.out.println("Sessions is null");
			setInvalidMsgAndClearPrompts();
			return;
		}
		else {
			System.out.println("Number of sessions: " + sessions.size());
		}
		super.okPressed();
	}
	
}