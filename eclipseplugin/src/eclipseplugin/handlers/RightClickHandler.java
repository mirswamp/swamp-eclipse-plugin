/*
 * Copyright 2016 Malcolm Reid Jr.
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

package eclipseplugin.handlers;

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

import eclipseplugin.SwampSubmitter;
import eclipseplugin.Utils;

import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RightClickHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// gets workbench window
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		// gets selection service
		ISelectionService service = window.getSelectionService();
		
		// gets structured selection
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();
		System.out.println("Selection: " + structured);
		System.out.println("Type of selection: " + structured.getFirstElement().getClass());
		
		if (!(structured.getFirstElement() instanceof IJavaProject)) {
			System.out.println("Somehow the selection is not a project");
			return null;
		}
		
		IJavaProject project = (IJavaProject)structured.getFirstElement();
		System.out.println("Project is open? " + project.isOpen());
		if (!project.isOpen()) {
			ConsolePlugin plugin = ConsolePlugin.getDefault();
			IConsoleManager conMgr = plugin.getConsoleManager();
			MessageConsole console = new MessageConsole("SWAMP Plugin", null);
			conMgr.addConsoles(new IConsole[]{console});
			IWorkbenchPage page = window.getActivePage();
			try {
				IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
				view.display(console);
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			MessageConsoleStream stream = console.newMessageStream();
			stream.println(Utils.getBracketedTimestamp() + "Error: Project is not open");
			return null;
		}
		
		SwampSubmitter ss = new SwampSubmitter(window);
		IProject prj = project.getProject();
		ss.launchBackgroundAssessment(prj);
		
		return null;
	}
}
