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

package org.continuousassurance.swamp.eclipse.ui;

import java.io.File;
import java.util.List;
import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.AssessmentDetails;
import org.continuousassurance.swamp.eclipse.ResultsRetriever;
import org.continuousassurance.swamp.eclipse.StatusChecker;
import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.continuousassurance.swamp.eclipse.Utils;
import org.continuousassurance.swamp.eclipse.exceptions.ResultsRetrievalException;
import org.continuousassurance.swamp.eclipse.exceptions.UserNotLoggedInException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * This class implements the UI/UX for the Assessment Status Dashboard. This
 * shows the user the status of her assessments and allows the user to "remove"
 * assessments. 
 * @author reid-jr
 *
 */
public class StatusView extends ViewPart {

	/**
	 * Names of the columns of the table
	 */
	public static final String[] COLUMN_NAMES = {"SWAMP Package", "Version", "Tool", "Submission Time", "Status", "Count", "Eclipse Project", "Platform" };
	
	/**
	 * Widths of the columns of the table
	 */
	private static final int[] COLUMN_WIDTHS = {200, 240, 240, 200, 260, 50, 240, 240};
	
	/**
	 * Types (String vs. int) of the columns of the table (this is used for sorting properly)
	 */
	private static final String[] COLUMN_TYPES = {Utils.STR_TYPE, Utils.STR_TYPE, Utils.STR_TYPE, Utils.STR_TYPE, Utils.STR_TYPE, Utils.INT_TYPE, Utils.STR_TYPE, Utils.STR_TYPE};
	
	/**
	 * Label for refresh toolbar button
	 */
	private static final String REFRESH_ACTION_LABEL = "Refresh";
	
	/**
	 * Key for storing Assessment UUID with row (see TableItem.getData()/setData())
	 */
	private static final String ASSESS_UUID = "assess_uuid";
	
	/**
	 * Key for storing results file path with row (see TableItem.getData()/setData())
	 */
	private static final String RESULTS_FILEPATH = "results_filepath";
	
	/**
	 * ID for this view (matches id in plugin.xml)
	 */
	public static final String ID = "org.continuousassurance.swamp.eclipse.ui.views.statusview";
	
	/**
	 * Action for refreshing assessment statuses
	 */
	private Action refreshItemAction;
	
	/**
	 * SWT Table widget
	 */
	private Table table;
	
	
	/**
	 * Label for right-click remove assessment option
	 */
	private static final String REMOVE_ASSESSMENT = "Remove Assessment";
	
	@Override
	/**
	 * This method creates the UI/UX for the StatusView 
	 * @param parent the composite on which this view is positioned
	 */
	public void createPartControl(Composite parent) {
		table = Utils.constructTable(parent);
		
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			Utils.addTableColumn(table, COLUMN_NAMES[i], COLUMN_WIDTHS[i], i, COLUMN_TYPES[i], ASSESS_UUID, RESULTS_FILEPATH);
		}
		
		Menu menu = new Menu(table);
		table.setMenu(menu);
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(REMOVE_ASSESSMENT);
		item.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				for (int i : table.getSelectionIndices()) {
					// TODO: In future, use SwampApiWrapper to remove a package 
					// (this functionality doesn't currently exist)
					TableItem row = table.getItem(i);
					String assessUUID = (String)row.getData(ASSESS_UUID);
					if (assessUUID != null) {
						ResultsRetriever.addAssessmentToDelete(assessUUID);
					}
					String filepath = (String)row.getData(RESULTS_FILEPATH);
					if (filepath != null) {
						System.out.println("Non-null filepath at: " + filepath);
						File f = new File(filepath);
						if (f.exists()) {
							System.out.println("File exists at: " + filepath);
							f.delete();
						}
					}
					table.remove(i);
				}
			}
		});
		
		createActions();
		createToolbar();
	}
	
	/**
	 * Creates refresh action for synchronously refreshing the assessment 
	 * statuses
	 */
	private void createActions() {
		refreshItemAction = new Action(REFRESH_ACTION_LABEL) {
			@Override
			public void run() {
				StatusChecker sc = Activator.getStatusChecker();
				if (sc != null && sc.isRunning()) {
					System.out.println("Status checker already running");
					return;
				}
				try {
					ResultsRetriever.retrieveResults();
				} catch (UserNotLoggedInException e) {
					SwampSubmitter ss = new SwampSubmitter(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
					if (ss.authenticateUser()) {
						try {
							ResultsRetriever.retrieveResults();
						} catch (UserNotLoggedInException e1) {
							e1.printStackTrace();
						} catch (ResultsRetrievalException e1) {
							e1.printStackTrace();
						}
					}
				} catch (ResultsRetrievalException e) {
					e.printStackTrace();
				}
			}
		};
		// TODO: refreshItemAction.setImageDescriptor("icons/refresh.png");
	}
	
	/**
	 * Creates toolbar on which actions will be placed
	 */
	private void createToolbar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		mgr.add(refreshItemAction);
	}

	@Override
	/**
	 * Sets focus on the table in this view
	 */
	public void setFocus() {
		table.setFocus();
	}
	
	/**
	 * This method repopulates the table of assessment statuses
	 * with the passed in statuses. Note: It also sets the assessment UUID
	 * and results file path as objects associated with each row.
	 * @param statuses list of comma-delimited assessment status strings
	 */
	public void addRowsToStatusTable(List<String> statuses) {
		if (table == null) {
			return;
		}
		table.removeAll();
		for (String s : statuses) {
			TableItem item = new TableItem(table, SWT.NONE);
			String[] parts = s.split(AssessmentDetails.DELIMITER);
			for (int i = AssessmentDetails.NUM_HIDDEN_FIELDS; i < parts.length; i++) {
				item.setText(i-AssessmentDetails.NUM_HIDDEN_FIELDS, parts[i]);
			}
			item.setData(ASSESS_UUID, parts[AssessmentDetails.ASSESS_UUID_PART]);
			item.setData(RESULTS_FILEPATH, parts[AssessmentDetails.RESULTS_FILEPATH_PART]);
		}
	}
}
