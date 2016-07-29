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

import eclipseplugin.SubmissionInfo;
import eclipseplugin.Utils;
import edu.uiuc.ncsa.swamp.api.PackageThing;
import edu.uiuc.ncsa.swamp.api.PackageVersion;
import edu.uiuc.ncsa.swamp.api.Platform;
import edu.wisc.cs.swamp.SwampApiWrapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigDialog extends TitleAreaDialog {

	/* Instance variables representing widgets */
	private Text buildDirText;
	private Text buildFileText;
	private Text buildTargetText;
	private Text prjFilePathText;
	private Text prjVersionText;
	private Combo eclipsePrjCombo;
	private Combo pkgCombo;
	private Combo pkgTypeCombo;
	private Text pkgNameText;
	private Combo buildSysCombo;
	
	private SubmissionInfo submissionInfo;
	
	private static int CREATE_NEW_PACKAGE = 0;
	
	
	private enum Type {
		PACKAGE_TYPE, ECLIPSE_PROJECT, BUILD, PACKAGE, SWAMP_PROJECT
	}
	
	public ConfigDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		submissionInfo = si;
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
		eclipsePrjCombo.deselectAll();
		pkgCombo.deselectAll();
		pkgTypeCombo.deselectAll();
		buildSysCombo.deselectAll();
		setDefaults();	
	}
	
	private void setDefaults() {
		setEclipseProjectDefault();
		setSwampProjectDefault();
		setPackageDefault();
		setBuildSysDefault();
		setPackageTypeDefault();
	}

	private void setEclipseProjectDefault() {
		if (eclipsePrjCombo.getItemCount() == 1) {
			eclipsePrjCombo.select(0);
			submissionInfo.setSelectedProjectIndex(0);
			handleProjectSelection();
		}
	}
	
	private void setSwampProjectDefault() {
		if (swampPrjCombo.getItemCount() == 1) {
			swampPrjCombo.select(0);
			submissionInfo.setSelectedProjectIndex(0);
		}
	}
	
	private void setPackageDefault() {
		if (pkgCombo.getItemCount() == 1) {
			pkgCombo.select(0);
			submissionInfo.setSelectedPackageIndex(0);
			handlePackageSelection();
		}
	}
	
	private void setBuildSysDefault() {
		if (buildSysCombo.getItemCount() == 1) {
			buildSysCombo.select(0);
			submissionInfo.setSelectedBuildSysIndex(0);
			handleBuildSelection();
		}
	}
	
	private void setPackageTypeDefault() {
		if (pkgTypeCombo.getItemCount() == 1) {
			pkgTypeCombo.select(0);
			submissionInfo.setSelectedPackageTypeIndex(0);
		}
	}
	
	private void handleBuildSelection() {
		if (submissionInfo.noBuild() || submissionInfo.generateBuild()) {
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
	
	private void handlePackageSelection() {
		pkgNameText.setText("");
		pkgNameText.setEnabled(submissionInfo.isNewPackage());
	}
	
	private void handleProjectSelection() {
		prjFilePathText.setText(submissionInfo.getProjectPath());
		prjFilePathText.setEnabled(false);	
	}
	
	private String[] getSelectionElements(Type type) {
		if (type == Type.PROJECT) { // Eclipse project
			return submissionInfo.getEclipseProjectList();
		}
		if (type == Type.BUILD) {
			return submissionInfo.getBuildSystemList();
		}
		if (type == Type.PACKAGE) {
			// SWAMP Package
			return submissionInfo.getSwampPackageList();
		}
		// Package Type
		return submissionInfo.getPackageTypeList();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle("Build Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget("SWAMP Project: ", SWT.NONE, container);
		String prjOptions[] = getSelectionElements(Type.PROJECT);
		eclipsePrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), prjOptions);
		eclipsePrjCombo.addSelectionListener(new ComboSelectionListener(eclipsePrjCombo, Type.PROJECT));
		
		DialogUtil.initializeLabelWidget("Eclipse Project: ", SWT.NONE, container);
		String prjOptions[] = getSelectionElements(Type.PROJECT);
		eclipsePrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), prjOptions);
		eclipsePrjCombo.addSelectionListener(new ComboSelectionListener(eclipsePrjCombo, Type.PROJECT));
		
		DialogUtil.initializeLabelWidget("Eclipse Project: ", SWT.NONE, container);
		String prjOptions[] = getSelectionElements(Type.PROJECT);
		eclipsePrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), prjOptions);
		eclipsePrjCombo.addSelectionListener(new ComboSelectionListener(eclipsePrjCombo, Type.PROJECT));
		
		DialogUtil.initializeLabelWidget("SWAMP Package: ", SWT.NONE, container);
		String pkgOptions[] = getSelectionElements(Type.PACKAGE);
		pkgCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), pkgOptions);
		pkgCombo.addSelectionListener(new ComboSelectionListener(pkgCombo, Type.PACKAGE));
		
		DialogUtil.initializeLabelWidget("New Package Name: ", SWT.NONE, container);
		pkgNameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		
		DialogUtil.initializeLabelWidget("Package Version: ", SWT.NONE, container);
		prjVersionText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));	
		prjVersionText.setText(submissionInfo.getPackageVersion());

		DialogUtil.initializeLabelWidget("Filepath: ", SWT.NONE, container);
		prjFilePathText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		
	
		DialogUtil.initializeLabelWidget("Build System: ", SWT.NONE, container);
		String[] buildSysOptions = getSelectionElements(Type.BUILD);
		buildSysCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), buildSysOptions);		
		buildSysCombo.addSelectionListener(new ComboSelectionListener(buildSysCombo, Type.BUILD));
		
		DialogUtil.initializeLabelWidget("Build Directory: ", SWT.NONE, container);
		buildDirText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		buildDirText.setText(".");

		DialogUtil.initializeLabelWidget("Build File: ", SWT.NONE, container);
		buildFileText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));

		DialogUtil.initializeLabelWidget("Build Target: ", SWT.NONE, container);
		buildTargetText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false));
		
		if (submissionInfo.isConfigInitialized()) {
			setupPackageType();
			setupProject();
			setupPackage();
			setupBuild();
		}
		else {
			setDefaults();
		}
		
		return area;
	}
	
	private void setupPackageType() {
		pkgTypeCombo.select(submissionInfo.getSelectedPackageTypeIndex());
		handlePackageTypeSelection();
	}
	
	private void setupProject() {
		eclipsePrjCombo.select(submissionInfo.getSelectedProjectIndex());
		handleProjectSelection();
	}
	
	private void setupPackage() {
		pkgCombo.select(submissionInfo.getSelectedPackageIndex());
		handlePackageSelection();
	}
	
	private void setupBuild() {
		buildSysCombo.select(submissionInfo.getSelectedBuildSysIndex());
		handleBuildSelection();
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
		int prjIndex = eclipsePrjCombo.getSelectionIndex();
		if (prjIndex < 0) {
			this.setMessage("Please select an Eclipse project");
			return false;
		}
		
		int pkgIndex = pkgCombo.getSelectionIndex();
		submissionInfo.setSelectedPackageIndex(pkgIndex);
		if (pkgIndex < 0) {
			this.setMessage("Please select a package");
			return false;
		}
		
		if (submissionInfo.isNewPackage()) {
			if (pkgNameText.equals("")) {
				this.setMessage("Please add a name for your new package");
				return false;
			}
		}
		
		int buildIndex = buildSysCombo.getSelectionIndex();
		if (buildIndex < 0) {
			this.setMessage("Please select a build system");
			return false;
		}
		if (submissionInfo.noBuild() || submissionInfo.generateBuild()) {
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
		if (isValid()) {
			if (submissionInfo.isNewPackage()) {
				submissionInfo.setPackageName(pkgNameText.getText());
			}
			submissionInfo.setBuildInfo(buildDirText.getText(), buildFileText.getText(), buildTargetText.getText());
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
				submissionInfo.setSelectedBuildSysIndex(selection);
				handleBuildSelection();
			}
			else if (type == Type.PROJECT) {
				submissionInfo.setSelectedProjectIndex(selection);
				handleProjectSelection();
			}
			else { // type == Type.PACKAGE
				submissionInfo.setSelectedPackageIndex(selection);
				handlePackageSelection();
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
}
