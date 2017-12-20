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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.continuousassurance.swamp.cli.SwampApiWrapper;
import org.continuousassurance.swamp.eclipse.ui.SwampPerspective;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "org.continuousassurance.swamp.eclipse"; //$NON-NLS-1$
	
	/**
	 * Hostname of SWAMP host
	 */
	private static String hostname;
	
	/**
	 * True if user is currently logged into SWAMP
	 */
	private static boolean loggedIn;

	/**
	 * The shared instance of the Activator
	 */
	private static Activator plugin;
	
	/**
	 * Default host (i.e. MIR SWAMP instance)
	 */
	private static final String DEFAULT_HOST = "https://www.mir-swamp.org";
	
	/**
	 * Name of file that stores host configuration information
	 */
	private static final String HOST_FILENAME = ".host";
	
	private static final String MARKER_PREFS_FILENAME = ".marker_preferences";
	
	/**
	 * Name of file that stores list of unfinished assessments
	 */
	private static final String UNFINISHED_ASSESS_FILENAME = ".unfinished_assess";
	
	/**
	 * Name of file that stores list of finished assessments
	 */
	private static final String FINISHED_ASSESS_FILENAME = ".finished_assess";
	
	/**
	 * StatusChecker job
	 */
	private static StatusChecker sc;
	
	/**
	 * Shared Controller instance
	 */
	//public static Controller controller;
	
	/**
	 * Name of console (i.e. this is what the Console instance is named when
	 * this plug-in makes one)
	 */
	public static final String SWAMP_PLUGIN_CONSOLE_NAME = "SWAMP Plugin";
	
	/**
	 * Default encoding for writing/reading to/from file
	 */
	public static final String ENCODING = "UTF-8";
	
	private static List<Pattern> patterns;
	
	private static List<String> icons;
	
	private static final String[] COLORS = {"red", "yellow", "green", "black", "blue", "orange",
			"purple", "gray", "white"};
	
	private static final String[] SHAPES = { "circle" };

	public static final String MARKER_SUFFIX = "-marker";
	
	public static final String MARKER_PREFIX = "eclipseplugin.";
	
	//private static final String SEPARATOR = System.getProperty("file.separator");
	
	private static final String SWAMP_SETTINGS_DIR_NAME = ".SWAMP_SETTINGS";
	
	private static final String SWAMP_SETTINGS_PATH = System.getProperty("user.home") + File.separator + SWAMP_SETTINGS_DIR_NAME; 
	/**
	 * The constructor
	 */
	public Activator() {
		//controller = new Controller();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
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
			InputStreamReader filereader = null;
			BufferedReader reader = null;
		
			try {
				filereader = new InputStreamReader(new FileInputStream(file), Activator.ENCODING);
				reader = new BufferedReader(filereader);
				String host = reader.readLine();
				reader.close();
				if ((host != null) && (!host.equals(""))) {
					api = new SwampApiWrapper();
					if (api != null) {
						hostname = host;
						setLoggedIn(api.restoreSession());
					}
				}
			} catch (Exception e) {
			}
		}
		
		getMarkerPreferences();
		
		// This is necessary because the perspective may already have been open
		// from last session
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
			if (window != null) {
				SwampPerspective.initializeFileChangeListener(window);
			}
		}
		
	}
	
	private void getMarkerPreferences() {
		String DEFAULT_REGEX = ".*:.*:.*";
		Pattern DEFAULT_PATTERN = Pattern.compile(DEFAULT_REGEX);
		String DEFAULT_COLOR = "black";
		String DEFAULT_SHAPE = "circle";
		String DASH = "-";
		String COMMENT_CHAR = "#";
		String SPACE_SEPARATOR = "  ";
		Set<String> VALID_COLORS = new HashSet<>();
		for (String color : COLORS) {
			VALID_COLORS.add(color);
		}
		
		patterns = new ArrayList<>();
		icons = new ArrayList<>();
		String markerPrefPath = getMarkerPrefsPath();
		File file = new File(markerPrefPath);
		if (file.exists()) {
			InputStreamReader filereader = null;
			BufferedReader reader = null;
			try {
				filereader = new InputStreamReader(new FileInputStream(file), Activator.ENCODING);
				reader = new BufferedReader(filereader);
				String ln = reader.readLine();
				while (ln != null) {
					if (!ln.contains(COMMENT_CHAR)) {
						String[] parts = ln.split(SPACE_SEPARATOR);
						if (parts.length >= 2) {
							String regex = parts[0];
							String figure = parts[1];
							String[] attr = figure.split(DASH);
							if (attr.length >= 2) {
								String color = attr[0];
								String shape = attr[1];
								if (isValidColor(color, VALID_COLORS) && isValidShape(shape)) {
									try {
										Pattern pattern = Pattern.compile(regex);
										patterns.add(pattern);
										icons.add(color.toLowerCase() + DASH + shape.toLowerCase());
									}
									catch (PatternSyntaxException e) {
									}
								}
							}
						}
					}
					ln = reader.readLine();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		patterns.add(DEFAULT_PATTERN);
		icons.add(DEFAULT_COLOR + DASH + DEFAULT_SHAPE);
	}
	
	private static boolean isValidShape(String shape) {
		for (String s : SHAPES) {
			if (s.equals(shape)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isValidColor(String color, Set<String> validColors) {
		if (color == null) {
			return false;
		}
		return validColors.contains(color.toLowerCase());
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
			e.printStackTrace();
		}
		
		// Write it to file here
		OutputStreamWriter filewriter = null;
		BufferedWriter writer = null;
		
		try {
			filewriter = new OutputStreamWriter(new FileOutputStream(f), Activator.ENCODING);
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
		return SwampApiWrapper.SWAMP_DIR_PATH +  HOST_FILENAME;
	}
	
	private static String getMarkerPrefsPath() {
		return SWAMP_SETTINGS_PATH + File.separator + MARKER_PREFS_FILENAME;
	}
	
	/**
	 * Gets path of unfinished assessments file
	 * @return path
	 */
	public static String getUnfinishedAssessmentsPath() {
		return ResultsUtils.getTopLevelResultsDirectory() + File.separator + UNFINISHED_ASSESS_FILENAME;
	}
	
	/**
	 * Gets path of finished assessments file
	 * @return path
	 */
	public static String getFinishedAssessmentsPath() {
		return ResultsUtils.getTopLevelResultsDirectory() + File.separator + FINISHED_ASSESS_FILENAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
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
	
	/**
	 * Getter for StatusChecker job
	 * @return StatusChecker, which is a background job that updates assessment
	 * statuses
	 */
	public static StatusChecker getStatusChecker() {
		return sc;
	}
	
	/**
	 * Gets installation's preferred marker type for this tool name, bug group,
	 * and severity
	 * @param toolName name of the tool the marker is for
	 * @param bugGroup group of bug marker is for
	 * @param bugSeverity severity of bug marker is for
	 * @return
	 */
	public static String getMarkerType(String toolName, String bugGroup, String bugSeverity) {
		String DELIMITER = ":";
		if (bugGroup == null){
			bugGroup = "";
		}
		String input = toolName + DELIMITER + bugGroup + DELIMITER + bugSeverity;
		System.out.println("Input: " + input);
		for (int i = 0; i < patterns.size(); i++) {
			if (patterns.get(i).matcher(input).matches()) {
				return MARKER_PREFIX + icons.get(i) + MARKER_SUFFIX;
			}
		}
		System.out.println("Pattern didn't match anything. Should never happen");
		return MARKER_PREFIX + icons.get(icons.size()-1) + MARKER_SUFFIX;
	}
	
	public static String[] getValidColors() {
		return COLORS;
	}
	
	public static String[] getValidShapes() {
		return SHAPES;
	}
	
}
