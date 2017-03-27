/*
 * Copyright 2016-2017 Malcolm Reid Jr.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.continuousassurance.swamp.eclipse.dialogs;

import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.cli.SwampApiWrapper;
import org.continuousassurance.swamp.eclipse.SubmissionInfo;
import org.continuousassurance.swamp.eclipse.Utils;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.make.core.IMakeBuilderInfo;
import org.eclipse.cdt.make.core.MakeBuilder;
import org.eclipse.cdt.make.core.MakeBuilderUtil;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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

import static org.continuousassurance.swamp.eclipse.SubmissionInfo.AUTO_GENERATE_BUILD_STRING;
import static org.continuousassurance.swamp.eclipse.SubmissionInfo.ECLIPSE_GENERATED_STRING;
import static org.continuousassurance.swamp.eclipse.SubmissionInfo.NO_BUILD_STRING;
import static org.eclipse.core.runtime.Path.SEPARATOR;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigDialog extends TitleAreaDialog {

	/* Instance variables representing widgets */
	/**
	 * Text widget for build target (e.g. all)
	 */
	private Text buildTargetText;

	/**
	 * Text widget for project file path
	 */
	private Text prjFilePathText;
	/**
	 * Text widget for SWAMP package version
	 */
	private Text pkgVersionText;
	/**
	 * Text widget for SWAMP package name
	 */
	private Text pkgNameText;
	/**
	 * Combo widget for SWAMP project selection
	 */
	private Combo swampPrjCombo;
	/**
	 * Combo widget for Eclipse project selection
	 */
	private Combo eclipsePrjCombo;
	/**
	 * Combo widget for SWAMP package selection
	 */
	private Combo pkgCombo;
	/**
	 * Combo widget for SWAMP package type selection
	 */
	private Combo pkgTypeCombo;
	/**
	 * Combo widget for build system selection
	 */
	private Combo buildSysCombo;
	/**
	 * Checkbox button widget for whether system libraries should be packaged
	 */
	private Button packageRTButton;
	/**
	 * Button widget for selecting a file
	 */
	private Button selectFileButton;
	/**
	 * Text widget for build path
	 */
	private Text buildPathText;
	/**
	 * Shell that this dialog will be on
	 */
	private Shell shell;
	/**
	 * Child AdvancedSettingsDialog
	 */
	private AdvancedSettingsDialog advancedSettingsDialog;
	
	/* Instance variables representing state */
	/**
	 * List of SWAMP projects available to this user
	 */
	private List<Project> swampProjects;
	/**
	 * List of Eclipse projects open in the workspace (matches what's showing
	 * in project explorer)
	 */
	private IProject[] eclipseProjects;

	/**
	 * List of SWAMP packages available to the user
	 */
	private List<PackageThing> swampPackages;
	/**
	 * Build options for the SWAMP package
	 */
	private String buildOpts;
	/**
	 * Config options for the SWAMP package
	 */
	private String configOpts;
	/**
	 * Relative path to the configuration script
	 */
	private String configScriptPath;
	/**
	 * UUID of the selected SWAMP project
	 */
	private String prjUUID;
	/**
	 * SubmissionInfo object backing this dialog
	 */
	private SubmissionInfo submissionInfo;
	/**
	 * Reference to SwampApiWrapper object
	 */
	private SwampApiWrapper api;
	
	/**
	 * Index of create new package on SWAMP option
	 */
	private static final int CREATE_NEW_PACKAGE_IDX = 0;
	
	/**
	 * Title of Config Dialog
	 */
	private static final String CONFIG_TITLE = "Build Configuration";
	
	/* Help messages */
	/**
	 * Help message for selecting SWAMP project
	 */
	private static final String SWAMP_PROJECT_HELP 				= "Select the SWAMP project that contains the package you want to assess. New projects can only be created using the SWAMP web interface.";
	/**
	 * Help message for selecting SWAMP package
	 */
	private static final String SWAMP_PACKAGE_HELP 				= "Select the SWAMP package you want to assess or select \"Create a new package\".";
	/**
	 * Help message for widget allowing user to specify name of new package
	 */
	private static final String NEW_PACKAGE_HELP 				= "Enter the name of your new package.";
	/**
	 * Help message for widget allowing user to specify package version
	 */
	private static final String PACKAGE_VERSION_HELP			= "Enter the version of your package that you are submitting now.";
	/**
	 * Help message for widget allowing user to specify Package Type
	 */
	private static final String PACKAGE_TYPE_HELP				= "Select the type of the package that you are uploading.";
	/**
	 * Help message for widget allowing user to specify Eclipse project
	 */
	private static final String ECLIPSE_PROJECT_HELP			= "Select the Eclipse project in your workspace to upload to the SWAMP.";
	/**
	 * Help message for widget allowing user to specify build system
	 */
	private static final String BUILD_SYSTEM_HELP				= "Select the build system of your project or select \"Auto-generate build file\".";
	/**
	 * Help message for widget allowing user build target
	 */
	private static final String BUILD_TARGET_HELP				= "Select the build target in the build file.";
	/**
	 * Help message for widget allowing user to select file
	 */
	private static final String SELECT_FILE_HELP				= "Select the build file for this project.";
	/**
	 * Help message for widget giving user option to package system libraries
	 */
	private static final String PACKAGE_SYSTEM_LIBRARIES_HELP 	= "Select this option to upload system libraries (e.g. JDK) to the SWAMP. By default, they will not be uploaded.";
	/**
	 * Help message for widget allowing user to specify advanced settings
	 */
	private static final String ADVANCED_SETTINGS				= "Advanced Settings...";
	/**
	 * Text for new package option
	 */
	private static final String CREATE_NEW_PACKAGE				= "Create new package";

	/* Dialog labels */
	/**
	 * Label for SWAMP Project
	 */
	private static final String SWAMP_PROJECT_LABEL = "SWAMP Project: ";
	/**
	 * Label for SWAMP Package
	 */
	private static final String SWAMP_PACKAGE_LABEL = "SWAMP Package: ";
	/**
	 * Label for New Package Name
	 */
	private static final String NEW_PACKAGE_NAME_LABEL = "New Package Name: ";
	/**
	 * Label for SWAMP Package Version
	 */
	private static final String SWAMP_PACKAGE_VERSION_LABEL = "Package Version: ";
	/**
	 * Label for Eclipse Project
	 */
	private static final String ECLIPSE_PROJECT_LABEL = "Eclipse Project: ";
	/**
	 * Label for Eclipse Project filepath
	 */
	private static final String FILEPATH_LABEL = "Filepath: ";
	/**
	 * Label for SWAMP Package Type
	 */
	private static final String PACKAGE_TYPE_LABEL = "Package Type: ";
	/**
	 * Label for build system
	 */
	private static final String BUILD_SYSTEM_LABEL = "Build System: ";
	/**
	 * Empty string label
	 */
	private static final String EMPTY_LABEL = "";
	/**
	 * Label for Packaging System Libraries 
	 */
	private static final String PACKAGE_SYSTEM_LIBRARIES_LABEL = "Package System Libraries?";
	/**
	 * Label for build file
	 */
	private static final String BUILD_FILE_LABEL = "Build File: ";
	/**
	 * Label for file selection
	 */
	private static final String MORE_OPTIONS_LABEL = "...";
	/**
	 * Label for build target
	 */
	private static final String BUILD_TARGET_LABEL = "Build Target: ";

	/* Build system/package type constants */
	/**
	 * Ant build system
	 */
	private static final String BUILD_SYSTEM_ANT = "ant";
	/**
	 * Maven build system
	 */
	private static final String BUILD_SYSTEM_MAVEN = "Maven";
	/**
	 * Make build system
	 */
	private static final String BUILD_SYSTEM_MAKE = "make";
	/**
	 * Makefile
	 */
	private static final String MAKEFILE = "makefile";
	/**
	 * Java language
	 */
	private static final String JAVA_LANG = "Java";
	/**
	 * Java 7 Src Package Type
	 */
	private static final String JAVA_7_SRC_PKG_TYPE = "Java 7 Source Code";
	/**
	 * Java 8 Src Package Type
	 */
	private static final String JAVA_8_SRC_PKG_TYPE = "Java 8 Source Code";
	/**
	 * ALL option (i.e. Java and C/C++ options)
	 */
	private static final String ALL_OPTION = "All";
	/**
	 * C/C++ language
	 */
	private static final String CCPP_LANG = "C/C++";
	/**
	 * Java compliance version 1.7
	 */
	private static final String JAVA_COMPLIANCE_VERSION_17 = "1.7";
	/**
	 * Java compliance version 1.8
	 */
	private static final String JAVA_COMPLIANCE_VERSION_18 = "1.8";
	/**
	 * Eclipse option for getting Java compiler compliance option for the project
	 */
	private static final String ECLIPSE_JAVA_COMPLIANCE_OPTION = "org.eclipse.jdt.core.compiler.compliance";

	/* Messages displayed to user if they try to advance without completing all necessary fields */
	/**
	 * Message for when user has not selected a SWAMP project
	 */
	private static final String SELECT_SWAMP_PROJECT = "Please select a SWAMP project.";
	/**
	 * Message for when user has not selected a SWAMP package
	 */
	private static final String SELECT_SWAMP_PACKAGE = "Please select a package.";
	/**
	 * Message for when user has chosen to make a new package but has not specified a name for that package
	 */
	private static final String ADD_PACKAGE_NAME = "Please add a name for your new package.";
	/**
	 * Message for when user has not specified package version
	 */
	private static final String ADD_PACKAGE_VERSION = "Please add a descriptor for your package version.";
	/**
	 * Message for when user has not selected a package type
	 */
	private static final String SELECT_PACKAGE_TYPE = "Please select a package type.";
	/**
	 * Message for when user has not selected an Eclipse project
	 */
	private static final String SELECT_ECLIPSE_PROJECT = "Please select an Eclipse project.";
	/**
	 * Message for when user has not selected a build system
	 */
	private static final String SELECT_BUILD_SYSTEM = "Please select a build system.";
	/**
	 * Message for when user has not specified a valid build file
	 */
	private static final String SELECT_BUILD_FILE = "Please select a valid build file.";
	/**
	 * Message for when user has not specified a build target
	 */
	private static final String SELECT_BUILD_TARGET = "Please enter a valid build target.";
	/**
	 * Message for when user has specified configuration options but no configuration script
	 */
	private static final String CONFIG_OPTS_WO_CONFIG_SCRIPT = "Config options set but no configuration script specified.";

	/* Error messages */
	/**
	 * Error message for issue getting build information
	 */
	private static final String PROBLEM_GETTING_BUILD_INFO = "Error: Problem getting build information";
	/**
	 * Error message for makefile not found
	 */
	private static final String UNABLE_TO_FIND_MAKEFILE = "Error: Unable to find makefile for project";

	/**
	 * Enum for type of combo (to enable different behaviors for the different combos)
	 */
	private enum Type {
		PACKAGE_TYPE, ECLIPSE_PROJECT, BUILD, PACKAGE, SWAMP_PROJECT
	}
	
	/**
	 * Constructor for ConfigDialog
	 * @param parentShell
	 * @param si
	 */
	public ConfigDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		submissionInfo = si;
		api = submissionInfo.getApi();
		shell = parentShell;
		buildOpts = "";
		configOpts = "";
		configScriptPath = "";
	}
	
	/**
	 * Resets widgets in ConfigDialog
	 */
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
		pkgCombo.setItems("");
		pkgTypeCombo.setItems("");
		pkgTypeCombo.setEnabled(false);
		buildSysCombo.setItems("");
		buildSysCombo.setEnabled(false);
		packageRTButton.setSelection(false);
		packageRTButton.setEnabled(false);
		setDefaults();	
	}
	
	/**
	 * Sets defaults
	 */
	private void setDefaults() {
		setSwampProjectDefault();
		setEclipseProjectDefault();
		setPackageDefault();
		setBuildSysDefault();
		setPackageTypeDefault();
	}

	/**
	 * Sets default Eclipse project
	 */
	private void setEclipseProjectDefault() {
		if (eclipsePrjCombo.getItemCount() == 1) {
			eclipsePrjCombo.select(0);
			handleEclipseProjectSelection(0);
		}
	}
	
	/**
	 * Sets default SWAMP project
	 */
	private void setSwampProjectDefault() {
		if (swampPrjCombo.getItemCount() == 1) {
			swampPrjCombo.select(0);
			handleSwampProjectSelection(0);
		}
	}
	
	/**
	 * Sets default SWAMP package
	 */
	private void setPackageDefault() {
		if (prjUUID == null) {
			return;
		}
		if (pkgCombo.getItemCount() == 1) {
			pkgCombo.select(0);
			handlePackageSelection(0);
		}
	}
	
	/**
	 * Sets default build system
	 */
	private void setBuildSysDefault() {
		if (buildSysCombo.getItemCount() == 1) {
			buildSysCombo.select(0);
			handleBuildSelection(0);
		}
	}
	
	/**
	 * Sets default package type
	 */
	private void setPackageTypeDefault() {
		if (pkgTypeCombo.getItemCount() == 1) {
			pkgTypeCombo.select(0);
		}
	}
	
	/**
	 * Gets list of elements 
	 * @param type member of Type enum
	 * @return list of elements
	 */
	private String[] getSelectionElements(Type type) {
		if (type == Type.ECLIPSE_PROJECT) { // Eclipse project
			return getEclipseProjectList();
		}
		if (type == Type.SWAMP_PROJECT) {
			return getSwampProjectList();
			
		}
		return getSwampPackageList();
	}
	
	/**
	 * @return list of Eclipse projects
	 */
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
	
	/**
	 * @return list of SWAMP projects
	 */
	private String[] getSwampProjectList() {
		swampProjects = api.getProjectsList();
		int size = swampProjects.size();
		String[] array = new String[size];
		for (int i = 0; i < size; i++) {
			array[i] = swampProjects.get(i).getFullName();
		}
		return array;
	}
	
	/**
	 * @return list of SWAMP packages
	 */
	private String[] getSwampPackageList() {
		if (prjUUID == null) {
			String[] array = {""};
			return array;
		}
		swampPackages = api.getPackagesList(prjUUID);
		int numPackages = swampPackages.size() + 1;
		String[] pkgNames = new String[numPackages];
		pkgNames[0] = CREATE_NEW_PACKAGE;
		for (int i = 1; i < numPackages; i++) {
			pkgNames[i] = swampPackages.get(i-1).getName();
		}
		return pkgNames; 
	}
	

	
	@Override
	/**
	 * Creates UI for ConfigDialog
	 * @param parent the Composite on which the ConfigDialog UI will be placed on
	 * @return composite with the ConfigDialog on it
	 */
	protected Control createDialogArea(Composite parent) {
		System.out.println("Redrawing Config Dialog");
		Composite area = (Composite) super.createDialogArea(parent);
		int horizontalSpan = 2;
		Composite container = new Composite(area, SWT.NONE);
		
		setTitle(CONFIG_TITLE);
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(4, false);
		container.setLayout(layout);
		
		DialogUtil.initializeLabelWidget(SWAMP_PROJECT_LABEL, SWT.NONE, container, horizontalSpan);
		String swampPrjOptions[] = getSelectionElements(Type.SWAMP_PROJECT);
		swampPrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), swampPrjOptions, horizontalSpan);
		swampPrjCombo.addSelectionListener(new ComboSelectionListener(swampPrjCombo, Type.SWAMP_PROJECT));
		swampPrjCombo.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, SWAMP_PROJECT_HELP));
		
		DialogUtil.initializeLabelWidget(SWAMP_PACKAGE_LABEL, SWT.NONE, container, horizontalSpan);
		String pkgOptions[] = getSelectionElements(Type.PACKAGE);
		pkgCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), pkgOptions, horizontalSpan);
		pkgCombo.addSelectionListener(new ComboSelectionListener(pkgCombo, Type.PACKAGE));
		pkgCombo.setEnabled(false);
		pkgCombo.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, SWAMP_PACKAGE_HELP));

		DialogUtil.initializeLabelWidget(NEW_PACKAGE_NAME_LABEL, SWT.NONE, container, horizontalSpan);
		pkgNameText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		pkgNameText.setEnabled(false);
		pkgNameText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, NEW_PACKAGE_HELP));
		
		DialogUtil.initializeLabelWidget(SWAMP_PACKAGE_VERSION_LABEL, SWT.NONE, container, horizontalSpan);
		pkgVersionText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);	
		pkgVersionText.setText(submissionInfo.getPackageVersion());
		pkgVersionText.setEnabled(false);
		pkgVersionText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PACKAGE_VERSION_HELP));
		
		DialogUtil.initializeLabelWidget(ECLIPSE_PROJECT_LABEL, SWT.NONE, container, horizontalSpan);
		String eclipsePrjOptions[] = getSelectionElements(Type.ECLIPSE_PROJECT);
		eclipsePrjCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), eclipsePrjOptions, horizontalSpan);
		eclipsePrjCombo.addSelectionListener(new ComboSelectionListener(eclipsePrjCombo, Type.ECLIPSE_PROJECT));
		eclipsePrjCombo.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, ECLIPSE_PROJECT_HELP));

		DialogUtil.initializeLabelWidget(FILEPATH_LABEL, SWT.NONE, container, horizontalSpan);
		prjFilePathText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		prjFilePathText.setEditable(false);
		DialogUtil.initializeLabelWidget(PACKAGE_TYPE_LABEL, SWT.NONE, container, horizontalSpan);

		//String pkgTypes[] = getSelectionElements(Type.PACKAGE_TYPE);
		pkgTypeCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), new String[0], horizontalSpan);
		pkgTypeCombo.setEnabled(false);
		pkgTypeCombo.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PACKAGE_TYPE_HELP));
		DialogUtil.initializeLabelWidget(BUILD_SYSTEM_LABEL, SWT.NONE, container, horizontalSpan);
		String[] buildSysOptions = getSelectionElements(Type.BUILD);
		buildSysCombo = DialogUtil.initializeComboWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), buildSysOptions, horizontalSpan);		
		buildSysCombo.addSelectionListener(new ComboSelectionListener(buildSysCombo, Type.BUILD));
		buildSysCombo.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, BUILD_SYSTEM_HELP));
		
		DialogUtil.initializeLabelWidget(EMPTY_LABEL, SWT.NONE, container, horizontalSpan);
		packageRTButton = DialogUtil.initializeButtonWidget(container, PACKAGE_SYSTEM_LIBRARIES_LABEL, new GridData(SWT.FILL, SWT.NONE, true, false), SWT.CHECK, horizontalSpan);
		packageRTButton.setEnabled(false);
		packageRTButton.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, PACKAGE_SYSTEM_LIBRARIES_HELP));
		
		DialogUtil.initializeLabelWidget(BUILD_FILE_LABEL, SWT.NONE, container, horizontalSpan);
		buildPathText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), 1);
		buildPathText.setEditable(false);

		selectFileButton = DialogUtil.initializeButtonWidget(container, MORE_OPTIONS_LABEL, new GridData(SWT.FILL, SWT.NONE, false, false), SWT.PUSH, 1);
		selectFileButton.addSelectionListener(new FileSelectionListener());
		selectFileButton.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, SELECT_FILE_HELP));

		DialogUtil.initializeLabelWidget(BUILD_TARGET_LABEL, SWT.NONE, container, horizontalSpan);
		buildTargetText = DialogUtil.initializeTextWidget(SWT.SINGLE | SWT.BORDER, container, new GridData(SWT.FILL, SWT.NONE, true, false), horizontalSpan);
		buildTargetText.addHelpListener(e -> MessageDialog.openInformation(shell, DialogUtil.HELP_DIALOG_TITLE, BUILD_TARGET_HELP));
		
		if (submissionInfo.isConfigInitialized()) {
			setupSwampProject();
			setupSwampPackage();
			setupEclipseProject();
			setupPackageType();
			setupBuild();
		}
		else if (submissionInfo.isProjectInitialized()) {
			setupEclipseProject();
		}
		else {
			setDefaults();
		}
		
		return area;
	}

	/**
	 * Sets various SWAMP package type-related widgets and state for the ConfigDialog
	 */
	private void setupPackageType() {
		// get the appropriate pkgType String, not the index
		String pkgType = submissionInfo.getPackageType();
		for (int i = 0; i < pkgTypeCombo.getItemCount(); i++) {
			if (pkgTypeCombo.getItem(i).equals(pkgType)) {
				pkgTypeCombo.select(i);
				return;
			}
		}
	}
	

	
	/**
	 * Method attempts to predict that Java build system based on project nature's and common build file names
	 * @param project Eclipse Java project
	 */
	private void setPredictedJavaBuildSys(IProject project) {
		String GRADLE_NATURE = "org.eclipse.buildship.core.gradleprojectnature";
		String MAVEN_NATURE = "org.eclipse.m2e.core.maven2Nature";
		String ANT_BUILD = "build.xml"; // this could be ant or ant+Ivy
		String MAVEN_BUILD = "pom.xml";
		String MAKE_UPPERCASE = "Makefile";
		String MAKE_LOWERCASE = "makefile";
		String path = project.getLocation().toOSString();
		IProjectDescription description = null;
		try {
			description = project.getDescription();
		} catch (CoreException e) {
			return;
		}
		if (description != null) {
			String[] natures = description.getNatureIds();
			// (1) nature approach
			for (int i = 0; i < natures.length; i++) {
				String nature = natures[i];
				System.out.println("Nature " + i + ": " + nature);
				if (nature.equals(GRADLE_NATURE)) {
					setBuildSystem(BUILD_SYSTEM_ANT);
					setBuildPath(path, ANT_BUILD);
					return;
				}
				if (nature.equals(MAVEN_NATURE)) {
					setBuildSystem(BUILD_SYSTEM_MAVEN);
					setBuildPath(path, MAVEN_BUILD);
					return;
				}
			}
		}
		// (2) File approach
		File file = new File(path);
		String files[] = file.list();
		
		if (files != null && files.length > 0) {
			for (int i = 0; i < files.length; i++) {
				String filename = files[i];
				System.out.println("Filename: " + filename);
				String filepath = path + SEPARATOR + filename;
				File f = new File(filepath);
				if (f.isDirectory()) {
					continue;
				}
				if (filename.equals(ANT_BUILD)) {
					setBuildSystem(BUILD_SYSTEM_ANT);
					buildPathText.setText(filepath);
					return;
				}
				if (filename.equals(MAVEN_BUILD)) {
					setBuildSystem(BUILD_SYSTEM_MAVEN);
					buildPathText.setText(filepath);
					return;
				}
				if ((filename.equals(MAKE_UPPERCASE)) || (filename.equals(MAKE_LOWERCASE))) {
					setBuildSystem(BUILD_SYSTEM_MAKE);
					buildPathText.setText(filepath);
					return;
				}
			}
		}
		// default to auto-generate
		packageRTButton.setEnabled(true);
		setBuildSystem(AUTO_GENERATE_BUILD_STRING);
	}
	
	/**
	 * Sets path of the build file
	 * @param dirPath directory path
	 * @param filename filename
	 */
	private void setBuildPath(String dirPath, String filename) {
		File file = new File(dirPath);
		String[] fileList = file.list();
		if (fileList != null && fileList.length > 0) {
			for (String f : fileList) {
				if (f.equals(filename)) {
					buildPathText.setText(dirPath + SEPARATOR + filename);
					return;
				}
			}
			buildPathText.setText("");
		}
	}

	/**
	 * Sets build system
	 * @param buildSys
	 */
	private void setBuildSystem(String buildSys) {
		for (int i = 0; i < buildSysCombo.getItemCount(); i++) {
			if (buildSysCombo.getItem(i).equals(buildSys)) {
				buildSysCombo.select(i);
				handleBuildSelection(i);
			}
		}
	}

	/**
	 * Sets various Eclipse project-related widgets and state for the ConfigDialog
	 */
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
	
	/**
	 * Handles Eclipse project being selected
	 * @param index index of the selected Eclipse project
	 */
	private void handleEclipseProjectSelection(int index) {
		if (index < 0) {
			prjFilePathText.setText("");
			pkgTypeCombo.setItems("");
			buildSysCombo.setItems("");
			packageRTButton.setSelection(false);
			packageRTButton.setEnabled(false);
		}
		else {
			pkgTypeCombo.setEnabled(true);
			buildSysCombo.setEnabled(true);
			IProject project = eclipseProjects[index];
			String lang = getProjectLanguage(project);
			if (lang.equals("Java")) {
				IJavaProject jp = JavaCore.create(project);
				// JavaCore.COMPILER_COMPLIANCE
				String complianceVersion = jp.getOption(ECLIPSE_JAVA_COMPLIANCE_OPTION, true);
				if (complianceVersion != null) {
					if (complianceVersion.equals(JAVA_COMPLIANCE_VERSION_17)) {
						System.out.println("Java 7 package");
						// set package type to Java 7
						// This API doesn't really make sense for our purposes
						//setPackageType(api.getPkgTypeString("Java", "java-7", "", null));
						setPkgTypeOptions(JAVA_LANG);
						setPackageType(JAVA_7_SRC_PKG_TYPE);
						setBuildSysOptions(JAVA_LANG);
					}
					else if (complianceVersion.equals(JAVA_COMPLIANCE_VERSION_18)) {
						System.out.println("Java 8 package");
						// set package type to Java 8
						//setPackageType(api.getPkgTypeString("Java", "java-8", "", null));
						setPkgTypeOptions(JAVA_LANG);
						setPackageType(JAVA_8_SRC_PKG_TYPE);
						setBuildSysOptions(JAVA_LANG);
					}
				}
				setPredictedJavaBuildSys(project);
			}
			else if (lang.equals(CCPP_LANG)) {
				packageRTButton.setEnabled(false);
				setPkgTypeOptions(CCPP_LANG);
				setPackageType(CCPP_LANG);
				setBuildSysOptions(CCPP_LANG);
				setBuildSystem(ECLIPSE_GENERATED_STRING);
			}
			else {
				packageRTButton.setEnabled(true);
				setPkgTypeOptions(ALL_OPTION);
				setBuildSysOptions(ALL_OPTION);
			}
			prjFilePathText.setText(project.getLocation().toOSString());
		}
	}
	
	/**
	 * Sets build system options based on language
	 * @param lang "All", "Java", or "C/C++"
	 */
	private void setBuildSysOptions(String lang) {
		String[] buildOptions;
		if (lang.equals(ALL_OPTION)) {
			String[] javaBuildOptions = SubmissionInfo.getBuildSystemList(JAVA_LANG);
			String[] cBuildOptions = SubmissionInfo.getBuildSystemList(CCPP_LANG);
			buildOptions = union(javaBuildOptions, cBuildOptions);
			Arrays.sort(buildOptions);
		}
		else {
			buildOptions = SubmissionInfo.getBuildSystemList(lang);
		}
		buildSysCombo.setItems(buildOptions);
	}
	
	/**
	 * Sets package type options based on language
	 * @param lang "All", "Java", or "C/C++"
	 */
	private void setPkgTypeOptions(String lang) {
		String[] pkgTypes;
		if (lang.equals(ALL_OPTION)) {
			String[] javaPkgTypes = SubmissionInfo.getPackageTypeList(JAVA_LANG);
			String[] cPkgTypes = SubmissionInfo.getPackageTypeList(CCPP_LANG);
			pkgTypes = union(javaPkgTypes, cPkgTypes);
			Arrays.sort(pkgTypes);
		}
		else {
			pkgTypes = SubmissionInfo.getPackageTypeList(lang);
		}
		pkgTypeCombo.setItems(pkgTypes);
	}
	
	/**
	 * Static method that returns the union of two arrays
	 * @param a one array
	 * @param b other array
	 * @return union of the arrays (unsorted)
	 */
	private static String[] union(String[] a, String[] b) {
		Set<String> set = new HashSet<>();
		for (String s : a) {
			set.add(s);
		}
		for (String s : b) {
			set.add(s);
		}
		return set.toArray(new String[0]);
	}

	/**
	 * Gets language for Eclipse project
	 * @param project Eclipse project
	 * @return "C/C++" or "Java"
	 */
	private String getProjectLanguage(IProject project) {
		if (CoreModel.hasCCNature(project) || CoreModel.hasCNature(project)) {
			return CCPP_LANG;
		}
		else {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					return JAVA_LANG;
				}
			} catch (CoreException e) { }
		}
		return "";
	}
	
	/**
	 * Sets package type
	 * @param versionString language version string
	 */
	private void setPackageType(String versionString) {
		// TODO If auto-generated build is chosen, we should enable package RT checkbox
		for (int i = 0; i < pkgTypeCombo.getItemCount(); i++) {
			if (pkgTypeCombo.getItem(i).equals(versionString)) {
				pkgTypeCombo.select(i);
				return;
			}
		}
	}
	
	/**
	 * Sets various SWAMP project-related widgets and state for the ConfigDialog
	 */
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
	
	/**
	 * Handles SWAMP project being selected
	 * @param index index of SWAMP project selected
	 */
	private void handleSwampProjectSelection(int index) {
		if (index < 0) {
			prjUUID = null;
			swampPrjCombo.deselectAll();
			pkgCombo.deselectAll();
			pkgCombo.setEnabled(false);
			pkgVersionText.setEnabled(false);
			pkgCombo.setItems("");
		}
		else {
			prjUUID = swampProjects.get(index).getUUIDString();
			pkgCombo.setEnabled(true);
			pkgVersionText.setEnabled(true);
			pkgCombo.setItems(getSelectionElements(Type.PACKAGE));
			setPackageDefault();
		}
	}
	
	/**
	 * Sets various SWAMP package-related widgets and state for the ConfigDialog
	 */
	private void setupSwampPackage() {
		// get the appropriate UUID from submissionInfo
		// disable the text box
		if (submissionInfo.isNewPackage()) {
			// this only happens if we came back from a later dialog
			pkgCombo.select(CREATE_NEW_PACKAGE_IDX);
			handlePackageSelection(CREATE_NEW_PACKAGE_IDX);
			pkgNameText.setText(submissionInfo.getPackageName());
		}
		else {
			String pkgThingUUID = submissionInfo.getSelectedPackageID();
			for (int i = 0; i < swampPackages.size(); i++) {
				if (swampPackages.get(i).getUUIDString().equals(pkgThingUUID)) {
					pkgCombo.select(i+1);
					handlePackageSelection(i+1);
					return;
				}
			}
			handlePackageSelection(-1);
		}
	}
	
	/**
	 * Handles SWAMP package being selected
	 * @param index index of package selected
	 */
	private void handlePackageSelection(int index) {
		pkgNameText.setText("");
		if (index == CREATE_NEW_PACKAGE_IDX) {
			pkgNameText.setEnabled(true);
		}
		else {
			pkgNameText.setEnabled(false);
		}
	}
	
	/**
	 * Sets various build-related widgets and state for the ConfigDialog
	 */
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
			String prjLocation = eclipseProjects[eclipsePrjCombo.getSelectionIndex()].getLocation().toOSString();
			String buildDir = submissionInfo.getBuildDirectory();
			String buildFile = submissionInfo.getBuildFile();
			String path = prjLocation + SEPARATOR + (buildDir.equals(".") ? buildFile : buildDir + SEPARATOR + buildFile);
			buildPathText.setText(path);
			buildTargetText.setText(submissionInfo.getBuildTarget());
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
	
	/**
	 * Handles build system being selected 
	 * @param index index of build system selected
	 */
	private void handleBuildSelection(int index) {
		String buildSys =  buildSysCombo.getItem(index);
		if (buildSys.equals(AUTO_GENERATE_BUILD_STRING) || buildSys.equals(NO_BUILD_STRING) || buildSys.equals(ECLIPSE_GENERATED_STRING)) {
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
	
	/**
	 * @return build options
	 */
	String getBuildOpts() {
		return buildOpts;
	}
	
	/**
	 * @return config script path
	 */
	String getConfigScriptPath() {
		return configScriptPath;
	}
	
	/**
	 * Gets config options
	 * @return config options
	 */
	String getConfigOpts() {
		return configOpts;
	}
	
	/**
	 * Sets build options
	 * @param opts options
	 */
	void setBuildOpts(String opts) {
		buildOpts = opts;
	}
	
	/**
	 * Sets config script path
	 * @param path
	 */
	void setConfigScriptPath(String path) {
		configScriptPath = path;
	}
	
	/**
	 * Sets config options for the dialog
	 * @param opts config options
	 */
	void setConfigOpts(String opts) {
		configOpts = opts;
	}
	
	@Override
	/**
	 * Places buttons on ConfigDialog's button bar
	 * @param parent Composite on which button bar will be placed
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		
		Button advancedButton = createButton(parent, IDialogConstants.NO_ID, ADVANCED_SETTINGS, false);
		advancedButton.addSelectionListener(new AdvancedButtonSelectionListener(this));
		
		Button clearButton = createButton(parent, IDialogConstants.NO_ID, DialogUtil.CLEAR_CAPTION, false);
		clearButton.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, DialogUtil.OK_CAPTION, true);
		createButton(parent, IDialogConstants.CANCEL_ID, DialogUtil.CANCEL_CAPTION, false);
	}
	

	
	/**
	 * Checks if the specified configuration is valid
	 * @return true if dialog has been filled in properly and adequately
	 */
	private boolean isValid() {
		int swampPrjIndex = swampPrjCombo.getSelectionIndex();
		if (swampPrjIndex < 0) {
			this.setMessage(SELECT_SWAMP_PROJECT);
		}
		
		int pkgIndex = pkgCombo.getSelectionIndex();
		if (pkgIndex < 0) {
			this.setMessage(SELECT_SWAMP_PACKAGE);
			return false;
		}
		
		if (pkgIndex == CREATE_NEW_PACKAGE_IDX) {
			if (pkgNameText.getText().equals("")) {
				this.setMessage(ADD_PACKAGE_NAME);
				return false;
			}
		}
		
		if (pkgVersionText.getText().equals("")) {
			this.setMessage(ADD_PACKAGE_VERSION);
			return false;
		}
		
		int pkgTypeIndex = pkgTypeCombo.getSelectionIndex();
		if (pkgTypeIndex < 0) {
			this.setMessage(SELECT_PACKAGE_TYPE);
			return false;
		}
		
		int eclipsePrjIndex = eclipsePrjCombo.getSelectionIndex();
		if (eclipsePrjIndex < 0) {
			this.setMessage(SELECT_ECLIPSE_PROJECT);
			return false;
		}
		
		int buildIndex = buildSysCombo.getSelectionIndex();
		if (buildIndex < 0) {
			this.setMessage(SELECT_BUILD_SYSTEM);
			return false;
		}
		String buildSysString = buildSysCombo.getItem(buildIndex);
		if (buildSysString.equals(NO_BUILD_STRING) || buildSysString.equals(AUTO_GENERATE_BUILD_STRING) || buildSysString.equals(ECLIPSE_GENERATED_STRING)) {
			return true;
		}
		if (buildPathText.getText().equals("")) { // TODO Add check to make sure build file is within project directory
			this.setMessage(SELECT_BUILD_FILE);
		}
		if (buildTargetText.getText().equals("")) {
			this.setMessage(SELECT_BUILD_TARGET);
			return false;
		}
		if (!configOpts.equals("") && configScriptPath.equals("")) {
			this.setMessage(CONFIG_OPTS_WO_CONFIG_SCRIPT);
			return false;
		}
		return true;
	}
	

	@Override
	/**
	 * Handles OK button being pressed. Does validation checks and either
	 * exits this dialog (if valid) or stays on this dialog
	 */
	protected void okPressed() {
		if (isValid()) {
			// do a lot of setting of submissionInfo
			
			// swamp project (UUID needed)
			submissionInfo.setSelectedProjectID(prjUUID);
			
			// swamp package (UUID if available, otherwise name)
			int index = pkgCombo.getSelectionIndex();
			if (index == CREATE_NEW_PACKAGE_IDX) {
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
			submissionInfo.setPackageType(pkgTypeCombo.getItem(index), false);
			
			// eclipse project (actual IProject)
			index = eclipsePrjCombo.getSelectionIndex();
			IProject project = eclipseProjects[index];
			submissionInfo.setProject(project);
			
			// build system (build system info -- including for create_new...)
			index = buildSysCombo.getSelectionIndex();
			String buildSysStr = buildSysCombo.getItem(index);
			String relBuildDir = "";
			String buildFileName = "";
			boolean createBuildFile = false;
			
			if (buildSysStr.equals(ECLIPSE_GENERATED_STRING)) {
				// TODO Fill this in
				IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
				IPath path = MakeBuilderUtil.getBuildDirectory(project, MakeBuilder.BUILDER_ID);
				IMakeBuilderInfo makeBuildInfo = null;
				try {
					makeBuildInfo = MakeCorePlugin.createBuildInfo(project, MakeBuilder.BUILDER_ID);
				} catch (CoreException e) {
					System.out.println(Utils.getBracketedTimestamp() + PROBLEM_GETTING_BUILD_INFO);
					e.printStackTrace();
					return;
				}
				IFile file = project.getFile(MAKEFILE);
				if (file == null) {
					System.out.println(Utils.getBracketedTimestamp() + UNABLE_TO_FIND_MAKEFILE);
					return;
				}
				String cleanTarget = makeBuildInfo.getCleanBuildTarget();
				String buildTarget = makeBuildInfo.getAutoBuildTarget();
				if (buildTarget == null) {
					buildTarget = makeBuildInfo.getFullBuildTarget();
					if (buildTarget == null) {
						buildTarget = makeBuildInfo.getIncrementalBuildTarget();
					}
					if (buildTarget == null) {
						// TODO Add error reporting
					}
				}
				if (cleanTarget == null) {
					// TODO Add warning
				}
				relBuildDir = getRelDir(project.getLocation().toOSString(), path.toOSString(), false);
				System.out.println("Build system: " + buildSysStr);
				System.out.println("Create build file: " + false);
				System.out.println("Relative build directory: " + relBuildDir);
				System.out.println("Build file name: " + IManagedBuilderMakefileGenerator.MAKEFILE_NAME);
				System.out.println("Build targets: " + cleanTarget + " " + buildTarget);
				System.out.println("Package Run-time libs: " + false);
				submissionInfo.setBuildInfo(buildSysStr, false, relBuildDir, IManagedBuilderMakefileGenerator.MAKEFILE_NAME, cleanTarget + " " + buildTarget, "", false);
				submissionInfo.setConfigInfo("", "", "");
			}
			else {
				if (buildSysStr.equals(AUTO_GENERATE_BUILD_STRING)) {
					createBuildFile = true;
					submissionInfo.setConfigInfo("", "", "");
				}
				else {
					createBuildFile = false;
					String prjDir = project.getLocation().toOSString();
					String buildPath = buildPathText.getText();
					relBuildDir = getRelDir(prjDir, buildPath, true);
					buildFileName = buildPath.substring(buildPath.lastIndexOf(SEPARATOR)+1);
					System.out.println("Relative Directory: " + relBuildDir);
					System.out.println("Build file: " + buildFileName);
					String cDir = "";
					String cCmd = "";
					if (!configScriptPath.equals("")) {
						cDir = getRelDir(prjDir, configScriptPath, true);
						cCmd = configScriptPath.substring(configScriptPath.lastIndexOf(SEPARATOR)+1);
					}
					submissionInfo.setConfigInfo(cDir, cCmd, configOpts);
				}
				submissionInfo.setBuildInfo(buildSysStr, createBuildFile, relBuildDir, buildFileName, buildTargetText.getText(), buildOpts, packageRTButton.getSelection());
			}
			submissionInfo.setConfigInitialized(true);
			super.okPressed();
		}
	}
	
	/**
	 * Gets relative directory
	 * @param projectDir project directory
	 * @param path path
	 * @param isFilePath true if path is for a file rather than a directory
	 * @return relative directory
	 */
	private String getRelDir(String projectDir, String path, boolean isFilePath) {
		int prjIndex = projectDir.lastIndexOf(SEPARATOR);
		int fileIndex = isFilePath ? path.lastIndexOf(SEPARATOR) : path.length();
		String relDir = path.substring(prjIndex+1, fileIndex);
		if (relDir.equals("")) {
			return ".";
		}
		return relDir;
	}
	
	/**
	 * Listener for combo widgets. When a selection is made in a combo widget
	 * it may affect one or more widgets elsewhere in the project
	 * @author reid-jr
	 *
	 */
	private class ComboSelectionListener implements SelectionListener {
		Combo combo;
		Type type;
		public ComboSelectionListener(Combo c, Type t) {
			combo = c;
			type = t;
		}
		
		@Override
		/**
		 * Handles selection in a combo widget
		 * @param e combo element selected
		 */
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
	
	/**
	 * Listener for advanced settings button. Clicking button launches new dialog
	 * @author reid-jr
	 *
	 */
	private class AdvancedButtonSelectionListener implements SelectionListener {
		private ConfigDialog cd;
		public AdvancedButtonSelectionListener(ConfigDialog cd) {
			this.cd = cd;
		}
		
		@Override
		/**
		 * Launches AdvancedSettingsDialog
		 * @param e button selection event
		 */
		public void widgetSelected(SelectionEvent e) {
			advancedSettingsDialog = new AdvancedSettingsDialog(shell, cd);
			advancedSettingsDialog.create();
			advancedSettingsDialog.open();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) { 
		}
	}
	
	/**
	 * Listener for clear button. Clicking "Clear" button clears fields in dialog	
	 * @author reid-jr
	 *
	 */
	private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		/**
		 * @param e "Clear" button selection event
		 */
		public void widgetSelected(SelectionEvent e) {
			resetWidgets();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
	
	/**
	 * Listener for button to open file selection dialog
	 * @author reid-jr
	 *
	 */
	private class FileSelectionListener implements SelectionListener {
		public FileSelectionListener() {
		}
		
		@Override
		/**
		 * @param e file selection event
		 * Handles file selection in file selection dialog
		 */
		public void widgetSelected(SelectionEvent e) {
			FileDialog dialog = new FileDialog(shell);
			String path = prjFilePathText.getText();
			if (path.equals("")) {
				path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
			}
			dialog.setFilterPath(path);
			String rc = dialog.open();
			if (rc != null) {
				buildPathText.setText(rc);
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
	}
}
