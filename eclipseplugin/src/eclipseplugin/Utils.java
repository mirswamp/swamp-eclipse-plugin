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

package eclipseplugin;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class provides utility methods that provide basic
 * functionality.
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class Utils {

	/**
	 * Generates a timestamp without spaces for the current time 
	 *
	 * @return a timestamp for the current time
	 */
	public static String getCurrentTimestamp() {
		Timestamp timestamp = new Timestamp(new Date().getTime());
		String timeString = timestamp.toString();
		timeString = timeString.substring(0, timeString.length()-4);
		timeString = timeString.replace(" ","-");
		return timeString;
	}
	
	/**
	 * Generates a bracketed timestamp for the current time 
	 *
	 * @return a bracketed timestamp for the current time
	 */
	public static String getBracketedTimestamp() {
		return "[" + getCurrentTimestamp() + "] ";
	}
	
	/**
	 * Converts a List of Strings to a delimited String
	 *
	 * @param list the list of Strings
	 * @param delimiter the delimiter to be used between elements of
	 * the list
	 * @return the delimited String
	 */
	public static String convertListToDelimitedString(List<String> list, String delimiter) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < list.size()-1; i++) {
			sb.append(list.get(i) + delimiter);
		}
		sb.append(list.get(list.size()-1));
		return sb.toString();
	}
	
	/**
	 * Converts a delimited String into a List of Strings
	 *
	 * @param delimitedString the delimited String
	 * @param delimiter the String that delimits separate elements in the delimited String
	 * @return list of Strings
	 */
	public static List<String> convertDelimitedStringToList(String delimitedString, String delimiter) {
		String[] array = delimitedString.split(delimiter);
		List<String> list = new ArrayList<String>(array.length);
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	/**
	 * Converts a List of Integers into an array of ints
	 *
	 * @param list the list of Integer objects
	 * @return an array of ints
	 */
	public static int[] convertIntListToArray(List<Integer> list) {
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	/**
	 * Converts an array of ints into a List of Integers
	 *
	 * @param array the array of ints
	 * @return the List of Integer objects
	 */
	public static List<Integer> convertIntArrayToList(int[] array) {
		int size = array.length;
		List<Integer> list = new ArrayList<Integer>(size);
		for (int i = 0; i < size; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	/**
	 * Converts a List of Strings into an array of Strings
	 *
	 * @param list the list of String objects
	 * @return the array of String objects
	 */
	public static String[] convertStringListToArray(List<String> list) {
		String[] ary = null;
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
}
