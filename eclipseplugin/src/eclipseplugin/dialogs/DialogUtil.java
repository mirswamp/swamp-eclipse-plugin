package eclipseplugin.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class DialogUtil {
	
	
	public static Text initializeTextWidget(int style, Composite container, GridData griddata)  {
		Text text = new Text(container, style);
		text.setLayoutData(griddata);
		return text;
	}
	
	public static Label initializeLabelWidget(String text, int style, Composite container, GridData griddata) {
		Label label = new Label(container, style);
		label.setText(text);
		label.setLayoutData(griddata);
		return label;
	}
	
	public static Combo initializeComboWidget(Composite container, GridData griddata, String[] options) {
		Combo combo = new Combo(container, SWT.DROP_DOWN);
		combo.setLayoutData(griddata);
		combo.setItems(options);
		return combo;
	}
}
