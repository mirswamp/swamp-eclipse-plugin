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

public class SelectionDialog extends TitleAreaDialog {
	
	private ArrayList<String> projectList;
	private ArrayList<String> platformList;
	private ArrayList<String> toolList;

	private enum Type {
		PROJECT, PLATFORM, TOOL
	}
	
	public SelectionDialog(Shell parentShell) {
		super(parentShell);
		projectList = new ArrayList<String>();
		platformList = new ArrayList<String>();
		toolList = new ArrayList<String>();
		// initialize the 3 lists here
		// dummy initializing
		for (int i = 0; i < 10; i++) {
			String tmp = "Choice " + i;
			projectList.add(tmp);
			platformList.add(tmp);
			toolList.add(tmp);
		}
	}
	
	private void setComboElements(Combo c, Type t) {
		if (t == Type.PROJECT) {
			// get list of projects as string array
			// do c.setItems(array)
		}
		else if (t == Type.PLATFORM) {
			// get list of platforms as string array
			// do c.setItems(array)			
		}
		else {
			// get list of tools as string array
			// do c.setItems(array)			
		}
		String[] array = {"String 0", "String 1", "String 2", "String 3", "String 4", "String 5", "String 6"};
		c.setItems(array);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Project Configuration");
		
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		GridData lblGridData = new GridData();
		lblGridData.horizontalAlignment = GridData.FILL;
		lblGridData.grabExcessHorizontalSpace = true;
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		
		Label projectLabel = new Label(container, SWT.NONE);
		projectLabel.setText("Project: ");
		projectLabel.setLayoutData(lblGridData);
		Combo prjCombo = new Combo(container, SWT.DROP_DOWN);
		setComboElements(prjCombo, Type.PROJECT);
		prjCombo.addSelectionListener(new ComboSelectionListener(prjCombo));
		prjCombo.setLayoutData(gd);

		Label platformLabel = new Label(container, SWT.NONE);
		platformLabel.setText("Platform: ");
		platformLabel.setLayoutData(lblGridData);
		Combo ptfCombo = new Combo(container, SWT.DROP_DOWN);
		setComboElements(ptfCombo, Type.PLATFORM);
		ptfCombo.addSelectionListener(new ComboSelectionListener(ptfCombo));
		ptfCombo.setLayoutData(gd);
		
		Label toolLabel = new Label(container, SWT.NONE);
		toolLabel.setText("Tool: ");
		toolLabel.setLayoutData(lblGridData);
		Combo toolCombo = new Combo(container, SWT.DROP_DOWN);
		setComboElements(toolCombo, Type.TOOL);
		toolCombo.addSelectionListener(new ComboSelectionListener(toolCombo));
		toolCombo.setLayoutData(gd);
		
		return area;
		
	}
	
	private class ComboSelectionListener implements SelectionListener {
		Combo combo;
		public ComboSelectionListener(Combo c) {
			combo = c;
		}
		
		public void widgetSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
			int selection = combo.getSelectionIndex();
			System.out.println("Index " + selection + " selected");
		}
	}

}
