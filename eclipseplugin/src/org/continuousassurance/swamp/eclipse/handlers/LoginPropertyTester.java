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
package org.continuousassurance.swamp.eclipse.handlers;

import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;

/**
 * Tests whether user is logged into SWAMP
 * @author reid-jr
 *
 */
public class LoginPropertyTester extends PropertyTester {
	
	public static final String PROPERTY_NAME = "loggedIn";
	
	public LoginPropertyTester() {
		System.out.println("Login property initialized");
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return Activator.getLoggedIn();
	}

}
