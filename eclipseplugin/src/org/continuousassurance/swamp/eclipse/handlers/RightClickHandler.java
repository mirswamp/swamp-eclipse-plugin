/*
 * Copyright 2016-2017 Malcolm Reid Jr.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.continuousassurance.swamp.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.continuousassurance.swamp.eclipse.Utils;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

/**
 * Handler for Right Click submit
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RightClickHandler extends AbstractHandler {
	
	private static final String PROJECT_NOT_OPEN_CONSOLE_ERROR = "Error: Project is not open";
	
	private static final String ERROR_MESSAGE_DIALOG_TITLE = "Error";
	
	private static final String PROJECT_NOT_OPEN_MESSAGE_DIALOG_ERROR = 
			"The selected project is not open. Please open it and then re-attempt your submission.";

	@Override
	/**
	 * This method executes when the click event occurs. It submits
	 * the project on which the "Submit Assessment" click took place 
	 * @param event the click event
	 * @return null
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// gets workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		// gets selection service
		ISelectionService service = window.getSelectionService();
		
		// gets structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();
		System.out.println("Selection: " + structured);
		System.out.println("Type of selection: " + structured.getFirstElement().getClass());
		
		Object proj = structured.getFirstElement();
		IJavaProject javaProject = null;
		ICProject cProject = null;
		if (proj instanceof IJavaProject) {
			javaProject = (IJavaProject)proj;
			if (!javaProject.isOpen()) {
				writeClosedProjectMessage(window);
				popupErrorDialog(window.getShell());
				return null;
			}
			submitAssessment(javaProject.getProject(), window);
		}
		else if (proj instanceof ICProject) {
			cProject = (ICProject)proj;
			if (!cProject.isOpen()) {
				writeClosedProjectMessage(window);
				popupErrorDialog(window.getShell());
				return null;
			}
			submitAssessment(cProject.getProject(), window);
		}
		else if (proj instanceof IProject){
			submitAssessment((IProject) proj, window);
		}
		return null;
	}
	
	/**
	 * Write message that project is closed to console
	 * @param window workbench window
	 */
	private void writeClosedProjectMessage(IWorkbenchWindow window) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMgr = plugin.getConsoleManager();
		MessageConsole console = new MessageConsole(Activator.SWAMP_PLUGIN_CONSOLE_NAME, null);
		conMgr.addConsoles(new IConsole[]{console});
		IWorkbenchPage page = window.getActivePage();
		try {
			IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			view.display(console);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		MessageConsoleStream stream = console.newMessageStream();
		stream.println(Utils.getBracketedTimestamp() + PROJECT_NOT_OPEN_CONSOLE_ERROR);
	}
	
	private void popupErrorDialog(Shell shell) {
		if (shell == null) {
			return;
		}
		MessageDialog.openError(shell, ERROR_MESSAGE_DIALOG_TITLE, PROJECT_NOT_OPEN_MESSAGE_DIALOG_ERROR);
	}
	
	/**
	 * Submit assessment for the specified project
	 * @param prj Eclipse project
	 * @param window workbench window
	 */
	private void submitAssessment(IProject prj, IWorkbenchWindow window) {
		SwampSubmitter ss = new SwampSubmitter(window);
		ss.launchBackgroundAssessment(prj);
	}
}
