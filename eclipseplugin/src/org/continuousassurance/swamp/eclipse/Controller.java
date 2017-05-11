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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.continuousassurance.swamp.eclipse.handlers.HandlerUtilityMethods;
import org.continuousassurance.swamp.eclipse.ui.DetailView;
import org.continuousassurance.swamp.eclipse.ui.SortListener;
import org.continuousassurance.swamp.eclipse.ui.StatusView;
import org.continuousassurance.swamp.eclipse.ui.SwampPerspective;
import org.continuousassurance.swamp.eclipse.ui.TableView;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.continuousassurance.scarf.datastructures.BugInstance;
import org.continuousassurance.scarf.datastructures.Location;

/**
 * This class provides the back-end communication for the views and editors
 * in the SWAMP perspective
 * @author reid-jr
 *
 */
public class Controller {
	
	/**
	 * Key for storing IMarker object with row (TableItem)
	 */
	public static final String MARKER_OBJ = "marker";
	
	/**
	 * Key for storing BugDetail object with row (TableItem)
	 */
	public static final String BUG_DETAIL_OBJ = "bugdetail";
	
	public static boolean showAll = true;
	
	/**
	 * Utility method for getting a view in the window given its ID
	 * @param window currently opened window
	 * @param viewID ID of the view
	 * @return view as IViewPart
	 */
	public static IViewPart getView(IWorkbenchWindow window, String viewID) {
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		return getView(page, viewID);
	}
	
	/**
	 * Utility method for getting a view in the page given its ID
	 * @param page the currently active page
	 * @param viewID ID of the view
	 * @return view as IViewPart
	 */
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
	
	public static void toggleShowAll() {
		showAll = !showAll;
	}
	
	/**
	 * Utility method for getting editor from window
	 * @param window currently opened window
	 * @return editor as IEditorPart
	 */
	public static IEditorPart getEditor(IWorkbenchWindow window) {
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		return getEditor(page);
	}
	
	/**
	 * Utility method for getting editor from page
	 * @param page currently active page
	 * @return editor as IEditorPart
	 */
	public static IEditorPart getEditor(IWorkbenchPage page) {
		return page.getActiveEditor();
	}
	
	/**
	 * Checks whether the SWAMP perspective is open
	 * @param page currently active page
	 * @return true if SWAMP perspective is open
	 */
	public static boolean swampPerspectiveOpen(IWorkbenchPage page) {
		IPerspectiveDescriptor pd = page.getPerspective();
		if (pd == null) {
			return false;
		}
		return pd.getId().equals(SwampPerspective.ID);
	}
	
	/**
	 * This method refreshes the workspace. This is called after all sorts of
	 * important events that might affect the views and editors and the SWAMP
	 * perspective occur
	 */
	public static void refreshWorkspace() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb == null) {
			return;
		}
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
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
			InputStreamReader filereader = null;
			BufferedReader reader = null;
			try {
				filereader = new InputStreamReader(new FileInputStream(f), Activator.ENCODING);
				reader = new BufferedReader(filereader);
				pkgThingUUID = reader.readLine();
				reader.close();
			}
			catch (Exception e) {
				System.err.println("Exception occured");
				return;
			}
		
			File resultsDir = new File(ResultsUtils.constructFilepath(pkgThingUUID));
			File[] files = resultsDir.listFiles();
			if (files != null && files.length > 0) {
				List<BugInstance> bugs;
				ResultsParser rp;
				for (File r : files) {
					bugs = new ArrayList<>();
					rp = new ResultsParser(r);
					;
					if (showAll) {
						bugs.addAll(rp.getAllBugs());
					}
					else {
						IFile file = HandlerUtilityMethods.getActiveFile(window);
						String filepath = eclipseToSCARFFilepath(file);
						bugs.addAll(rp.getFileBugs(filepath));
					}
					updateEditorAndViews(page, bugs, rp.getToolName(), rp.getPlatformName());
				}
				Table table = getTable(page);
				if (table != null) {
					TableColumn col = table.getSortColumn();
					if (col != null) {
						SortListener.sortByCol(col);
					}
				}
			}
		}
	}
	
	private static String eclipseToSCARFFilepath(IFile file) {
		String filepath = file.getFullPath().toString(); 
		return filepath.substring(1);
	}
	
	private static IFile SCARFtoEclipseFile(String filepath) {
		Path path = new Path(filepath);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	}
	
	private static Table getTable(IWorkbenchPage page) {
		if (page == null) {
			return null;
		}
		TableView view = (TableView) getView(page, TableView.ID);
		if (view == null) {
			return null;
		}
		return view.getTable();
	}
	
	/**
	 * This method updates the editor and views in the SWAMP perspective
	 * @param file currently active file
	 * @param page currently active page
	 * @param bugs BugInstances for the currently opened project
	 * @param toolName name of the tool that found these bugs
	 * @param platformName name of the platform on which the assessment that
	 * found these bugs was run
	 */
	private static void updateEditorAndViews(IWorkbenchPage page, List<BugInstance> bugs, String toolName, String platformName) {
		Table table = getTable(page);
		System.out.println("Is table null? " + (table == null));
		List<TableItem> rows = new ArrayList<>();
		Map<String, IFile> filePathToEclipseFile = new HashMap<>();
		for (BugInstance bug : bugs) {
			BugDetail details = new BugDetail(bug, toolName, platformName);
			TableItem item = null;
			if (table != null) {
				item = new TableItem(table, SWT.NONE);
			}
			for (Location loc : bug.getLocations()) {
				if (loc.isPrimary()) {
					IFile file = null;
					IMarker marker = null;
					String filename = loc.getSourceFile();
					if (filePathToEclipseFile.containsKey(filename)) {
						file = filePathToEclipseFile.get(filename);
					}
					else {
						file = SCARFtoEclipseFile(filename);
						System.out.println("Found the matching file: " + file);
						filePathToEclipseFile.put(filename, file);
					}
					if (file != null) {
						marker = createMarkerForResource(file, loc, bug, toolName);
					}
					if (table != null) {
						details.setPrimaryFilename(filename);
						details.setPrimaryLineNumber(loc);
						item.setText(0, filename);
						item.setText(1, BugDetail.formatSingleLineNumber(loc.getStartLine()));
						item.setText(2, BugDetail.formatSingleLineNumber(loc.getEndLine()));
						item.setText(3, bug.getBugGroup());
						item.setText(4, toolName);
						item.setText(5, platformName);
						if (marker != null) {
							item.setData(MARKER_OBJ, marker);
						}
					}
				}
				details.addLocation(loc);
			}
			if (table != null) {
				item.setData(BUG_DETAIL_OBJ, details);
			}
			rows.add(item);
		}
		resetDetailView(page);
	}
	
	/**
	 * Creates an editor marker for a single bug/weakness
	 * @param resource file to create markers for
	 * @param bug weakness to create a marker for
	 * @param toolName tool that found this weakness
	 * @return editor marker
	 */
	private static IMarker createMarkerForResource(IFile resource, Location l, BugInstance bug, String toolName) {
				try {
					String markerType = Activator.getMarkerType(toolName, bug.getBugGroup(), bug.getBugSeverity());
					System.out.println("MARKER TYPE: " + markerType);
					IMarker marker = resource.createMarker(markerType);
					marker.setAttribute(IMarker.MESSAGE, toolName + ": " + bug.getBugMessage());
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING); // TODO Get priority right here
					marker.setAttribute(IMarker.LINE_NUMBER, l.getStartLine()); // TODO End line also / lines between also?
					return marker;
				}
				catch (CoreException e) {
					System.err.println("Core exception when creating marker");
					e.printStackTrace();
				}
			// TODO Maybe add some marker for non-primary bugs? This would probably get too noisy
		return null;
	}
	
	/**
	 * Resets the ViewParts and editor of the SWAMP perspective
	 * @param window currently opened window
	 * @param page currently active page
	 */
	private static void resetPerspectiveParts(IWorkbenchWindow window, IWorkbenchPage page) {
		resetFileMarkers(window);
		resetTableView(page);
		resetDetailView(page);
	}
	
	/**
	 * Resets the TableView
	 * @param page currently active page
	 */
	private static void resetTableView(IWorkbenchPage page) {
		TableView view = (TableView) getView(page, TableView.ID);
		if (view != null) {
			view.resetTable();
		}
	}
	
	/**
	 * Resets the DetailView
	 * @param page currently active page
	 */
	private static void resetDetailView(IWorkbenchPage page) {
		DetailView view = (DetailView) getView(page, DetailView.ID);
		if (view != null) {
			view.reset();
		}
	}
	
	/**
	 * Removes all plug-in file markers from the file in the window
	 * @param window currently opened window
	 */
	public static void resetFileMarkers(IWorkbenchWindow window) {
		String DASH = "-";
		System.out.println("Attempting to reset file markers");
		IFile file = HandlerUtilityMethods.getActiveFile(window);
		if (file != null) {
			System.out.println("Removing file markers for file " + file.getName());
			try {
				for (String color : Activator.getValidColors()) {
					for (String shape: Activator.getValidShapes()) {
						String markerType = Activator.MARKER_PREFIX + color + DASH + shape + Activator.MARKER_SUFFIX;
						file.deleteMarkers(markerType, true, 1);
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("File is null");
		}
	}
	
	/**
	 * Updates the detail view with a specific bug
	 * @param bug Bug to be displayed in the DetailView
	 */
	public static void updateDetailView(BugDetail bug) {
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
	
	/**
	 * Utility method for getting active workbench window
	 * @return reference to the active workbench window
	 */
	private static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb == null) {
			return null;
		}
		return wb.getActiveWorkbenchWindow();
	}
	
	/**
	 * Jumps editor to marker location
	 * @param marker the marker whose location to jump to
	 */
	public static void jumpToLocation(IMarker marker) {
		if (marker == null || (marker.getAttribute(IMarker.LINE_NUMBER, 0) == 0)) {
			return;
		}
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			IFile file = (IFile) marker.getResource();
			IEditorPart editor = null;
			try {
				editor = IDE.openEditor(window.getActivePage(), file, true);
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (editor != null) {
				System.out.println("Going to marker at: " + marker.getAttribute(IMarker.LINE_NUMBER, 0));
				IDE.gotoMarker(editor, marker);
			}
		}
	}
	
	/**
	 * Updates status view (i.e. table of assessment statuses)
	 * @param statuses list of assessment statuses
	 */
	public static void updateStatusView(List<String> statuses) {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			StatusView view = (StatusView) getView(window, StatusView.ID);
			if (view != null) {
				view.addRowsToStatusTable(statuses);
			}
		}
	}
	
}
