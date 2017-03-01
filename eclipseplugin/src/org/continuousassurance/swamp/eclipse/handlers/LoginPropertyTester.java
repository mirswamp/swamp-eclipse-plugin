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

import org.continuousassurance.swamp.eclipse.Activator;
import org.eclipse.core.expressions.PropertyTester;

/**
 * Tests whether user is logged into SWAMP. This uses an Eclipse property to
 * determine whether the user is logged in. Does not require any clicks to be
 * activated
 * @author reid-jr
 *
 */
public class LoginPropertyTester extends PropertyTester {
	
	/**
	 * Property that is being checked
	 */
	public static final String PROPERTY_NAME = "loggedIn";
	
	/**
	 * Constructor for LoginPropertyTester
	 */
	public LoginPropertyTester() {
		System.out.println("Login property initialized");
	}

	@Override
	/**
	 * Test method for Property Tester. Basically just checks whether user is
	 * logged in
	 */
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return Activator.getLoggedIn();
	}

}
