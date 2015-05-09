package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Utils {
	public static final DateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss:SSS");
	
	public static void println(String s) {
		System.out.println(s);
	}
	
	public static void print(String s) {
		System.out.print(s);
	}
	
}
