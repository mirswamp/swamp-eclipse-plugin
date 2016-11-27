package org.continuousassurance.swamp.eclipse.handlers;

import org.continuousassurance.swamp.eclipse.Activator;
import org.continuousassurance.swamp.eclipse.SwampSubmitter;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;

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
