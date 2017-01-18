/*
 * Copyright 2016 Malcolm Reid Jr.
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
import java.util.Map;

import org.continuousassurance.swamp.eclipse.ResultsParser;
import org.continuousassurance.swamp.eclipse.ResultsUtils;
import org.continuousassurance.swamp.eclipse.handlers.HandlerUtilityMethods;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPageService;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPerspectiveListener3;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import dataStructures.BugInstance;
import dataStructures.Location;

public class SwampPerspective implements IPerspectiveFactory {

	public static String TABLE_VIEW_DESCRIPTOR = "org.continuousassurance.swamp.eclipse.ui.views.tableview";
	public static String DETAIL_VIEW_DESCRIPTOR = "org.continuousassurance.swamp.eclipse.ui.views.detailview";
	public static IWorkbenchWindow window;
	
	public SwampPerspective() {
		super();
		IWorkbench wb = PlatformUI.getWorkbench();
		window = wb.getActiveWorkbenchWindow();
	}
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		//IWorkbench wb = PlatformUI.getWorkbench();
		//window = wb.getActiveWorkbenchWindow();
		defineActions(layout);
		defineLayout(layout);
	}
	
	public void defineActions(IPageLayout layout) {
		
	}
	
	public void defineLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		// TODO: Add custom view for list of bugs
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.3f, editorArea);
		bottom.addView(TABLE_VIEW_DESCRIPTOR);
		// TODO: Add custom view for detailed look at a single bug
		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.2f, editorArea);
		right.addView(DETAIL_VIEW_DESCRIPTOR);
		//right.addView(IPageLayout.ID_TASK_LIST);
		IPartService service = window.getPartService();
		service.addPartListener(new FileChangeListener());
	}
	
	public void annotateEditor(IFile file, List<BugInstance> bugs, String toolName) {
		
		try {
			for (BugInstance bug : bugs) {
				// System.out.println(bug);
				createMarkerForResource(file, bug, toolName);
			}
		}
		catch (Exception e) {
			System.out.println("Failed to create marker");
			e.printStackTrace();
		}
	}
	
	public void createMarkerForResource(IFile resource, BugInstance bug, String toolName) {
		for (Location l : bug.getLocations()) {
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
			}
			catch (CoreException e) {
				System.err.println("Core exception when creating marker");
				e.printStackTrace();
			}
		}
	}
	
	public void updateTableView(List<String[]> rows) {
		IWorkbenchPage page = window.getActivePage();
		TableView view = (TableView) page.findView(TABLE_VIEW_DESCRIPTOR);
		view.update(rows);
	}
	
	public class FileChangeListener implements IPartListener2 {

		@Override
		public void partActivated(IWorkbenchPartReference part) {

			
			System.out.println("Doing all of the part updating");
			System.out.println("Part Activated");
			IProject project = HandlerUtilityMethods.getActiveProject(window);
			System.out.println("Project open: " + project.getName());
			IFile file = HandlerUtilityMethods.getActiveFile(window);
			
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
				List<String[]> rows = new ArrayList<String[]>();
				//List<BugInstance> bugs = new ArrayList<>();
				File[] files = resultsDir.listFiles();
				if (files != null && files.length > 0) {
					for (File r : files) {
						List<BugInstance> bugs = new ArrayList<>();
						ResultsParser rp = new ResultsParser(r);
						String toolName = rp.getToolName();
						rows.addAll(rp.getRows());
						String filepath = file.getFullPath().toString();
						filepath = filepath.substring(1);
						bugs.addAll(rp.getFileBugs(filepath));
						annotateEditor(file, bugs, toolName);
					}
				}
				updateTableView(rows);
				//annotateEditor(file, bugs);

				if (rows.size() == 0) {
					setDetailViewMessage("No bugs found");
				}
			}
			else {
				System.out.println("No results found");
				updateTableView(new ArrayList<String[]>());
				setDetailViewMessage("No results files found for this project");
			}
			//annotateEditor(file, lines);
			// TODO: else print no results found
			
			//ResultsParser.updateWindow(window, project, file);
			//updateTableView(project);
			//updateDetailView(file);
			//printPartInfo(part);
			// TODO Auto-generated method stub
			
		}
		
		private void setDetailViewMessage(String message) {
			IWorkbenchPage page = window.getActivePage();
			DetailView view = (DetailView) page.findView(DETAIL_VIEW_DESCRIPTOR);
			view.redrawPartControl(message, "N/A", "N/A", "N/A", "N/A", "N/A");
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference arg0) {
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
			// TODO Auto-generated method stub
		}

		@Override
		public void partOpened(IWorkbenchPartReference part) {
			System.out.println("Part Opened");
			//printPartInfo(part);
			
		}

		@Override
		public void partVisible(IWorkbenchPartReference arg0) {
			// TODO Auto-generated method stub
			
		}
		
		private void printPartInfo(IWorkbenchPartReference part) {
			System.out.println("Part Opened");
			System.out.println("Part class: " + part.getClass());
			System.out.println("Part ID: " + part.getId());
			System.out.println("Part title: " + part.getTitle());
			System.out.println("Part Content description: " + part.getContentDescription());
			System.out.println("Part name: " + part.getPartName());
			//part.getPage().getActiveEditor().getEditorInput()
		}
		
	}
	
}
