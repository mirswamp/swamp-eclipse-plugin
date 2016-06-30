package eclipseplugin.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import java.util.ArrayList;

public class ConfigDialog extends TitleAreaDialog {

	/* Instance variables representing widgets */
	private Text buildDirText;
	private Text buildFileText;
	private Text buildTargetText;
	private Text prjFilePathText;
	private Text prjVersionText;
	private Combo prjCombo;
	private Combo buildSysCombo;
	
	/* Instance variables representing state */
	private boolean needsBuildFile;
	private IProject project;
	private int prjIndex;
	private String pkgVersion;
	private String pkgName;
	private String pkgPath;
	private String buildSys;
	private int buildSysIndex;
	private String buildTarget;
	private String buildDir;
	private String buildFile;
	private String buildOptions[] = { "Auto-generate build file", "android+ant", "android+ant+ivy", "android+gradle", "android+maven", "ant", "ant+ivy", "gradle", "java-bytecode", "make", "Maven", "no-build", "other" };
	
	
	private static int NO_BUILD = 11;
	private static int AUTO_GENERATE_BUILD = 0;
	
	public String getPkgPath() {
		return pkgPath;
	}
	
	public void setPkgPath(String path) {
		pkgPath = path;
	}
	
	public String getPkgName() {
		return pkgName;
	}
	
	public void setPkgName(String name) {
		pkgName = name;
	}
	
	public String getBuildSys() {
		return buildSys;
	}
	
	public void setBuildSys(String strBuildSys) {
		buildSys = strBuildSys;
	}
	
	public String getBuildDir() {
		return buildDir;
	}
	
	public void setBuildDir(String strBuildDir) {
		buildDir = strBuildDir;
	}
	
	public String getBuildFile() {
		return buildFile;
	}
	
	public void setBuildFile(String strBuildFile) {
		buildFile = strBuildFile;
	}
	
	public String getBuildTarget() {
		return buildTarget;
	}
	
	public void setBuildTarget(String strBuildTarget) {
		buildTarget = strBuildTarget;
	}
	
	public String getPkgVersion() {
		return pkgVersion;
	}
	
	public void setPkgVersion(String strPkgVersion) {
		pkgVersion = strPkgVersion;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public int getBuildSysIndex() {
		return buildSysIndex;
	}
	
	private enum Type {
		PROJECT, BUILD
	}
	
	public ConfigDialog(Shell parentShell) {
		super(parentShell);
		resetState();
	}
	
	public void resetState() {
		prjIndex = -1;
		buildSysIndex = -1;
		needsBuildFile = false;
		pkgVersion = null;
		pkgName = null;
		pkgPath = null;
		buildSys = null;
		buildTarget = null;
		buildDir = null;
		buildFile = null;
		project = null;
	}
	
	private void resetWidgets() {
		buildDirText.setText("");
		buildDirText.setEnabled(true);
		buildFileText.setText("");
		buildFileText.setEnabled(true);
		buildTargetText.setText("");
		buildTargetText.setEnabled(true);
		prjFilePathText.setText("");
		prjFilePathText.setEnabled(true);
		prjVersionText.setText("");
		prjVersionText.setEnabled(true);
		prjCombo.deselectAll();
		if (prjCombo.getItemCount() == 1) {
			prjCombo.select(0);
			handleProjectSelection(0);
		}
		buildSysCombo.deselectAll();
		if (buildSysCombo.getItemCount() == 1) {
			buildSysCombo.select(0);
			handleBuildSelection(0);
		}
	}

	public boolean initializeProject(String strPackageName, String strPackagePath) {
IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.getName().equals(strPackageName) && project.getLocation().toString().equals(strPackagePath)) {
				prjIndex = i;
				return true;
			}
		}
		return false;
	}
	
	public boolean initializeBuild(int iBuildIndex, String strBuildFile, String strBuildDir, String strBuildTarget) {
		if (iBuildIndex > -1 && iBuildIndex < buildOptions.length ) {
			buildSysIndex = iBuildIndex;
			buildFile = strBuildFile;
			buildDir = strBuildDir;
			buildTarget = strBuildTarget;
			return true;
		}
		return false;
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
		
		if (prjIndex > -1) {
			// We've read this from file
			prjCombo.select(prjIndex);
			handleProjectSelection(prjIndex);
			if (pkgVersion != null) {
				prjVersionText.setText(pkgVersion);
			}
		}
		else {
			if (prjCombo.getItemCount() == 1) {
				prjCombo.select(0);
				handleProjectSelection(0);
			}
		}
	
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
		
		if (buildSysIndex > -1) {
			// We've read this from file
			buildSysCombo.select(buildSysIndex);
			handleBuildSelection(buildSysIndex);
			if (!(buildSysIndex == NO_BUILD) && (!(buildSysIndex == AUTO_GENERATE_BUILD))) {
				if (buildDir != null) {
					buildDirText.setText(buildDir);
				}
				if (buildFile != null && !buildFile.equals("")) {
					buildFileText.setText(buildFile);
				}
				if (buildTarget != null) {
					buildTargetText.setText(buildTarget);
				}
			}
		}
		else {
			if (buildSysCombo.getItemCount() == 1) {
				buildSysCombo.select(0);
				handleBuildSelection(0);
			}
		}
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		
		Button button = createButton(parent, IDialogConstants.NO_ID, "Clear All", false);
		button.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	public void handleBuildSelection(int selection) {
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
	}
	
	public void handleProjectSelection(int selection) {
		// populate filepath text
		IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		IProject project = projects[selection];
		IPath p = project.getLocation();
		if (p == null)
			return;
		prjFilePathText.setText(p.toString());
		prjFilePathText.setEnabled(false);
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
			IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			project = projects[prjCombo.getSelectionIndex()];
			pkgName = project.getName();
			pkgPath = project.getLocation().toString();
			buildSysIndex = buildSysCombo.getSelectionIndex();
			if (buildSysIndex == AUTO_GENERATE_BUILD) {
				needsBuildFile = true;
				buildSys = "ant";
				buildTarget = "build";
				buildDir = ".";
				buildFile = "build.xml";
			}
			else if (buildSysIndex == NO_BUILD) {
				buildSys = "no-build";
				buildTarget = "";
				buildDir = "";
				buildFile = "";
			}
			else {
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
				handleBuildSelection(selection);
			}
			else if (type == Type.PROJECT) {
				handleProjectSelection(selection);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
		}
	}
	
	private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			resetState();
			resetWidgets();

		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}
