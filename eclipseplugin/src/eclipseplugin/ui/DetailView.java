package eclipseplugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import eclipseplugin.dialogs.DialogUtil;

public class DetailView extends ViewPart {
	Composite composite;
	@Override
	public void createPartControl(Composite parent) {
		composite = parent;
		DialogUtil.initializeLabelWidget("Information: Piece A", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Information: Piece B", SWT.NONE, parent);
		DialogUtil.initializeLabelWidget("Information: Piece C", SWT.NONE, parent);
	}

	@Override
	public void setFocus() {
		composite.setFocus();
	}

}
