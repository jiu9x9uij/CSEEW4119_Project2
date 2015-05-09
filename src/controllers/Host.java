package controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
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
	private ConcurrentLinkedQueue<DistanceVector> dvQueue;
	
	public Host(int port, Hashtable<String, Neighbor> directNeighbors) {
		try {
			// Open the UDP socket for both sending and listening
			this.socket = new DatagramSocket(port);
			System.out.println("Host Address = " + InetAddress.getLocalHost().getHostAddress() + ":" + port + "\n");///
			
			// Initialize the essentials for host
			this.neighbors = new Hashtable<String, Neighbor>(directNeighbors);
			this.dvQueue = new ConcurrentLinkedQueue<DistanceVector>();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void start() {
		// Start sender to maintain time-outs and DV sending in a thread pool
		sender = new ThreadPooledSender(socket);
		new Thread(sender).start();
					
		// Start listener to listen to incoming messages in a thread pool
		listener = new ThreadPooledListener(socket);
		new Thread(listener).start();
		
		listenToCommand();
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
				else if (command.startsWith("showrt")) {
					showRoutingTable();
				}
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
	
	private void showRoutingTable() {
		Date timestamp = new java.util.Date();
		Utils.println(timestamp.toString() + " Distance vector list is:");
		for (Neighbor n: neighbors.values()) {
			Utils.print("Destination = " + n.getSocketAddress() + ", ");
			Utils.print("Cost = " + n.getCost() + ", ");
			Utils.println("Link = (" + n.getNextHop() + ")");
		}
	}
	
	public Hashtable<String, Neighbor> getNeighbors() {
		return neighbors;
	}
	
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getSocketAddress(), n);
	}
	
	public Queue<DistanceVector> getDVQueue() {// TODO not used
		return dvQueue;
	}
	
	/**
	 * To be called by packet processor worker thread
	 * when receiving a new DV from a neighbor.
	 * Update DV for that neighbor and recalculate routing table.
	 * @param dv
	 */
	public synchronized void updateDV(DistanceVector dv) {
		String socketAddress = dv.getSocketAddress();
		// If dv of this dv's host is changed, update its dv and this host's routing table
		if (!dv.equals(neighbors.get(socketAddress).getDV())) {
			System.out.println("Recieved new dv from " + socketAddress + " = " + dv);///
			neighbors.get(socketAddress).setDV(dv);
			updateRoutingTable();
		}
		
	}

	/**
	 * Recalculate routing table according to current DVs.
	 */
	public synchronized void updateRoutingTable() {
		System.out.println("\n=== Updating Routing Table ===");///
		boolean changed = false;
		
		for (Neighbor y: neighbors.values()) {
			/* TODO 
			 * 第一部分还差bellman-ford。
			 * 在考虑要不要把host的dv存成host的一个member。现在是在sender里动态生成的。
			 * 气氛上，现在不需要每次有新的dv来都重新算dv，但实际上似乎是顺便的。
			 */
			System.out.println("y = " + y);///
			for (Neighbor v: neighbors.values()) {
				if(!y.equals(v) && v.getDV() != null) {
					System.out.println("\tv = " + v.getDV());///
					double newCost = v.getCost() + v.getDV().getCostTo(y.getSocketAddress());
					if (newCost < y.getCost()) {
						y.setCost(newCost);
						y.setNextHop(v.getSocketAddress());
						changed = true;
					}
				}	
			}
		}
		
		if (changed) {
			sender.dvChanged();
			showRoutingTable();
		}
		System.out.println("============ Done ============\n");///
	}
}
