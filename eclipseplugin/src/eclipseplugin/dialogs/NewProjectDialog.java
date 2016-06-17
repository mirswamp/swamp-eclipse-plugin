package eclipseplugin.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.session.Session;

public class NewProjectDialog extends TitleAreaDialog {
	
	Project project;
	Text affiliationText;
	Text descriptionText;
	Text fullnameText;
	Text shortnameText;
	
	public NewProjectDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public String getProjectUUID() {
		return project.getIdentifierString();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle("Project Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		GridData lblGridData = new GridData();
		lblGridData.horizontalAlignment = GridData.FILL;
		lblGridData.grabExcessHorizontalSpace = false;
		GridData elementGridData = new GridData();
		elementGridData.horizontalAlignment = GridData.FILL;
		elementGridData.grabExcessHorizontalSpace = true;
		
		DialogUtil.initializeLabelWidget("Affiliation: ", SWT.NONE, container, lblGridData);
		affiliationText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, elementGridData);
		
		DialogUtil.initializeLabelWidget("Description: ", SWT.NONE, container, lblGridData);
		descriptionText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, elementGridData);
				
		DialogUtil.initializeLabelWidget("Full name (Required): ", SWT.NONE, container, lblGridData);
		fullnameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, elementGridData);
		
		DialogUtil.initializeLabelWidget("Short name: ", SWT.NONE, container, lblGridData);
		shortnameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, elementGridData);
		
		return area;
		
	}
	
	public Project createNewProject() {
		return new Project(new Session(new String()));
	}
	
	@Override
	protected void okPressed() {
		// TODO Add validation
		project = createNewProject();
	}
	
}
