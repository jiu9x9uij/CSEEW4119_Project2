package controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import models.DistanceVector;
import models.Neighbor;
import models.Utils;

public class Host {
	private DatagramSocket socket;
	private ThreadPooledListener listener;
	private ThreadPooledSender sender;
	private Hashtable<String, Neighbor> neighbors;
	private ConcurrentLinkedQueue<DistanceVector> DVQueue;
	
	public Host(int port, Hashtable<String, Neighbor> directNeighbors) {
		try {
			// Open the UDP socket for both sending and listening
			this.socket = new DatagramSocket(port);
			System.out.println("getLocalAddress() = " + socket.getLocalAddress());///
			System.out.println("getLocalSocketAddress() = " + socket.getLocalSocketAddress());///
			
			// Start sender to maintain time-outs and DV sending in a thread pool
			sender = new ThreadPooledSender(socket);
			new Thread(sender).start();
			
			// Start listener to listen to incoming messages in a thread pool
			listener = new ThreadPooledListener(socket);
			new Thread(listener).start();
			
			// Initialize the essentials for host
			this.neighbors = directNeighbors;
			this.DVQueue = new ConcurrentLinkedQueue<DistanceVector>();
			
			listenToCommand();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private void listenToCommand() {
		try {
			BufferedReader input;
			String command;
			input = new BufferedReader (new InputStreamReader(System.in));
			
			while (true) {
				command = input.readLine();
				if (command.toLowerCase().equals("close")) {
					close();
					break;
				}
				
				if (command.toLowerCase().startsWith("linkdown")) {
					linkDown(command);
				}
//				else if (command.toLowerCase().startsWith("linkup")) linkUp(command);
//				else if (command.equals("changecost")) changeCost();
//				else if (command.startsWith("showrt")) showRT(command);
//				else if (command.startsWith("transfer")) transfer(command);
//				else if (command.startsWith("addproxy")) addProxy(command);
//				else if (command.startsWith("removeproxy")) removeProxy(command);
//				else if (command.equals("help")) listCommands();
				else Utils.println("ERROR: Invalid command. Type \"help\" to see list of commands.");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void close() {
		sender.stop();
		listener.stop();
		System.exit(0);
	}
	
	private void linkDown(String command) {
		// TODO Auto-generated method stub
		System.out.println("LINKDOWN");///
	}
	
	public Hashtable<String, Neighbor> getNeighbors() {
		return neighbors;
	}
	
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getSocketAddress(), n);
	}
	
	public Queue<DistanceVector> getDVQueue() {
		return DVQueue;
	}
}
