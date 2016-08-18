package eclipseplugin.exceptions;

/**
 * This class is an exception thrown when a project has cyclical dependencies
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 08/2016 
 */
public class CyclicDependenciesException extends RuntimeException {

	/**
	 * Constructor for CyclicDependenciesException
	 *
	 * @param msg the error message for the exception
	 */
	public CyclicDependenciesException(String msg) {
		super(msg);
	}
	
}