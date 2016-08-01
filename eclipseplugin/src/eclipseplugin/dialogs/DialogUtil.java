package eclipseplugin.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

public class DialogUtil {
	
	
	public static Text initializeTextWidget(int style, Composite container, GridData griddata)  {
		Text text = new Text(container, style);
		text.setLayoutData(griddata);
		return text;
	}
	
	public static Label initializeLabelWidget(String text, int style, Composite container) {
		Label label = new Label(container, style);
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		return label;
	}
	
	public static Combo initializeComboWidget(Composite container, GridData griddata, String[] options) {
		Combo combo = new Combo(container, SWT.DROP_DOWN);
		combo.setLayoutData(griddata);
		combo.setItems(options);
		return combo;
	}
	
	public static List initializeListWidget(Composite container, GridData griddata, String[] options) {
		List list = new List(container, SWT.MULTI + SWT.V_SCROLL);
		list.setLayoutData(griddata);
		list.setItems(options);
		return list;
	}
	
	public static Button initializeButtonWidget(Composite container, String text, GridData griddata, int style) {
		Button button = new Button(container, style);
		button.setText(text);
		button.setLayoutData(griddata);
		return button;
	}
}
