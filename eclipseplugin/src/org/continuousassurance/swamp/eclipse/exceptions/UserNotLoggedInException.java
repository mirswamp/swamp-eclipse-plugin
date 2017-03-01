/*
 * Copyright 2017 Malcolm Reid Jr.
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
 * Exception for when plug-in tries to do an action when user is logged in that
 * should only be done when a user is logged in 
 * @author reid-jr
 *
 */
public class UserNotLoggedInException extends Exception {

	/**
	 * Serial version UID (see Java Serializable interface for details)
	 */
	private static final long serialVersionUID = -8770059401215592649L;

}
