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

import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to implement logout from SWAMP using menu
 * @author reid-jr
 *
 */
public class LogoutHandler extends AbstractHandler {
	@Override
	/**
	 * This method logs the user out of the SWAMP
	 * @param event click event
	 * @throws ExecutionException
	 * @returns null
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Executed Logout Handler");
		SwampSubmitter ss = new SwampSubmitter(HandlerUtil.getActiveWorkbenchWindow(event));
		ss.logOutOfSwamp();	
		return null;
	}
}
