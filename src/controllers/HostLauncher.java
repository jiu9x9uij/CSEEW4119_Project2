package controllers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

import models.Neighbor;
import models.SettingsNOTUSED;
import models.Utils;


public class HostLauncher {
	public static Host host;
	public static final int BUFFER_SIZE = 1024;
	public static long TIME_OUT = 30; // In seconds
	
	public static void main(String[] args) {
		
		/* Check argument */
		if (args.length != 1) {
			Utils.println("ERROR: Wrong argument format. Please follow the instructions in README.");
			System.exit(0);
		}
		
		int port;
		Hashtable<String, Neighbor> directNeighbors = new Hashtable<String, Neighbor>();
		String line;
		String words[];
		FileInputStream fileInputStream;
		try {
			/* Config host from file input */
			fileInputStream = new FileInputStream(args[0]);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
			
			// Read host parameters from first line
			line = bufferedReader.readLine();
			words = line.split(" ");
			port = Integer.parseInt(words[0]);
			TIME_OUT = Integer.parseInt(words[1]);
			
			// Read neighbor information from the rest
			while ((line = bufferedReader.readLine()) != null) {
				words = line.split(" ");
				Neighbor neighbor = new Neighbor(words[0], Double.parseDouble(words[1]), true);
				directNeighbors.put(neighbor.getSocketAddress(), neighbor);
			}
			
			bufferedReader.close();
			
			
			/* Start host */
			host = new Host(port, directNeighbors);
			host.start();
			
		} catch (FileNotFoundException e) {
			Utils.println("ERROR: Cannot open file. Please make sure the file is put under correct dir following README.");
		} catch (IOException e) {
			Utils.println("ERROR: Failure occurred while reading file.");
		} catch (NumberFormatException e) {
			Utils.println("ERROR: Config file format is wrong.");
		}
		
		
	}
}
