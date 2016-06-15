/* Malcolm Reid Jr.
 * 06/10/2016
 * UW SWAMP
 * SelectionDialog.java
 * Code to implement a dialog box for getting package, tool, and platform from end user
 */

package eclipseplugin.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
//import org.eclipse.jface.viewers.ILabelProvider;
//import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.widgets.*;
//import org.eclipse.ui.dialogs.FilteredList;
//import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import java.util.ArrayList;
import edu.uiuc.ncsa.swamp.session.handlers.HandlerFactory;
import edu.wisc.cs.swamp.ParseCommandLine;

public class SelectionDialog extends TitleAreaDialog {
	
	//private ArrayList<String> projectList;
	//private ArrayList<String> platformList;
	//private ArrayList<String> toolList;
	private Combo projCombo;
	private Combo platCombo;
	private Combo toolCombo;
	private int prjIndex;
	private int pltIndex;
	private int toolIndex;
	private HandlerFactory handler;

	private enum Type {
		PROJECT, PLATFORM, TOOL
	}
	
	public SelectionDialog(Shell parentShell) {
		super(parentShell);
		//projectList = new ArrayList<String>();
		//platformList = new ArrayList<String>();
		//toolList = new ArrayList<String>();
	}
	
	public void setHandlerFactory(HandlerFactory h) {
		handler = h;
	}
	
	private void setComboElements(Combo c, Type t) {
		ArrayList<String> list;
		if (t == Type.PROJECT) {
			list = ParseCommandLine.getProjectList(handler);
			list.add(0,"Create new project");
		}
		else if (t == Type.PLATFORM) {
			list = ParseCommandLine.getPlatformList(handler);
		}
		else {
			list = ParseCommandLine.getToolList(handler);
		}
		setComboList(c, list);
	}
	
	private void setComboList(Combo c, ArrayList<String> list) {
		if (list != null) {
			String[] ary = new String[list.size()];
			list.toArray(ary);
			c.setItems(ary);
		}
	}
	
	public int getProjectIndex() {
		return prjIndex;
	}
	
	public int getPlatformIndex() {
		return pltIndex;
	}
	
	public int getToolIndex() {
		return toolIndex;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Project Configuration");
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		GridData lblGridData = new GridData();
		lblGridData.horizontalAlignment = SWT.FILL;
		lblGridData.grabExcessHorizontalSpace = true;
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		
		projCombo = addCombo(container, "Project: ", Type.PROJECT, lblGridData, gd);
		platCombo = addCombo(container, "Platform: ", Type.PLATFORM, lblGridData, gd);
		toolCombo = addCombo(container, "Tool: ", Type.TOOL, lblGridData, gd);

		return area;
		
	}
	
	private Combo addCombo(Composite container, String labelText, Type type, GridData lblGridData, GridData comboGridData) {
		Label label = new Label(container, SWT.NONE);
		label.setText(labelText);
		label.setLayoutData(lblGridData);
		Combo c = new Combo(container, SWT.DROP_DOWN);
		setComboElements(c, type);
		c.addSelectionListener(new ComboSelectionListener(c, type));
		c.setLayoutData(comboGridData);
		return c;
	}
	
	private class ComboSelectionListener implements SelectionListener {
		Combo combo;
		Type type;
		public ComboSelectionListener(Combo c, Type t) {
			combo = c;
			type = t;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
			if (type == Type.PROJECT) {
				prjIndex = selection;
				if (selection == 0) {
					System.out.println("Create a project selected");
					platCombo.removeAll();
					toolCombo.removeAll();
				}
				else {
					setComboElements(platCombo, Type.PLATFORM);
					setComboElements(toolCombo, Type.TOOL);
				}
				// logic for creating a project
			}
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
		}
	}

}
