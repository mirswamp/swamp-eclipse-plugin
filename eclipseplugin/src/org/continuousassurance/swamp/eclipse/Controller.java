/*
 * Copyright 2017 Malcolm Reid Jr.
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
package org.continuousassurance.swamp.eclipse;

import static org.continuousassurance.swamp.eclipse.Activator.PLUGIN_ID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.continuousassurance.swamp.eclipse.handlers.HandlerUtilityMethods;
import org.continuousassurance.swamp.eclipse.ui.DetailView;
import org.continuousassurance.swamp.eclipse.ui.StatusView;
import org.continuousassurance.swamp.eclipse.ui.SwampPerspective;
import org.continuousassurance.swamp.eclipse.ui.TableView;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import dataStructures.BugInstance;
import dataStructures.Location;

public class Controller {
	
	/**
	 * Key for storing IMarker object with row (TableItem)
	 */
	public static final String MARKER_OBJ = "marker";
	
	/**
	 * Key for storing BugDetail object with row (TableItem)
	 */
	public static final String BUG_DETAIL_OBJ = "bugdetail";
	
	private static final String HIGH_SEVERITY = "eclipseplugin.highseverity";
	
	private static final String MED_SEVERITY = "eclipseplugin.medseverity";
	
	private static final String LOW_SEVERITY = "eclipseplugin.lowseverity";
	
	private static final String UNKNOWN_SEVERITY = "eclipseplugin.unknownseverity";
	
	private static final String[] MARKER_TYPES = {HIGH_SEVERITY, MED_SEVERITY, LOW_SEVERITY, UNKNOWN_SEVERITY}; 
	
	//private IProject currentProject; // TODO Project caching

	public static IViewPart getView(IWorkbenchWindow window, String viewID) {
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		return getView(page, viewID);
	}
	
	public static IViewPart getView(IWorkbenchPage page, String viewID) {
		IViewReference[] refs = page.getViewReferences();
		for (IViewReference ref : refs) {
			if (ref.getId().equals(viewID)) {
				IViewPart view = ref.getView(false);
				return view;
			}
		}
		return null;
	}
	
	public static IEditorPart getEditor(IWorkbenchWindow window) {
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		return getEditor(page);
	}
	
	public static IEditorPart getEditor(IWorkbenchPage page) {
		return page.getActiveEditor();
	}
	
	public static boolean swampPerspectiveOpen(IWorkbenchPage page) {
		IPerspectiveDescriptor pd = page.getPerspective();
		if (pd == null) {
			return false;
		}
		return pd.getId().equals(SwampPerspective.ID);
	}
	
	// TODO: New SCARF file downloaded
	public void refreshWorkspace() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb == null) {
			return;
		}
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IWorkbenchPage page = window.getActivePage();
		if ((page == null) || (!swampPerspectiveOpen(page))) {
			return;
		}
		resetPerspectiveParts(window, page);
		IProject project = HandlerUtilityMethods.getActiveProject(window);
		if (project == null) {
			System.out.println("Project null");
			return;
		}

		System.out.println("Refreshing workspace with project " + project.getName());
		String path = project.getProject().getWorkingLocation(PLUGIN_ID).toOSString() + Path.SEPARATOR + ResultsUtils.ECLIPSE_TO_SWAMP_FILENAME;
		
		File f = new File(path); 
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
					updateEditorAndViews(file, page, bugs, rp.getToolName(), rp.getPlatformName());
				}
			}
		}
	}
	
	private void updateEditorAndViews(IFile file, IWorkbenchPage page, List<BugInstance> bugs, String toolName, String platformName) {
		TableView view = (TableView) getView(page, TableView.ID);
		Table table = null;
		if (view != null) {
			table = view.getTable();
		}
		System.out.println("Is table null? " + (table == null));
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
						item.setText(1, Integer.toString(loc.getStartLine()));
						item.setText(2, Integer.toString(loc.getEndLine()));
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
	
	
	private IMarker createMarkerForResource(IFile resource, BugInstance bug, String toolName) {
		for (Location l : bug.getLocations()) {
			if (l.isPrimary()) {
				System.out.println("Primary bug!");
				System.out.println(bug);
				try {
					//IMarker marker = resource.createMarker(IMarker.PROBLEM);
					//IMarker marker = resource.createMarker("highseverity");
					IMarker marker;
					String severity = bug.getBugSeverity();
					severity = severity == null ? "" : severity.toUpperCase();
					if (severity.equals("HIGH")) {
						marker = resource.createMarker(HIGH_SEVERITY);
					}
					else if (severity.equals("MED")) {
						marker = resource.createMarker(MED_SEVERITY);
					}
					else if (severity.equals("LOW")) {
						marker = resource.createMarker(LOW_SEVERITY);
					}
					else {
						marker = resource.createMarker(UNKNOWN_SEVERITY);
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
			else {
				System.out.println("Non-primary bug!");
			}
			// TODO Maybe add some marker for non-primary bugs? This would probably get too noisy
		}
		return null;
	}
	
	private void resetPerspectiveParts(IWorkbenchWindow window, IWorkbenchPage page) {
		resetFileMarkers(window);
		resetTableView(page);
		resetDetailView(page);
	}
	
	private void resetTableView(IWorkbenchPage page) {
		TableView view = (TableView) getView(page, TableView.ID);
		if (view != null) {
			view.resetTable();
		}
	}
	
	private void resetDetailView(IWorkbenchPage page) {
		DetailView view = (DetailView) getView(page, DetailView.ID);
		if (view != null) {
			view.reset();
		}
	}
	
	public static String[] getMarkerTypes() {
		return MARKER_TYPES;
	}
	
	private void resetFileMarkers(IWorkbenchWindow window) {
		System.out.println("Attempting to reset file markers");
		IFile file = HandlerUtilityMethods.getActiveFile(window);
		if (file != null) {
			System.out.println("Removing file markers for file " + file.getName());
			try {
				for (String markerType : getMarkerTypes())
				file.deleteMarkers(markerType, true, 1);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("File is null");
		}
	}
	
	// Row selected in TableView
	public void updateDetailView(BugDetail bug) {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
			if (window != null) {
				DetailView view = (DetailView) getView(window, DetailView.ID);
				if (view != null) {
					view.update(bug);
				}
			}
		}
	}
	
	private IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb == null) {
			return null;
		}
		return wb.getActiveWorkbenchWindow();
	}
	
	public void jumpToLocation(IMarker marker) {
		if (marker == null || (marker.getAttribute(IMarker.LINE_NUMBER, 0) == 0)) {
			return;
		}
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			IEditorPart editor = getEditor(window);
			if (editor != null) {
				IDE.gotoMarker(editor, marker);
			}
		}
	}
	
	// TODO: update status dashboard
	public void updateStatusView(List<String> statuses) {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			StatusView view = (StatusView) getView(window, StatusView.ID);
			if (view != null) {
				view.addRowsToStatusTable(statuses);
			}
		}
	}
	
}
