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

import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Listener that enables sorting of columns when the column header is clicked
 * @author reid-jr
 *
 */
public class SortListener implements Listener {
	String[] keys;
	public SortListener(String[] dataKeys) {
		keys = dataKeys;
	}

	@Override
	/**
	 * Sorts the clicked on column as appropriate
	 * @param e click event
	 */
	public void handleEvent(Event e) {
		TableColumn selectedCol = (TableColumn) e.widget;
		Table table = selectedCol.getParent();
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
	            	int numCols = table.getColumnCount();
	            	String[] values = new String[numCols];
	            	for (int k = 0; k < numCols; k++) {
	            		values[k] = items[i].getText(k);
	            	}
	            	TableItem item = new TableItem(table, SWT.NONE, j);
	            	item.setText(values);
	            	if (keys != null && keys.length > 0) {
	            		for (String key : keys) {
	            			Object obj = items[i].getData(key);
	            			if (obj != null) {
	            				item.setData(key, obj);
	            			}
	            		}
	            	}
	            	Object obj = items[i].getData();
	            	if (obj != null) {
	            		item.setData(obj);
	            	}
	            	items[i].dispose();
	            	items = table.getItems();
	            	break;
	            }
			}
		}
	}
}