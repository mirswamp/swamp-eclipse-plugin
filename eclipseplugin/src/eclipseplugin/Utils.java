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

public class Utils {

	public static String getCurrentTimestamp() {
		Timestamp timestamp = new Timestamp(new Date().getTime());
		String timeString = timestamp.toString();
		timeString = timeString.substring(0, timeString.length()-4);
		timeString = timeString.replace(" ","-");
		return timeString;
	}
	
	public static String getBracketedTimestamp() {
		return "[" + getCurrentTimestamp() + "] ";
	}
	
	public static String convertListToDelimitedString(List<String> list, String delimiter) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i) + delimiter);
		}
		return sb.toString();
	}
	
	public static List<String> convertDelimitedStringToList(String delimitedString, String delimiter) {
		String[] array = delimitedString.split(delimiter);
		List<String> list = new ArrayList<String>(array.length);
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	public static int[] convertIntListToArray(List<Integer> list) {
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public static List<Integer> convertIntArrayToList(int[] array) {
		int size = array.length;
		List<Integer> list = new ArrayList<Integer>(size);
		for (int i = 0; i < size; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
	public static String[] convertStringListToArray(List<String> list) {
		String[] ary = null;
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
}
