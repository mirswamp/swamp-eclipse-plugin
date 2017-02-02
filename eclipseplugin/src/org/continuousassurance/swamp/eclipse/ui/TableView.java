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

import java.util.Comparator;
import java.util.List;

import org.continuousassurance.swamp.eclipse.BugDetail;
import org.continuousassurance.swamp.eclipse.ResultsParser;
import org.continuousassurance.swamp.eclipse.Utils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;


/**
 * TableView class is a view that shows a list of bugs and their information.
 * The columns are sortable. Clicking on a bug shows more detailed information
 * about the bug in the Detail view. Double-clicking on the bug jumps to the
 * bug's location in a source file. 
 * @author reid-jr
 *
 */
public class TableView extends ViewPart { 
	
	/**
	 * Names of the columns of the table
	 */
	public static final String[] COLUMN_NAMES = {"File", "Start Line", "End Line", "Bug Type", "Tool", "Platform"};
	/**
	 * Widths of the columns of the table
	 */
	private static final int[] COLUMN_WIDTHS = {500, 300, 300, 500, 500, 500};
	/**
	 * SWT Table widget
	 */
	private Table table;
	
	/**
	 * Name of int type for table column
	 */
	private static String INT_TYPE = "INT";
	
	/**
	 * Name of string type for table column 
	 */
	private static String STR_TYPE = "STR";
	
	// TODO: Make columns have more appropriate widths by default
	
	/**
	 * Constructor for TableView
	 */
	public TableView() {
		super();
	}
	
	/**
	 * Creates and returns a table column for the specified table
	 * @param table SWT table widget for the view
	 * @param index index for column name and width to be set properly
	 * @param type "INT" or "STRING" (need to know for sorting the column properly)
	 * @return newly created table column
	 */
	private static TableColumn getTableColumn(Table table, int index, String type) {
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMN_NAMES[index]);
		col.setWidth(COLUMN_WIDTHS[index]);
		if (type.equals(INT_TYPE)) {
			col.setData(Utils.INT_CMP(index));
		}
		else {
			col.setData(Utils.STR_CMP(index));
		}
		return col;
	}
	
	/**
	 * Getter for table (ideally this wouldn't be exposed but we need it to be
	 * to create rows and add the markers and BugDetail objects to them)
	 * @return table widget
	 */
	public Table getTable() {
		return table;
	}
	
	@Override
	/**
	 * Creates part control
	 * @param parent composite on which widgets will be placed and positioned
	 */
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(gd);
		
		TableColumn fileColumn = getTableColumn(table, 0, STR_TYPE);
		TableColumn startLineColumn = getTableColumn(table, 1, INT_TYPE);
		TableColumn endLineColumn = getTableColumn(table, 2, INT_TYPE);
		TableColumn typeColumn = getTableColumn(table, 3, STR_TYPE);
		TableColumn toolColumn = getTableColumn(table, 4, STR_TYPE);
		TableColumn platformColumn = getTableColumn(table, 5, STR_TYPE);
		
		fileColumn.addListener(SWT.Selection, new SortListener());
		startLineColumn.addListener(SWT.Selection, new SortListener());
		endLineColumn.addListener(SWT.Selection, new SortListener());
		typeColumn.addListener(SWT.Selection, new SortListener());
		toolColumn.addListener(SWT.Selection, new SortListener());
		platformColumn.addListener(SWT.Selection, new SortListener());
		
		table.addSelectionListener(new RowSelectionListener(table));
		table.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TableItem[] items = table.getSelection();
				if (items.length > 0) {
					TableItem item = items[0];
					IMarker marker = (IMarker)item.getData(SwampPerspective.MARKER_OBJ);
					IWorkbench wb = PlatformUI.getWorkbench();
					IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
					IWorkbenchPage page = window.getActivePage();
					IEditorPart editor = page.getActiveEditor();
					if ((marker != null) && (editor != null)) {
						IDE.gotoMarker(editor, marker);
					}
				}
			}
		});
	}
	
	@Override
	/**
	 * Sets focus on the table
	 */
	public void setFocus() {
		table.setFocus();
	}
	
	/**
	 * Listener that enables sorting of columns when the column header is clicked
	 * @author reid-jr
	 *
	 */
	private class SortListener implements Listener {

		@Override
		/**
		 * Sorts the clicked on column as appropriate
		 * @param e click event
		 */
		public void handleEvent(Event e) {
			TableColumn selectedCol = (TableColumn) e.widget;
			int dir = table.getSortDirection();
			if (table.getSortColumn() == selectedCol) {
				dir = (dir == SWT.UP) ? SWT.DOWN : SWT.UP;
			}
			else {
				dir = SWT.UP;
				table.setSortColumn(selectedCol);
			}
			table.setSortDirection(dir);
			TableItem[] items = table.getItems();
			@SuppressWarnings("unchecked")
			Comparator<TableItem> comparator = (Comparator<TableItem>) selectedCol.getData();
			for (int i = 1; i < items.length; i++) {
				for (int j = 0; j < i; j++) {
		            if ((comparator.compare(items[i], items[j]) < 0 && dir == SWT.UP) || (comparator.compare(items[i], items[j]) > 0 && dir == SWT.DOWN)) {
		            	String[] values = new String[COLUMN_NAMES.length];
		            	for (int k = 0; k < COLUMN_NAMES.length; k++) {
		            		values[k] = items[i].getText(k);
		            	}
		            	items[i].dispose();
		            	TableItem item = new TableItem(table, SWT.NONE, j);
		            	item.setText(values);
		            	items = table.getItems();
		            	break;
		            }
				}
			}
		}
	}
	
	/**
	 * Listener for TableItem (i.e. row) selection in a table
	 * @author reid-jr
	 */
	private class RowSelectionListener implements SelectionListener {
		/**
		 * Reference to the table in which the rows (i.e. TableItem objects)
		 * are in
		 */
		private Table table;

		/**
		 * Constructor for RowSelectionListener
		 * @param t table
		 */
		public RowSelectionListener(Table t) {
			table = t;
		}

		@Override
		/**
		 * Updates Detail View when a row gets selected
		 * @param event selection event that occured
		 */
		public void widgetSelected(SelectionEvent event) {
			TableItem[] items = table.getSelection();
			if (items.length > 0) {
				TableItem selectedRow = items[0];
				// TODO: Store a reference to detail view, so we don't keep on having to do this!
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				DetailView view = (DetailView) page.findView(SwampPerspective.DETAIL_VIEW_DESCRIPTOR);
				BugDetail details = (BugDetail)selectedRow.getData(SwampPerspective.BUG_DETAIL_OBJ);
				if (view != null) {
					view.update(details);
				}
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event) {
		}
		
	}
	
}
