package eclipseplugin.handlers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.File;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

import eclipseplugin.Activator;
import eclipseplugin.SwampSubmitter;
import eclipseplugin.Utils;
import eclipseplugin.dialogs.AuthenticationDialog;
import edu.uiuc.ncsa.swamp.api.AssessmentRecord;
import edu.wisc.cs.swamp.SwampApiWrapper;

public class CheckResultsHandler extends AbstractHandler {

	private IWorkbenchWindow window;
	SwampApiWrapper api = null;
	
	public CheckResultsHandler() {
		window = null;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String message = "Got results\n";
		window = HandlerUtil.getActiveWorkbenchWindow(event);
		MessageDialog.open(MessageDialog.CONFIRM, window.getShell(), "Results", message, SWT.NONE);
		checkForResults();
		// Eventually this will be refactored into some sort of SwampResultsHandler but for now, we'll do it all here
		// need to be logged in
		// then we can fetch - keep some persistent object with the Set<String> of assessment ids                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
		return null;
	}
	
	private void checkForResults() {
		try {
			api = new SwampApiWrapper(SwampApiWrapper.HostType.CUSTOM, Activator.getLastHostname());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		try {
			if (!api.restoreSession()) {
				// Add authentication dialog here
				if (!authenticateUser()) {
					return;
				}
			}
		} catch (Exception e) {
			if (!authenticateUser()) {
				return;
			}
		}
		Map<String, Set<String>> unfinishedMap = Activator.getUnfinishedAssessments();
		for (String projectID : unfinishedMap.keySet()) {
			Set<String> set = unfinishedMap.get(projectID);
			for (String assessID : set) {
				// TODO: Get SwampApiWrapper instance
				// TODO: Construct filepath
				String id = projectID + "-" + assessID;
				AssessmentRecord record = api.getAssessmentRecord(projectID, assessID);
				System.out.println("Status: " + record.getStatus());
				if (record.getStatus().equals("Finished")) {
					String filepath = constructFilepath(projectID, record.getPackageUUID(), record.getToolUUID(), record.getPlatformUUID());
					java.io.File f = new java.io.File(filepath);
					if (f.exists()) {
						f.delete();
					}
					api.getAssessmentResults(projectID, record.getAssessmentResultUUID(), filepath);
					System.out.println("Found results for " + id);
					System.out.println("Written to filepath: " + filepath);
					Activator.finish(projectID, assessID);
				}
				else {
					System.out.println("Still waiting on " + id);
				}
			}
		}
	}
	
	/* TODO: This is actually code from SwampSubmitter -- need to refactor it! */
	private boolean authenticateUser() {
		AuthenticationDialog ad = new AuthenticationDialog(window.getShell(), new MessageConsoleStream(new MessageConsole("SWAMP Results", null)));
		ad.create();
		if (ad.open() != Window.OK) {
			return false;
		}
		api = ad.getSwampApiWrapper();
		Activator.setLoggedIn(true);
		return true;
	}
	
	private String constructFilepath(String projectUUID, String pkgUUID, String toolUUID, String platformUUID) {
		// TODO: get plugin location + SEPARATOR + projectID + "-" + assessID + ".results";
		System.out.println("Project UUID: " + projectUUID);
		System.out.println("Package UUID: " + pkgUUID);
		System.out.println("Tool UUID: " + toolUUID);
		System.out.println("Platform UUID: " + platformUUID);
		String SEPARATOR = System.getProperty("file.separator");
		return System.getProperty("user.home") + SEPARATOR + SwampSubmitter.SWAMP_RESULTS_DIRNAME + SEPARATOR + projectUUID + SEPARATOR + pkgUUID + SEPARATOR + toolUUID + "-" + platformUUID + "-" + "results.xml";
	}

}
