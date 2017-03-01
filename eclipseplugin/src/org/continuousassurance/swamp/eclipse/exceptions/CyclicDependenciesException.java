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
package org.continuousassurance.swamp.eclipse.exceptions;

/**
 * This class is an exception thrown when a project has cyclical dependencies
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 08/2016 
 */
public class CyclicDependenciesException extends RuntimeException {

	/**
	 * Serial version UID (see Java Serializable interface for details)
	 */
	private static final long serialVersionUID = 7949231262296631262L;

	/**
	 * Constructor for CyclicDependenciesException
	 *
	 * @param msg the error message for the exception
	 */
	public CyclicDependenciesException(String msg) {
		super(msg);
	}
	
}