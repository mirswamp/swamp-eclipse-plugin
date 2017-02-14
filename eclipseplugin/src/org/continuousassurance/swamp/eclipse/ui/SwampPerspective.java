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

package org.continuousassurance.swamp.eclipse.ui;

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.continuousassurance.swamp.eclipse.BugDetail;
import org.continuousassurance.swamp.eclipse.ResultsParser;
import org.continuousassurance.swamp.eclipse.ResultsUtils;
import org.continuousassurance.swamp.eclipse.handlers.HandlerUtilityMethods;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import dataStructures.BugInstance;
import dataStructures.Location;

/**
 * Class for implementing SWAMP perspective, which is composed of a Project
 * Explorer view, a custom TableView (shows a list of bugs), and a custom 
 * DetailView (shows a single bug in detail)
 * @author reid-jr
 *
 */
public class SwampPerspective implements IPerspectiveFactory {

	/**
	 * Descriptor for the table view 
	 */
	public static final String TABLE_VIEW_DESCRIPTOR = "org.continuousassurance.swamp.eclipse.ui.views.tableview";
	/**
	 * Descriptor for the detail view
	 */
	public static final String DETAIL_VIEW_DESCRIPTOR = "org.continuousassurance.swamp.eclipse.ui.views.detailview";
	/**
	 * Cached reference to the active window
	 */
	public static IWorkbenchWindow window;
	
	/**
	 * Key for storing IMarker object with row (TableItem)
	 */
	public static final String MARKER_OBJ = "marker";
	
	/**
	 * Key for storing BugDetail object with row (TableItem)
	 */
	public static final String BUG_DETAIL_OBJ = "bugdetail";
	
	/**
	 * Constructor for SwampPerspective
	 */
	public SwampPerspective() {
		super();
		IWorkbench wb = PlatformUI.getWorkbench();
		window = wb.getActiveWorkbenchWindow();
	}
	
	@Override
	/**
	 * Creates layout of the perspective
	 * @param layout page layout (see Java-doc for more details on what an
	 * 		  IPageLayout is)
	 */
	public void createInitialLayout(IPageLayout layout) {
		defineLayout(layout);
	}
	
	/**
	 * Places the views in their proper locations and sizes them appropriately
	 * @param layout page layout
	 */
	public void defineLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.70f, editorArea);
		bottom.addView(TABLE_VIEW_DESCRIPTOR);
		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.60f, editorArea);
		right.addView(DETAIL_VIEW_DESCRIPTOR);
		IPartService service = window.getPartService();
		service.addPartListener(new FileChangeListener());
	}
	
	/**
	 * Takes a given file and adds bug markers to the left hand side ruler of the file
	 * @param file file to add markers to
	 * @param bugs list of bugs
	 * @param toolName name of the tool
	 */
	/*
	public void annotateEditor(IFile file, List<BugInstance> bugs, String toolName) {
		
		try {
			for (BugInstance bug : bugs) {
				createMarkerForResource(file, bug, toolName);
			}
		}
		catch (Exception e) {
			System.out.println("Failed to create marker");
			e.printStackTrace();
		}
	}
	*/
	
	/**
	 * Adds a single bug marker to the specified file
	 * @param resource file that marker will be added to
	 * @param bug bug that we are adding a marker for
	 * @param toolName name of tool
	 * @return IMarker object
	 */
	public IMarker createMarkerForResource(IFile resource, BugInstance bug, String toolName) {
		for (Location l : bug.getLocations()) {
			if (l.isPrimary()) {
				try {
					//IMarker marker = resource.createMarker(IMarker.PROBLEM);
					//IMarker marker = resource.createMarker("highseverity");
					IMarker marker;
					String severity = bug.getBugSeverity();
					severity = severity == null ? "" : severity.toUpperCase();
					if (severity.equals("HIGH")) {
						marker = resource.createMarker("eclipseplugin.highseverity");
					}
					else if (severity.equals("MED")) {
						marker = resource.createMarker("eclipseplugin.medseverity");
					}
					else if (severity.equals("LOW")) {
						marker = resource.createMarker("eclipseplugin.lowseverity");
					}
					else {
						marker = resource.createMarker("eclipseplugin.unknownseverity");
					}
					marker.setAttribute(IMarker.MESSAGE, toolName + ": " + bug.getBugMessage());
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING); // TODO Get priority right here
					marker.setAttribute(IMarker.LINE_NUMBER, l.getStartLine()); // TODO End line also / lines between also?
					return marker;
				}
				catch (CoreException e) {
					System.err.println("Core exception when creating marker");
					e.printStackTrace();
				}
			}
			// TODO Maybe add some marker for non-primary bugs? This would probably get too noisy
		}
		return null;
	}
	
	/**
	 * Listener for file open in editor being changed.
	 * @author reid-jr
	 *
	 */
	public class FileChangeListener implements IPartListener2 {

		/* We need to set the rows in the TableView from here. We need to store
		 * markers and BugDetail objects with rows in order to have the desired
		 * behavior (i.e. for us to jump to a specific marker and to have all
		 * the info for a bug when a row is clicked). We have to break
		 * modularity a bit in doing this since we need to set the state of the
		 * TableView from here. Doing it here in the SwampPerspective class
		 * makes the most sense as we can update the editor from here as well.
		 */
		
		private IProject currentlyOpenedProject;
		
		private void updatePerspective(IProject project) {
			
			if (project == null) {
				System.out.println("Project null");
				return;
			}
			if (project.equals(currentlyOpenedProject)) {
				System.out.println("Project is the same!");
				return;
			}
			currentlyOpenedProject = project;
			
			System.out.println("Switching projects!");
			System.out.println("Project open: " + project.getName());
			
			String path = project.getProject().getWorkingLocation(PLUGIN_ID).toOSString() + Path.SEPARATOR + ResultsUtils.ECLIPSE_TO_SWAMP_FILENAME;
			
			File f = new File(path); // TODO Change the filepath used here
			if (f.exists()) {
				String pkgThingUUID = "";
				try {
					FileReader filereader = new FileReader(f);
					BufferedReader reader = new BufferedReader(filereader);
					pkgThingUUID = reader.readLine();
				}
				catch (Exception e) {
					System.err.println("Exception occured");
					return;
				}
				
				File resultsDir = new File(ResultsUtils.constructFilepath(pkgThingUUID));
				File[] files = resultsDir.listFiles();
				if (files != null && files.length > 0) {
					IFile file = HandlerUtilityMethods.getActiveFile(window);
					List<BugInstance> bugs;
					ResultsParser rp;
					String filepath;
					for (File r : files) {
						bugs = new ArrayList<>();
						rp = new ResultsParser(r);
						filepath = file.getFullPath().toString();
						filepath = filepath.substring(1);
						bugs.addAll(rp.getFileBugs(filepath));
						updateEditorAndViews(file, bugs, rp.getToolName(), rp.getPlatformName());
					}
				}
			}
			else {
				System.out.println("No results found");
				IWorkbenchPage page = window.getActivePage();
				resetTableView(page);
				resetDetailView(page);
			}
		}

		@Override
		/**
		 * Parses SCARF and updates views as appropriate when file is changed
		 * @param part reference to the workbench part
		 */
		public void partActivated(IWorkbenchPartReference part) {
			System.out.println("Part Activated");
			IProject project = HandlerUtilityMethods.getActiveProject(window);
			updatePerspective(project);
		}
		
		/**
		 * Resets DetailView so it no longer shows info on a bug
		 * @param page reference to workbench page
		 */
		private void resetDetailView(IWorkbenchPage page) {
			DetailView view = (DetailView) page.findView(DETAIL_VIEW_DESCRIPTOR);
			if (view != null) {
				view.update(null);
			}
		}
		
		/**
		 * Resets table view so it no longer has any bugs/rows
		 * @param page reference to workbench page
		 */
		private void resetTableView(IWorkbenchPage page) {
			TableView view = (TableView) page.findView(TABLE_VIEW_DESCRIPTOR);
			Table table = null;
			if (view != null) {
				table = view.getTable();
			}
			if (table != null) {
				table.removeAll();
			}
		}
		
		/**
		 * Updates editor (with markers) and TableView (with rows). Also, creates
		 * BugDetail objects and adds them to the rows as appropriate
		 * @param file file currently active in editor
		 * @param bugs list of bugs found in that file
		 * @param toolName name of tool
		 * @param platformName name of platform
		 */
		private void updateEditorAndViews(IFile file, List<BugInstance> bugs, String toolName, String platformName) {
			Table table = null;
			IWorkbenchPage page = window.getActivePage();
			TableView view = (TableView) page.findView(TABLE_VIEW_DESCRIPTOR);
			if (view != null) {
				table = view.getTable();
			}
			if (table != null) {
				table.removeAll();
			}
			List<TableItem> rows = new ArrayList<>();
			for (BugInstance bug : bugs) {
				IMarker marker = createMarkerForResource(file, bug, toolName);
				if (table != null) {
					BugDetail details = new BugDetail(bug, toolName, platformName);
					TableItem item = new TableItem(table, SWT.NONE);
					for (Location loc : bug.getLocations()) {
						if (loc.isPrimary()) {
							String filename = loc.getSourceFile();
							details.setPrimaryFilename(filename);
							details.setPrimaryLineNumber(loc);
							item.setText(0, filename);
							item.setText(1, BugDetail.formatSingleLineNumber(loc.getStartLine()));
							item.setText(2, BugDetail.formatSingleLineNumber(loc.getEndLine()));
							item.setText(3, bug.getBugCode());
							item.setText(4, toolName);
							item.setText(5, platformName);
						}
						details.addLocation(loc);
					}
					item.setData(MARKER_OBJ, marker);
					item.setData(BUG_DETAIL_OBJ, details);
					rows.add(item);
				}
			}
			resetDetailView(page);
		}
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference arg0) {
			System.out.println("Part brought to the top #allthewayup");
			// TODO Auto-generated method stub
		}

		@Override
		public void partClosed(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void partHidden(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference part) {
			System.out.println("Part Input Changed");
			printPartInfo(part);
		}

		@Override
		public void partOpened(IWorkbenchPartReference part) {
			System.out.println("Part Activated");
			IProject project = HandlerUtilityMethods.getActiveProject(window);
			updatePerspective(project);
		}

		@Override
		public void partVisible(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
		}
		
		/**
		 * Helpful method for debugging what's happening with the listener
		 * @param part reference to workbench part
		 */
		private void printPartInfo(IWorkbenchPartReference part) {
			System.out.println("Part Opened");
			System.out.println("Part class: " + part.getClass());
			System.out.println("Part ID: " + part.getId());
			System.out.println("Part title: " + part.getTitle());
			System.out.println("Part Content description: " + part.getContentDescription());
			System.out.println("Part name: " + part.getPartName());
		}
		

		
	}
	
}
