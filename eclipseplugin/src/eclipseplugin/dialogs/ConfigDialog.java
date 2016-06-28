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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import java.util.ArrayList;

public class ConfigDialog extends TitleAreaDialog {

	private Text buildDirText;
	private Text buildFileText;
	private Text buildTargetText;
	private Text prjFilePathText;
	private Text prjVersionText;
	private IProject project;
	private Combo prjCombo;
	private Combo buildSysCombo;
	
	private boolean needsBuildFile;
	private String pkgVersion;
	private String pkgName;
	private String buildSys;
	private String buildTarget;
	private String buildDir;
	private String buildFile;
	private String pkgPath;
	private String buildOptions[] = { "Auto-generate build file", "android+ant", "android+ant+ivy", "android+gradle", "android+maven", "ant", "ant+ivy", "gradle", "java-bytecode", "make", "Maven", "no-build", "other" };
	private static int NO_BUILD = 11;
	private static int AUTO_GENERATE_BUILD = 0;
	
	public String getPkgPath() {
		return pkgPath;
	}
	
	public String getPkgName() {
		return pkgName;
	}
	
	public String getBuildSys() {
		return buildSys;
	}
	
	public String getBuildDir() {
		return buildDir;
	}
	
	public String getBuildFile() {
		return buildFile;
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
	
	private String[] getProjectOptions() {
		IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		System.out.println("Workspace: " + ResourcesPlugin.getWorkspace());
		System.out.println("Root: " + ResourcesPlugin.getWorkspace().getRoot());
		ArrayList<String> list = new ArrayList<String>();
		String[] ary = null;
		for (IProject prj : projects) {
			System.out.println(prj.getName());
			list.add(prj.getName());
			try {
				prj.open(null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle("Build Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget("Package Name: ", SWT.NONE, container);
		String prjOptions[] = getProjectOptions();
		prjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), prjOptions);
		prjCombo.addSelectionListener(new ComboSelectionListener(prjCombo, Type.PROJECT));
		
		DialogUtil.initializeLabelWidget("Package Version: ", SWT.NONE, container);
		prjVersionText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));	

		DialogUtil.initializeLabelWidget("Filepath: ", SWT.NONE, container);
		prjFilePathText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));	
	
		DialogUtil.initializeLabelWidget("Build System: ", SWT.NONE, container);
		buildSysCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), buildOptions);		
		buildSysCombo.addSelectionListener(new ComboSelectionListener(buildSysCombo, Type.BUILD));
		
		DialogUtil.initializeLabelWidget("Build Directory: ", SWT.NONE, container);
		buildDirText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		buildDirText.setText(".");

		DialogUtil.initializeLabelWidget("Build File: ", SWT.NONE, container);
		buildFileText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));

		DialogUtil.initializeLabelWidget("Build Target: ", SWT.NONE, container);
		buildTargetText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));

		return area;
	}
	
	public boolean needsGeneratedBuildFile() {
		return needsBuildFile;
	}
	
	private boolean isValid() {
		if (prjCombo.getSelectionIndex() < 0) {
			this.setMessage("Please select a project");
			return false;
		}
		int selection = buildSysCombo.getSelectionIndex();
		if (selection < 0) {
			this.setMessage("Please select a build system");
			return false;
		}
		if ((selection == NO_BUILD) || (selection == AUTO_GENERATE_BUILD)) {
			return true;
		}
		if (buildDirText.getText().equals("")) {
			this.setMessage("Please enter a valid build directory");
			return false;
		}
		if (buildFileText.getText().equals("")) {
			this.setMessage("Please enter a valid build file");
		}
		if (buildTargetText.getText().equals("")) {
			this.setMessage("Please enter a valid build target");
			return false;
		}
		return true;
	}
	
	@Override
	protected void okPressed() {
		// Here we do some checks to make sure that everything has actually been populated
		if (isValid()) {
			pkgVersion = prjVersionText.getText();
			int selection = buildSysCombo.getSelectionIndex();
			if ((selection != NO_BUILD) && (selection != AUTO_GENERATE_BUILD)) {
				buildDir = buildDirText.getText();
				buildFile = buildFileText.getText();
				buildTarget = buildTargetText.getText();
			}
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
				if (selection == NO_BUILD || selection == AUTO_GENERATE_BUILD) {
					buildTargetText.setText("");
					buildTargetText.setEnabled(false);
					buildDirText.setText("");
					buildDirText.setEnabled(false);
					buildFileText.setText("");
					buildFileText.setEnabled(false);
				}
				else {
					buildTargetText.setEnabled(true);
					buildDirText.setEnabled(true);
					buildFileText.setEnabled(true);
				}
				if (selection > -1) {
					if (selection == AUTO_GENERATE_BUILD) {
						needsBuildFile = true;
						buildSys = "ant";
						buildTarget = "build";
						buildDir = ".";
						buildFile = "build.xml";
					}
					else {
						buildSys = buildOptions[selection];
					}
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
