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
	
	private String[] convertStringListToArray(List<String> list) {
		String[] ary = null;
		if (list != null) {
			ary = new String[list.size()];
			list.toArray(ary);
		}
		return ary;
	}
}
