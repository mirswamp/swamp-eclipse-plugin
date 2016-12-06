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

import java.util.Comparator;
import java.util.List;

import org.continuousassurance.swamp.eclipse.ResultsParser;
import org.continuousassurance.swamp.eclipse.Utils;
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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.ViewPart;

import javaSCARF.ScarfInterface;

public class TableView extends ViewPart implements ScarfInterface {
	
	public static final String[] COLUMN_NAMES = {"File", "Start Line", "End Line", "Bug Type", "Tool", "Platform"};
	private static final int[] COLUMN_WIDTHS = {500, 300, 300, 500, 500, 500};
	private Table table;
	private ResultsParser resultsParser;
	
	// TODO: Make columns have more appropriate widths by default
	public TableView(ResultsParser rp) {
		super();
		resultsParser = rp;
	}
	
	public TableView() {
		super();
	}
	
	private static TableColumn getTableColumn(Table table, int index, String type) {
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(COLUMN_NAMES[index]);
		col.setWidth(COLUMN_WIDTHS[index]);
		if (type.equals("INT")) {
			col.setData(Utils.INT_CMP(index));
		}
		else {
			col.setData(Utils.STR_CMP(index));
		}
		return col;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(gd);
		
		TableColumn fileColumn = getTableColumn(table, 0, "STRING");
		TableColumn lineColumn = getTableColumn(table, 1, "INT");
		TableColumn typeColumn = getTableColumn(table, 3, "STRING");
		TableColumn toolColumn = getTableColumn(table, 4, "STRING");
		TableColumn platformColumn = getTableColumn(table, 5, "STRING");
		

		/* The following code adapted from http://stackoverflow.com/questions/15508493/swt-table-sorting-by-clicking-the-column-header */
		Listener sortListener = e -> {
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
		};
		
		fileColumn.addListener(SWT.Selection, sortListener);
		lineColumn.addListener(SWT.Selection, sortListener);
		typeColumn.addListener(SWT.Selection, sortListener);
		toolColumn.addListener(SWT.Selection, sortListener);
		platformColumn.addListener(SWT.Selection, sortListener);
		
		table.addSelectionListener(new RowSelectionListener(table));
		//table.addMouseListener(new DoubleClickListener(table));
		
		/*
		List<String[]> rowElements = resultsParser.getRows();
		for (String[] row : rowElements) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(row);
			// item.addListener(SWT.Selection, rowSelectionListener);
		}
		*/
		
		/*
		// TODO: Add table items from the actual data
		for (int i = 0; i < 5; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, i % 2 == 0 ? "Test1.java" : "Test2.java");
			item.setText(1, Integer.toString(i*100));
			item.setText(2, i % 2 == 0 ? "Style" : "Other");
			item.setText(3, i % 3 == 0 ? "FindBugs" : "Test Tool");
			item.setText(4, "RedHat version" + i);
			//item.addListener(SWT.Selection, rowSelectionListener);
		}
		*/
	
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			table.getColumn(i).pack();
		}

	}
	
	@Override
	public void setFocus() {
		table.setFocus();
	}
	
	private class RowSelectionListener implements SelectionListener {
		Table table;
		public RowSelectionListener(Table t) {
			table = t;
		}
		@Override
		public void widgetSelected(SelectionEvent event) {
			System.out.println("Selection occured");
			TableItem[] items = table.getSelection();
			if (items.length > 0) {
				TableItem selectedRow = items[0];
				// TODO: Store a reference to detail view, so we don't keep on having to do this!
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				DetailView view = (DetailView) page.findView("org.continuousassurance.swamp.eclipse.ui.views.detailview");
				view.redrawPartControl(selectedRow.getText(0), selectedRow.getText(1), selectedRow.getText(2), selectedRow.getText(3), selectedRow.getText(4));
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
