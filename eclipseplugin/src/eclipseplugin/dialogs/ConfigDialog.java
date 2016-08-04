package eclipseplugin.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eclipseplugin.SubmissionInfo;
import static eclipseplugin.SubmissionInfo.AUTO_GENERATE_BUILD_STRING;
import static eclipseplugin.SubmissionInfo.NO_BUILD_STRING;
import eclipseplugin.Utils;
import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.Project;
import edu.wisc.cs.swamp.SwampApiWrapper;

import java.awt.Window;
import java.util.List;

public class ConfigDialog extends TitleAreaDialog {

	/* Instance variables representing widgets */
	private Text buildTargetText;
	private Text prjFilePathText;
	private Text pkgVersionText;
	private Text pkgNameText;
	private Combo swampPrjCombo;
	private Combo eclipsePrjCombo;
	private Combo pkgCombo;
	private Combo pkgTypeCombo;
	private Combo buildSysCombo;
	private Button packageRTButton;
	private Button selectFileButton;
	private Text buildPathText;
	private String fullPath;
	
	private Shell shell;
	
	/* Instance variables representing state */
	private List<Project> swampProjects;
	private IProject[] eclipseProjects;
	private List<PackageThing> swampPackages;

	private String prjUUID;
	
	private SubmissionInfo submissionInfo;
	private SwampApiWrapper api;
	
	private static int CREATE_NEW_PACKAGE = 0;
	
	private enum Type {
		PACKAGE_TYPE, ECLIPSE_PROJECT, BUILD, PACKAGE, SWAMP_PROJECT
	}
	
	public ConfigDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		submissionInfo = si;
		api = submissionInfo.getApi();
		shell = parentShell;
	}
	
	private void resetWidgets() {
		buildPathText.setText("");
		buildPathText.setEditable(false);
		buildTargetText.setText("");
		buildTargetText.setEnabled(true);
		prjFilePathText.setText("");
		pkgVersionText.setText("");
		pkgVersionText.setEnabled(false);  
		pkgNameText.setText("");
		pkgNameText.setEnabled(false);
		swampPrjCombo.deselectAll();
		eclipsePrjCombo.deselectAll();
		pkgCombo.setItems("");
		pkgTypeCombo.deselectAll();
		buildSysCombo.deselectAll();
		setDefaults();	
	}
	
	private void setDefaults() {
		setSwampProjectDefault();
		setEclipseProjectDefault();
		setPackageDefault();
		setBuildSysDefault();
		setPackageTypeDefault();
	}

	private void setEclipseProjectDefault() {
		if (eclipsePrjCombo.getItemCount() == 1) {
			eclipsePrjCombo.select(0);
			handleEclipseProjectSelection(0);
		}
	}
	
	private void setSwampProjectDefault() {
		if (swampPrjCombo.getItemCount() == 1) {
			swampPrjCombo.select(0);
			handleSwampProjectSelection(0);
		}
	}
	
	private void setPackageDefault() {
		if (prjUUID == null) {
			return;
		}
		if (pkgCombo.getItemCount() == 1) {
			pkgCombo.select(0);
			handlePackageSelection(0);
		}
	}
	
	private void setBuildSysDefault() {
		if (buildSysCombo.getItemCount() == 1) {
			buildSysCombo.select(0);
			handleBuildSelection(0);
		}
	}
	
	private void setPackageTypeDefault() {
		if (pkgTypeCombo.getItemCount() == 1) {
			pkgTypeCombo.select(0);
		}
	}
	
	private String[] getSelectionElements(Type type) {
		if (type == Type.ECLIPSE_PROJECT) { // Eclipse project
			return getEclipseProjectList();
		}
		if (type == Type.SWAMP_PROJECT) {
			return getSwampProjectList();
			
		}
		if (type == Type.BUILD) {
			return getBuildSystemList();
		}
		if (type == Type.PACKAGE) {
			// SWAMP Package
			return getSwampPackageList();
		}
		// Package Type
		return getPackageTypeList();
	}
	
	private String[] getEclipseProjectList() {
		if (eclipseProjects == null) {
			eclipseProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		}
		int length = eclipseProjects.length;
		String[] array = new String[length];
		for (int i = 0; i < length; i++) {
			IProject prj = eclipseProjects[i];
			String name = prj.getName();
			System.out.println(name);
			try {
				prj.open(null);
			} catch (CoreException e) {
				System.err.println("Unable to open project " + name);
			}
			array[i] = name;
		}
		return array;
	}
	
	private String[] getSwampProjectList() {
		swampProjects = api.getProjectsList();
		int size = swampProjects.size();
		String[] array = new String[size];
		for (int i = 0; i < size; i++) {
			array[i] = swampProjects.get(i).getFullName();
		}
		return array;
	}
	
	private String[] getBuildSystemList() {
		return submissionInfo.getBuildSystemList();
	}
	
	private String[] getSwampPackageList() {
		if (prjUUID == null) {
			String[] array = {""};
			return array;
		}
		swampPackages = api.getPackagesList(prjUUID);
		int numPackages = swampPackages.size() + 1;
		String[] pkgNames = new String[numPackages];
		pkgNames[0] = "Create new package";
		for (int i = 1; i < numPackages; i++) {
			pkgNames[i] = swampPackages.get(i-1).getName();
		}
		return pkgNames; 
	}
	
	private String[] getPackageTypeList() {
		List<String> pkgTypes = api.getPackageTypesList();
		return Utils.convertStringListToArray(pkgTypes);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		int horizontalSpan = 2;
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle("Build Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(4, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget("SWAMP Project: ", SWT.NONE, container, horizontalSpan);
		String swampPrjOptions[] = getSelectionElements(Type.SWAMP_PROJECT);
		swampPrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), swampPrjOptions, horizontalSpan);
		swampPrjCombo.addSelectionListener(new ComboSelectionListener(swampPrjCombo, Type.SWAMP_PROJECT));
		
		DialogUtil.initializeLabelWidget("SWAMP Package: ", SWT.NONE, container, horizontalSpan);
		String pkgOptions[] = getSelectionElements(Type.PACKAGE);
		pkgCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), pkgOptions, horizontalSpan);
		pkgCombo.addSelectionListener(new ComboSelectionListener(pkgCombo, Type.PACKAGE));
		pkgCombo.setEnabled(false);
		
		DialogUtil.initializeLabelWidget("New Package Name: ", SWT.NONE, container, horizontalSpan);
		pkgNameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		pkgNameText.setEnabled(false);
		
		DialogUtil.initializeLabelWidget("Package Version: ", SWT.NONE, container, horizontalSpan);
		pkgVersionText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);	
		pkgVersionText.setText(submissionInfo.getPackageVersion());
		pkgVersionText.setEnabled(false);
		
		DialogUtil.initializeLabelWidget("Package Type: ", SWT.NONE, container, horizontalSpan);
		String pkgTypes[] = getSelectionElements(Type.PACKAGE_TYPE);
		pkgTypeCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), pkgTypes, horizontalSpan);
		pkgTypeCombo.setEnabled(false);
		
		DialogUtil.initializeLabelWidget("Eclipse Project: ", SWT.NONE, container, horizontalSpan);
		String eclipsePrjOptions[] = getSelectionElements(Type.ECLIPSE_PROJECT);
		eclipsePrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), eclipsePrjOptions, horizontalSpan);
		eclipsePrjCombo.addSelectionListener(new ComboSelectionListener(eclipsePrjCombo, Type.ECLIPSE_PROJECT));

		DialogUtil.initializeLabelWidget("Filepath: ", SWT.NONE, container, horizontalSpan);
		prjFilePathText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		prjFilePathText.setEditable(false);
		
		DialogUtil.initializeLabelWidget("Build System: ", SWT.NONE, container, horizontalSpan);
		String[] buildSysOptions = getSelectionElements(Type.BUILD);
		buildSysCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), buildSysOptions, horizontalSpan);		
		buildSysCombo.addSelectionListener(new ComboSelectionListener(buildSysCombo, Type.BUILD));
		
		DialogUtil.initializeLabelWidget("", SWT.NONE, container, horizontalSpan);
		packageRTButton = DialogUtil.initializeButtonWidget(container, "Package System Libraries?", new GridData(SWT.FILL, SWT.NONE, true, false), SWT.CHECK, horizontalSpan);
		packageRTButton.setEnabled(false);
		
		DialogUtil.initializeLabelWidget("Build File: ", SWT.NONE, container, horizontalSpan);
		buildPathText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		buildPathText.setEditable(false);
		selectFileButton = DialogUtil.initializeButtonWidget(container, " ... ", new GridData(SWT.FILL, SWT.NONE, false, false), SWT.PUSH, 1);
		selectFileButton.addSelectionListener(new FileSelectionListener());

		DialogUtil.initializeLabelWidget("Build Target: ", SWT.NONE, container, horizontalSpan);
		buildTargetText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		
		if (submissionInfo.isConfigInitialized()) {
			setupSwampProject();
			setupSwampPackage();
			setupPackageType();
			setupEclipseProject();
			setupBuild();
		}
		else {
			setDefaults();
		}
		
		return area;
	}
	
	private void setupPackageType() {
		// get the appropriate pkgType String, not the index
		String pkgType = submissionInfo.getPackageType();
		System.out.println("Package Type: |" + pkgType + "|");
		for (int i = 0; i < pkgTypeCombo.getItemCount(); i++) {
			System.out.println("Package type in combo: |" + pkgTypeCombo.getItem(i) + "|");
			if (pkgTypeCombo.getItem(i).equals(pkgType)) {
				pkgTypeCombo.select(i);
				return;
			}
		}
	}
	
	private void setupEclipseProject() {
		IProject project = submissionInfo.getProject();
		for (int i = 0; i < eclipseProjects.length; i++) {
			if (project.equals(eclipseProjects[i])) {
				eclipsePrjCombo.select(i);
				handleEclipseProjectSelection(i);
				return;
			}
		}
		handleEclipseProjectSelection(-1);
	}
	
	private void handleEclipseProjectSelection(int index) {
		if (index < 0) {
			prjFilePathText.setText("");
		}
		else {
			IProject project = eclipseProjects[index];	
			prjFilePathText.setText(project.getLocation().toOSString());
		}
	}
	
	private void setupSwampProject() {
		prjUUID = submissionInfo.getSelectedProjectID();
		for (int i = 0; i < swampProjects.size(); i++) {
			if (swampProjects.get(i).getUUIDString().equals(prjUUID)) {
				swampPrjCombo.select(i);
				handleSwampProjectSelection(i);
				return;
			}
		}
		prjUUID = null;
		handleSwampProjectSelection(-1);
	}
	
	private void handleSwampProjectSelection(int index) {
		if (index < 0) {
			prjUUID = null;
			swampPrjCombo.deselectAll();
			pkgCombo.deselectAll();
			pkgCombo.setEnabled(false);
			pkgVersionText.setEnabled(false);
			pkgTypeCombo.deselectAll();
			pkgTypeCombo.setEnabled(false);
			pkgCombo.setItems("");
		}
		else {
			prjUUID = swampProjects.get(index).getUUIDString();
			pkgCombo.setEnabled(true);
			pkgVersionText.setEnabled(true);
			pkgTypeCombo.setEnabled(true);
			pkgCombo.setItems(getSelectionElements(Type.PACKAGE));
			setPackageDefault();
		}
	}
	
	private void setupSwampPackage() {
		// get the appropriate UUID from submissionInfo
		// disable the text box
		String pkgThingUUID = submissionInfo.getSelectedPackageID();
		for (int i = 0; i < swampPackages.size(); i++) {
			if (swampPackages.get(i).getUUIDString().equals(pkgThingUUID)) {
				pkgCombo.select(i+1);
				handlePackageSelection(i+1);
				return;
			}
		}
		handlePackageSelection(-1);
		// if "Create new package" selected, enable the text box
	}
	
	private void handlePackageSelection(int index) {
		pkgNameText.setText("");
		if (index == CREATE_NEW_PACKAGE) {
			pkgNameText.setEnabled(true);
		}
		else {
			pkgNameText.setEnabled(false);
		}
	}
	
	private void setupBuild() {
		// get the build system, not an index
		// set it
		String buildSys = null;
		if (submissionInfo.needsBuildFile()) {
			buildSys = AUTO_GENERATE_BUILD_STRING; 
			packageRTButton.setSelection(submissionInfo.packageSystemLibraries());
		}
		else {
			buildSys = submissionInfo.getBuildSystem();
		}
		for (int i = 0; i < buildSysCombo.getItemCount(); i++) {
			if (buildSys.equals(buildSysCombo.getItem(i))) {
				buildSysCombo.select(i);
				handleBuildSelection(i);
				return;
			}
		}
		handleBuildSelection(-1);
		// if auto or no-build, keep the build text boxes disabled
	}
	
	private void handleBuildSelection(int index) {
		// TODO There is a more elegant way of handling build than hard coding these strings in multiple places
		String buildSys =  buildSysCombo.getItem(index);
		if (buildSys.equals(AUTO_GENERATE_BUILD_STRING) || buildSys.equals(NO_BUILD_STRING)) {
			selectFileButton.setEnabled(false);
			buildTargetText.setText("");
			buildTargetText.setEnabled(false);
			buildPathText.setText("");
			buildPathText.setEnabled(false);
			if (buildSys.equals(AUTO_GENERATE_BUILD_STRING)) {
				packageRTButton.setEnabled(true);
			}
		}
		else {
			selectFileButton.setEnabled(true);
			buildTargetText.setEnabled(true);
			buildPathText.setEnabled(true);
			packageRTButton.setSelection(false);
			packageRTButton.setEnabled(false);
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
	
	private boolean isValid() {
		int swampPrjIndex = swampPrjCombo.getSelectionIndex();
		if (swampPrjIndex < 0) {
			this.setMessage("Please select a SWAMP project.");
		}
		
		int pkgIndex = pkgCombo.getSelectionIndex();
		if (pkgIndex < 0) {
			this.setMessage("Please select a package.");
			return false;
		}
		
		if (pkgIndex == CREATE_NEW_PACKAGE) {
			if (pkgNameText.equals("")) {
				this.setMessage("Please add a name for your new package.");
				return false;
			}
		}
		
		if (pkgVersionText.equals("")) {
			this.setMessage("Please add a descriptor for your package version.");
			return false;
		}
		
		int pkgTypeIndex = pkgTypeCombo.getSelectionIndex();
		if (pkgTypeIndex < 0) {
			this.setMessage("Please select a package type.");
			return false;
		}
		
		int eclipsePrjIndex = eclipsePrjCombo.getSelectionIndex();
		if (eclipsePrjIndex < 0) {
			this.setMessage("Please select an Eclipse project.");
			return false;
		}
		
		int buildIndex = buildSysCombo.getSelectionIndex();
		if (buildIndex < 0) {
			this.setMessage("Please select a build system.");
			return false;
		}
		String buildSysString = buildSysCombo.getItem(buildIndex);
		if (buildSysString.equals(NO_BUILD_STRING) || buildSysString.equals(AUTO_GENERATE_BUILD_STRING)) {
			return true;
		}
		if (buildPathText.getText().equals("")) { // TODO Add check to make sure build file is within project directory
			this.setMessage("Please select a valid build file.");
		}
		if (buildTargetText.getText().equals("")) {
			this.setMessage("Please enter a valid build target.");
			return false;
		}
		return true;
	}
	

	@Override
	protected void okPressed() {
		if (isValid()) {
			// do a lot of setting of submissionInfo
			
			// swamp project (UUID needed)
			submissionInfo.setSelectedProjectID(prjUUID);
			
			// swamp package (UUID if available, otherwise name)
			int index = pkgCombo.getSelectionIndex();
			if (index == CREATE_NEW_PACKAGE) {
				submissionInfo.setPackageName(pkgNameText.getText());
				submissionInfo.setNewPackage(true);
			}
			else {
				String pkgThingUUID = swampPackages.get(index-1).getUUIDString();
				submissionInfo.setSelectedPackageID(pkgThingUUID);
				submissionInfo.setNewPackage(false);
			}
			
			// swamp package version (String)
			submissionInfo.setPackageVersion(pkgVersionText.getText());
			
			// package type
			index = pkgTypeCombo.getSelectionIndex();
			submissionInfo.setPackageType(pkgTypeCombo.getItem(index));
			
			// eclipse project (actual IProject)
			index = eclipsePrjCombo.getSelectionIndex();
			IProject project = eclipseProjects[index];
			submissionInfo.setProject(project);
			
			// build system (build system info -- including for create_new...)
			index = buildSysCombo.getSelectionIndex();
			String buildSysStr = buildSysCombo.getItem(index);
			String relativeDir = "";
			String buildFile = "";
			boolean createBuildFile = false;
			if (buildSysStr.equals(AUTO_GENERATE_BUILD_STRING)) {
				createBuildFile = true;
			}
			else {
				relativeDir = getRelativeBuildDir(project.getLocation().toOSString(), fullPath);
				buildFile = buildPathText.getText();
				System.out.println("Relative Directory: " + relativeDir);
				System.out.println("Build file: " + buildFile);
			}
			submissionInfo.setBuildInfo(buildSysStr, buildSysStr.equals(AUTO_GENERATE_BUILD_STRING), relativeDir, buildFile, buildTargetText.getText(), packageRTButton.getSelection());
			//submissionInfo.setBuildInfo(buildSysStr, buildSysStr.equals(AUTO_GENERATE_BUILD_STRING), buildDirText.getText(), buildFileText.getText(), buildTargetText.getText(), packageRTButton.getSelection());
			super.okPressed();
		}
	}
	
	private String getRelativeBuildDir(String projectDir, String buildPath) {
		int index = projectDir.lastIndexOf(Path.SEPARATOR);
		int fileIndex = buildPath.lastIndexOf(Path.SEPARATOR);
		String relPath = buildPath.substring(index+1, fileIndex);
		System.out.println("Rel path: " + relPath);
		if (relPath.equals("")) {
			relPath = ".";
		}
		return relPath;
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
			else if (type == Type.SWAMP_PROJECT) {
				handleSwampProjectSelection(selection);
			}
			else if (type == Type.ECLIPSE_PROJECT) {
				handleEclipseProjectSelection(selection);
			}
			else if (type == Type.PACKAGE){ 
				handlePackageSelection(selection);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
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
			String path = prjFilePathText.getText();
			if (path.equals("")) {
				path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			}
			dialog.setFilterPath(path);
			String rc = dialog.open();
			fullPath = rc;
			if (rc != null) {
				String lastSegment = rc.substring(rc.lastIndexOf(Path.SEPARATOR)+1);
				buildPathText.setText(lastSegment);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
		
	}
}
