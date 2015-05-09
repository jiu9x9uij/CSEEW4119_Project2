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
import models.SettingsNOTUSED;
import models.Utils;

public class Host {
	private String address;
	private int port;
	private DatagramSocket socket;
	private ThreadPooledListener listener;
	private ThreadPooledSender sender;
	private Hashtable<String, Neighbor> neighbors;
	private ConcurrentLinkedQueue<DistanceVector> dvQueue;
	
	public Host(int port, Hashtable<String, Neighbor> directNeighbors) {
		try {
			this.address = InetAddress.getLocalHost().getHostName();///
			System.out.println("Host Address = " + address + "\n");///
			address = "127.0.0.1"; // TODO delete
			this.port = port;
			// Open the UDP socket for both sending and listening
			this.socket = new DatagramSocket(port);
			System.out.println("Host Port = " + port + "\n");///
			System.out.println("TIME_OUT = " + HostLauncher.TIME_OUT + "\n");///
			
			// Initialize the essentials for host
			this.neighbors = new Hashtable<String, Neighbor>(directNeighbors);
			for (Neighbor n: neighbors.values()) {
				System.out.println("Down?" + n.isDown());///
				System.out.println(n);///
			}
			System.out.println();///
			this.dvQueue = new ConcurrentLinkedQueue<DistanceVector>();
		} catch (SocketException e) {
			// Error occured when opening socket on specified port, exit program
			Utils.println("SocketException - Host constructor: " + e.getMessage());
			System.exit(0);
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
					showRoutingTable("");
				}
//				else if (command.startsWith("transfer")) transfer(command);
//				else if (command.startsWith("addproxy")) addProxy(command);
//				else if (command.startsWith("removeproxy")) removeProxy(command);
//				else if (command.equals("help")) listCommands();
				else Utils.println("ERROR: Invalid command. Type \"help\" to see list of commands.");
			}
		} catch (IOException e) {
			Utils.println("IOException - Config reading: " + e.getMessage());
		}
	}
	
	public String getAddress() {
		return address;
	}
	
	private void close() {
		sender.stop();
		listener.stop();
		System.exit(0);
	}
	
	private void linkDown(String command) {
		// TODO Auto-generated method stub
		System.out.println("LINKDOWN");///
		String parts[] = command.split(" ");
		String addressToKickOff = parts[0];
		String portToKickOff = parts[1];
		neighbors.get(addressToKickOff + ":" + portToKickOff).linkDown();
	}
	
	public synchronized void showRoutingTable(String leftPadding) {
		System.out.println(leftPadding + "====== Routing Table ======");///
		Date timestamp = new java.util.Date();
		Utils.println(leftPadding + timestamp.toString() + " Distance vector list is:");
		for (Neighbor n: neighbors.values()) {
			Utils.print(leftPadding + "Destination = " + n.getSocketAddress() + ", ");
			if (n.getCost() == Double.MAX_VALUE) {
				Utils.print("Cost = INF, ");
				Utils.println("Link = (nil)");
			} else {
				Utils.print("Cost = " + n.getCost() + ", ");
				Utils.println("Link = (" + n.getNextHop() + ")");
			}
		}
		System.out.println(leftPadding + "=========== End ===========");///
		
	}
	
	public Hashtable<String, Neighbor> getNeighbors() {
		return neighbors;
	}
	
	public void addNeighbor(Neighbor n) {
		neighbors.put(n.getSocketAddress(), n);
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
		long t = System.nanoTime();///
		System.out.println("\n### Updating Routing Table " + t + " ###");///
		showRoutingTable("");///
		boolean changed = false;
		
		for (Neighbor dest: neighbors.values()) {
			// Break links to offline hosts by setting cost via offline server to infinity
						if (neighbors.get(dest.getNextHop()).isDown()) {
							System.out.println("\tdest's NH is down");///
							dest.setCost(Double.MAX_VALUE);
						}
			
						// Force recomputing cost to offline hosts since its original next hop may not be able to reach it anymore
						if (dest.isDown() && !dest.getSocketAddress().equals(dest.getNextHop()) && neighbors.get(dest.getNextHop()).getDV() != null) {
							System.out.println("\tdest is down && dest's NH is not itself && NH's dv exists");///
							double currentCostFromNextHopToDest = neighbors.get(dest.getNextHop()).getDV().getCostTo(dest.getSocketAddress());
							if (dest.getCost() < currentCostFromNextHopToDest) {
								dest.setCost(Double.MAX_VALUE);
							}
						}
		}
		
		for (Neighbor dest: neighbors.values()) {
			System.out.println("dest = " + dest);///
			
			
			

			
			// Compute new next hops
			for (Neighbor potentialNextHop: neighbors.values()) {
				if(dest.equals(potentialNextHop) && potentialNextHop.getDV() != null && !potentialNextHop.isDown()) {
					System.out.println("\tpotentialNextHop = " + potentialNextHop.getDV());///
					double newCost = potentialNextHop.getDV().getCostTo(address + ":" + port);
					System.out.println("\t\tdest.cost = " + dest.getCost());///
					System.out.println("\t\tnewCost = " + newCost);///
					if (newCost < dest.getCost()) {
						dest.setCost(newCost);
						dest.setNextHop(potentialNextHop.getSocketAddress());
						changed = true;
					}
				} else if(!dest.equals(potentialNextHop) && potentialNextHop.getDV() != null  && !potentialNextHop.isDown()) {
					System.out.println("\tpotentialNextHop = " + potentialNextHop.getDV());///
					double newCost = potentialNextHop.getCost() + potentialNextHop.getDV().getCostTo(dest.getSocketAddress());
					System.out.println("\t\tdest.cost = " + dest.getCost());///
					System.out.println("\t\tnewCost = " + newCost);///
					if (newCost < dest.getCost()) {
						dest.setCost(newCost);
						dest.setNextHop(potentialNextHop.getSocketAddress());
						changed = true;
					}
				}	
			}
		}
		
		if (changed) {
			sender.dvChanged();
			showRoutingTable("");
		}
		System.out.println("############ Done " + t + " ############\n");///
	}
	
	public synchronized void notifyLinkDown(String socketAddressNewDown) {
		sender.notifyLinkDown(socketAddressNewDown);
	}
}
