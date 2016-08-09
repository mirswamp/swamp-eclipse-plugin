package eclipseplugin.handlers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;

import eclipseplugin.SwampSubmitter;

public class LoginPropertyTester extends PropertyTester {
	
	public static final String PROPERTY_NAME = "loggedIn";
	
	SwampSubmitter ss;
	public LoginPropertyTester() {
		System.out.println("Login property initialized");
		ss = new SwampSubmitter(PlatformUI.getWorkbench().getActiveWorkbenchWindow()); 
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		System.out.println("Login property tested");
		return ss.loggedIntoSwamp();
	}

}
