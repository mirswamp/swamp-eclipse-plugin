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


import org.continuousassurance.swamp.eclipse.dialogs.AboutSWAMPDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for showing AboutSWAMP dialog (which has information about the SWAMP
 * and this plug-in)
 * @author reid-jr
 *
 */
public class AboutSwampHandler extends AbstractHandler {

		@Override
		/**
		 * Opens AboutSWAMP dialog
		 * @param event click event
		 * @return null
		 */
		public Object execute(ExecutionEvent event) throws ExecutionException {
			// gets workbench window
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			if (window != null) {
				AboutSWAMPDialog asd = new AboutSWAMPDialog(window.getShell());
				asd.open();
			}
			return null;
		}
}
