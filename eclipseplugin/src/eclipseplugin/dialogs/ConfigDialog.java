package eclipseplugin.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import java.util.List;
import java.util.ArrayList;
public class ConfigDialog extends TitleAreaDialog {

	private HandlerFactory handler;
	private Text buildText;
	private Text prjFilePathText;
	private Text prjVersionText;
	private IProject project;
	
	private String pkgVersion;
	private String pkgName;
	private String buildSys;
	private String buildTarget;
	private String pkgPath;
	private String buildOptions[] = { "Auto-generate build file", "ant", "ant+ivy", "Maven", "No build" };

	
	public String getPkgPath() {
		return pkgPath;
	}
	
	public String getPkgName() {
		return pkgName;
	}
	
	public String getBuildSys() {
		return buildSys;
	}
	
	public String getBuildTarget() {
		return buildTarget;
	}
	
	public String getPkgVersion() {
		return pkgVersion;
	}
	
	public IProject getProject() {
		return project;
	}
	
	private enum Type {
		PROJECT, BUILD
	}
	
	public ConfigDialog(Shell parentShell) {
		super(parentShell);
	}
	
	private void setProjectCombo(Combo c) {
		IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		System.out.println("Workspace: " + ResourcesPlugin.getWorkspace());
		System.out.println("Root: " + ResourcesPlugin.getWorkspace().getRoot());
		ArrayList<String> list = new ArrayList<String>();
		for (IProject prj : projects) {
			System.out.println(prj.getName());
			list.add(prj.getName());
			try {
				prj.open(null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Project path2: " + prj.getLocation());
		}
		if (list != null) {
			String[] ary = new String[list.size()];
			list.toArray(ary);
			c.setItems(ary);
		}
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle("Build Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		GridData lblGridData = new GridData();
		lblGridData.horizontalAlignment = GridData.FILL;
		lblGridData.grabExcessHorizontalSpace = false;
		GridData elementGridData = new GridData();
		elementGridData.horizontalAlignment = GridData.FILL;
		elementGridData.grabExcessHorizontalSpace = true;
		
		
		Label prjName = new Label(container, SWT.NONE);
		prjName.setText("Project Name: ");
		prjName.setLayoutData(lblGridData);
		
		/*
		Text prjText = new Text(container, SWT.SINGLE | SWT.BORDER);
		prjText.setLayoutData(elementGridData);
		*/
		
		Combo project = new Combo(container, SWT.DROP_DOWN);
		setProjectCombo(project);
		project.setLayoutData(elementGridData);
		project.addSelectionListener(new ComboSelectionListener(project, Type.PROJECT));
		
		
		Label prjVersion = new Label(container, SWT.NONE);
		prjVersion.setText("Project Version: ");
		prjVersionText = new Text(container, SWT.SINGLE | SWT.BORDER);
		prjVersionText.setLayoutData(elementGridData);
		
		Label prjFilepath = new Label(container, SWT.NONE);
		prjFilepath.setText("Project Filepath: ");
		prjFilepath.setLayoutData(lblGridData);
		
		// This should probably be a label
		prjFilePathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		prjFilePathText.setLayoutData(elementGridData);
		/*Text prjFilepathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		prjFilepathText.setLayoutData(elementGridData);
		*/
		Label buildSystem = new Label(container, SWT.NONE);
		buildSystem.setText("Build System: ");
		buildSystem.setLayoutData(lblGridData);
		Combo buildSysCombo = new Combo(container, SWT.DROP_DOWN);
		// set its elements
		buildSysCombo.setLayoutData(elementGridData);
		buildSysCombo.setItems(buildOptions);
		buildSysCombo.addSelectionListener(new ComboSelectionListener(buildSysCombo, Type.BUILD));
		
		Label buildFilepath = new Label(container, SWT.NONE);
		buildFilepath.setText("Build Filepath: ");
		buildFilepath.setLayoutData(lblGridData);
		buildText = new Text(container, SWT.SINGLE | SWT.BORDER);
		buildText.setLayoutData(elementGridData);
		
		return area;
	}
	
	public void setHandlerFactory(HandlerFactory h) {
		handler = h;
	}
	
	private boolean isValid() {
		// TODO add actual checks here for the text and combo fields
		return true;
	}
	
	@Override
	protected void okPressed() {
		// Here we do some checks to make sure that everything has actually been populated
		if (isValid()) {
			pkgVersion = prjVersionText.getText();
			buildTarget = buildText.getText();
			
			super.okPressed();
		}
	}
	
	private class ComboSelectionListener implements SelectionListener {
		Combo combo;
		Type type;
		public ComboSelectionListener(Combo c, Type t) {
			combo = c;
			type = t;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
			if (type == Type.BUILD) {
				if (selection == 0) {
					buildText.setText("");
					buildText.setEnabled(false);
				}
				else {
					buildText.setEnabled(true);
				}
				if (selection > -1) {
					buildSys = buildOptions[selection];
				}
				else {
					buildSys = null;
				}
			}
			else if (type == Type.PROJECT) {
				// populate filepath text
				IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				project = projects[selection];
				pkgName = project.getName();
				IPath p = project.getLocation();
				if (p == null)
					return;
				pkgPath = p.toString();//project.getWorkingLocation(pkgName).toString();
				prjFilePathText.setText(pkgPath);
				prjFilePathText.setEnabled(false);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
		}
	}
}
