package eclipseplugin.exceptions;

public class CyclicDependenciesException extends RuntimeException {

	public CyclicDependenciesException(String msg) {
		super(msg);
	}
	
}