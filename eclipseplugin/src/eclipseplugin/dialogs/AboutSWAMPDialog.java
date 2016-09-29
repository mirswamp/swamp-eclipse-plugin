package eclipseplugin.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class AboutSWAMPDialog extends TitleAreaDialog {
	
	private static String ABOUT_SWAMP_TITLE = "About SWAMP";
	private static String ABOUT_SWAMP_TEST = "Placeholder: The Software Assurance Marketplace";
	
	public AboutSWAMPDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		this.setTitle(ABOUT_SWAMP_TITLE);
		DialogUtil.initializeLabelWidget(ABOUT_SWAMP_TEST, SWT.NONE, area);
		return area;
	}

}
