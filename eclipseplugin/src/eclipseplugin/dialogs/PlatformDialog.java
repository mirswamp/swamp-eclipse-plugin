package eclipseplugin.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eclipseplugin.SubmissionInfo;
import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.uiuc.ncsa.swamp.api.Platform;

public class PlatformDialog extends TitleAreaDialog {
	private List<Platform> platforms;
	private org.eclipse.swt.widgets.List swtPlatformList;
	private SwampApiWrapper api;
	private SubmissionInfo submissionInfo;
	
	public PlatformDialog(Shell parentShell, SubmissionInfo si) {
		super(parentShell);
		submissionInfo = si;
		api = submissionInfo.getApi();
	}
	
	@Override protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		
		this.setTitle("Platform Selection");
		
		/* Note: From GridData JavaDoc, "Do not reuse GridData objects. Every control in a composite
		 * that is managed by a GridLayout must have a unique GridData object.
		 */
		
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));

		DialogUtil.initializeLabelWidget("Platforms: ", SWT.NONE, container);
		platforms = getPlatforms(submissionInfo.getSelectedToolIDs(), submissionInfo.getSelectedProjectID());
		swtPlatformList = DialogUtil.initializeListWidget(container, new GridData(SWT.FILL, SWT.NONE, true, false), convertPlatformListToStringArray());
		
		if (submissionInfo.platformsInitialized()) {
			List<String> platformUUIDs = submissionInfo.getSelectedPlatformIDs();
			setSelectedPlatforms(platformUUIDs);
		}
		else {
			// select all platforms by default
			swtPlatformList.selectAll();
		}
			
		return area;
	}
	
	private List<Platform> getPlatforms(List<String> toolUUIDs, String prjUUID) {
		Set<Platform> platformSet = new HashSet<Platform>();
		for (String toolUUID : toolUUIDs) {
			List<Platform> list = api.getSupportedPlatforms(toolUUID, prjUUID);
			for (Platform p : list) {
				platformSet.add(p);
			}
		}
		List<Platform> platformList = new ArrayList<Platform>(platformSet.size());
		for (Platform p : platformSet) {
			platformList.add(p);
		}
		return platformList;
	}
	
	private String[] convertPlatformListToStringArray() {
		int numPlatforms = platforms.size();
		String[] platformArray = new String[numPlatforms];
		for (int i = 0; i < numPlatforms; i++) {
			platformArray[i] = platforms.get(i).getUUIDString();
		}
		Arrays.sort(platformArray);
		return platformArray;
	}
	
	private void setSelectedPlatforms(List<String> platformUUIDs) {
		int count = 0;
		int numIDs = platformUUIDs.size();
		for (int i = 0; (i < platforms.size()) && (count < numIDs); i++) {
			String id = platforms.get(i).getUUIDString();
			if (platformUUIDs.contains(id)) {
				swtPlatformList.select(i);
				count++;
			}
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		Button button = createButton(parent, IDialogConstants.NO_ID, "Clear All", false);
		button.addSelectionListener(new ClearButtonSelectionListener());
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}

	@Override
	protected void okPressed() {
		if (swtPlatformList.getSelectionCount() < 1) {
			this.setMessage("Select at least one platform.");
		}
		int[] selectedIndices = swtPlatformList.getSelectionIndices();
		List<String> selectedPlatformIDs = new ArrayList<String>(selectedIndices.length);
		for (int i : selectedIndices) {
			Platform platform = platforms.get(i);
			selectedPlatformIDs.add(platform.getUUIDString());
		}
		submissionInfo.setSelectedPlatformIDs(selectedPlatformIDs);
		super.okPressed();
	}
	
private class ClearButtonSelectionListener implements SelectionListener {
		
		public ClearButtonSelectionListener() {
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			swtPlatformList.deselectAll();
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}