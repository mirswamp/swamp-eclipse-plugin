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
	
	private ArrayList<String> projectList;
	private ArrayList<String> platformList;
	private ArrayList<String> toolList;
	private HandlerFactory handler;

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
	
	public void setHandlerFactory(HandlerFactory h) {
		handler = h;
	}
	
	private void setComboElements(Combo c, Type t) {
		ArrayList<String> list;
		if (t == Type.PROJECT) {
			list = ParseCommandLine.getProjectList(handler);
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
