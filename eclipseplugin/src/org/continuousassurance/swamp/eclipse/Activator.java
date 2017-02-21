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

package org.continuousassurance.swamp.eclipse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.wisc.cs.swamp.SwampApiWrapper;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.continuousassurance.swamp.eclipse"; //$NON-NLS-1$
	
	// Hostname of SWAMP host
	private static String hostname;
	
	// Logged into SWAMP
	private static boolean loggedIn;

	// The shared instance
	private static Activator plugin;
	
	// Default host
	private static final String DEFAULT_HOST = "https://www.mir-swamp.org";
	
	// Name of file that stores host
	private static final String HOST_FILENAME = ".host";
	
	// Name of file that stores list of unfinished assessments
	private static final String UNFINISHED_ASSESS_FILENAME = ".unfinished_assess";
	
	// Name of file that stores list of finished assessments
	private static final String FINISHED_ASSESS_FILENAME = ".finished_assess";
	
	private static StatusChecker sc;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		// (1) get last hostname from file
		String hostnamePath = getHostnamePath();
		File file = new File(hostnamePath);
		SwampApiWrapper api;
		setLoggedIn(false);
		hostname = DEFAULT_HOST;

		if (file.exists()) {
			FileReader filereader = null;
			BufferedReader reader = null;
		
			try {
				filereader = new FileReader(file);
				reader = new BufferedReader(filereader);
				String host = reader.readLine();
				reader.close();
				if ((host != null) && (!host.equals(""))) {
					api = new SwampApiWrapper(SwampApiWrapper.HostType.CUSTOM, host);
					if (api != null) {
						hostname = host;
						setLoggedIn(api.restoreSession());
					}
				}
			} catch (Exception e) {
			}
		}
		
	}
	
	/**
	 * Sets hostname for SWAMP instance
	 * @param name hostname
	 */
	public static void setHostname(String name) {
		hostname = name;
		File f = new File(getHostnamePath());
		if (f.exists()) {
			f.delete();
		}
		
		try {
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Write it to file here
		FileWriter filewriter = null;
		BufferedWriter writer = null;
		
		try {
			filewriter = new FileWriter(f);
			writer = new BufferedWriter(filewriter);
			writer.write(name);
			writer.close();
		}
		catch (Exception e) {
			System.err.println("Unable to serialize file");
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets path of hostname file
	 * @return path
	 */
	private static String getHostnamePath() {
		return SwampApiWrapper.SWAMP_DIR_PATH + System.getProperty("file.separator") + HOST_FILENAME;
	}
	
	/**
	 * Gets path of unfinished assessments file
	 * @return path
	 */
	public static String getUnfinishedAssessmentsPath() {
		return SwampApiWrapper.SWAMP_DIR_PATH + UNFINISHED_ASSESS_FILENAME;
	}
	
	/**
	 * Gets path of finished assessments file
	 * @return path
	 */
	public static String getFinishedAssessmentsPath() {
		return SwampApiWrapper.SWAMP_DIR_PATH +  FINISHED_ASSESS_FILENAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if (sc != null) {
			sc.cancel();
		}
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/**
	 * Returns whether the current user is logged into SWAMP
	 * @return true if user is logged in
	 */
	public static boolean getLoggedIn() {
		return loggedIn;
	}
	
	/**
	 * Setter for loggedIn
	 * @param loggedIn true if user is logged into SWAMP
	 */
	public static void setLoggedIn(boolean loggedIn) {
		if ((sc != null) && !loggedIn) {
			sc.cancel();
			sc = null;
		}
		else if ((sc == null) && loggedIn) {
			sc = new StatusChecker();
			sc.schedule();
		}
		Activator.loggedIn = loggedIn;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	
	/**
	 * Getter for most recent hostname 
	 * @return most recent hostname
	 */
	public static String getLastHostname() {
		return hostname;
	}
	
	public static StatusChecker getStatusChecker() {
		return sc;
	}
	
}
