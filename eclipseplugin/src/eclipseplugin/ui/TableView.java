package eclipseplugin.ui;

import java.util.Comparator;

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
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.ViewPart;

import eclipseplugin.Utils;

public class TableView extends ViewPart {
	
	public static final String[] COLUMN_NAMES = {"File", "Line", "Bug Type"};
	private static final int[] COLUMN_WIDTHS = {300, 100, 300};
	private Table table;
	
	// TODO: Make columns have more appropriate widths by default
	
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
		TableColumn typeColumn = getTableColumn(table, 2, "STRING");
		

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
		            	String[] values = {items[i].getText(0), items[i].getText(1), items[i].getText(2)};
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
		
		// TODO: Add table items from the actual data
		for (int i = 0; i < 5; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, i % 2 == 0 ? "Test1.java" : "Test2.java");
			item.setText(1, Integer.toString(i*100));
			item.setText(2, i % 2 == 0 ? "Style" : "Other");
		}
	
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			table.getColumn(i).pack();
		}

	}

	@Override
	public void setFocus() {
		table.setFocus();
	}
	
	public class RowSelectionListener implements Listener {

		@Override
		public void handleEvent(Event event) {
			//HandlerUtil.getActiveEditor(event);
			// TODO: Handle row selection
			// TODO: Editor annotations should be shown for this bug
			// TODO: Jump to this place in the editor
		}
		
	}
	
}
