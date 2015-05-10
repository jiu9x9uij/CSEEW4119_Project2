package controllers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

import models.DistanceVector;
import models.FileChunk;
import models.Neighbor;
import models.Utils;

public class Host {
	private String address;
	private int port;
	private DatagramSocket socket;
	private ThreadPooledListener listener;
	private ThreadPooledSender sender;
	private Hashtable<String, Neighbor> neighbors;
	private Hashtable<String, ArrayList<FileChunk>> fileBuffer;
	private ConcurrentLinkedQueue<DistanceVector> dvQueue;
	
	public Host(int port, Hashtable<String, Neighbor> directNeighbors) {
		try {
			this.address = InetAddress.getLocalHost().getHostAddress();///
			System.out.println("Host Address = " + address);///
//			address = "127.0.0.1"; // TODO delete
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
			this.fileBuffer = new Hashtable<String, ArrayList<FileChunk>>();
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
				
				if (command.toLowerCase().startsWith("showrt")) {
					showRoutingTable("");
				} else if (command.toLowerCase().equals("changecost")) {
					changeCost(command);
				} else if (command.toLowerCase().startsWith("linkdown")) {
					linkDown(command);
				} else if (command.toLowerCase().startsWith("linkup")) {
					linkUp(command);
				} else if (command.toLowerCase().startsWith("transfer")) {
					transfer(command);
				} else if (command.equals("help")) {
					listCommands();
				} else {
					Utils.println("ERROR: Invalid command. Type \"help\" to see list of commands.");
				}
			}
		} catch (IOException e) {
			Utils.println("IOException - Config reading: " + e.getMessage());
		}
	}
	
	private void changeCost(String command) {
		String parts[] = command.split(" ");
		String addressToChange = parts[1];
		String portToChange = parts[2];
		
//		for (Neighbor n: neighbors.values()) {
//			if (n.isDirectNeighbor())
//		}
	}

	private void transfer(String command) {
		String parts[] = command.split(" ");
		String fileName = parts[1];
		String destinationAddress = parts[2];
		String destinationPort = parts[3];
		
		final int CHUNK_SIZE = 2048;
		try {
			String nextHopSocketAddress = neighbors.get(destinationAddress + ":" + destinationPort).getNextHop();
			parts = nextHopSocketAddress.split(":");
			String nextHopAddress = parts[0];
			int nextHopPort = Integer.parseInt(parts[1]);
			
			fileName = "test.jpg";// TODO delete
			FileInputStream in = new FileInputStream(fileName);
			
			FileOutputStream fos;
			fos = new FileOutputStream("output.jpg");
			
			int chunkIndex = 0;
			int bytesRead = 0;
			byte[] buffer = new byte[CHUNK_SIZE];
			int numOfChunks = (int)Math.ceil(in.getChannel().size()/(double)CHUNK_SIZE);
			System.out.println("*  channelSize = " + in.getChannel().size());///
			System.out.println("*  numOfChunks = " + numOfChunks);///
			
			while ((bytesRead = in.read(buffer, 0, CHUNK_SIZE)) != -1)
			{
				fos.write(buffer);
				boolean isLast = false;
				JSONObject fileChunkPacket = buildFileChunkPacket(fileName, numOfChunks, chunkIndex, buffer, destinationAddress, Integer.parseInt(destinationPort));
				new PacketSenderWorkerRunnable(socket, fileChunkPacket, nextHopAddress, nextHopPort, "Send file chunk").run();
				System.out.println("File chunk " + chunkIndex + " sent");///
				chunkIndex++;
			}
			
			in.close();
			
			fos.close(); // TODO
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			Utils.println("NumberFormatException - file transfer port: " + e.getMessage());
		}
		
	}

	private JSONObject buildFileChunkPacket(String fileName, int numOfChunks, int index, byte[] buffer, String destinationAddress, int destinationPort) {
		JSONObject packet = new JSONObject();
		
		packet.put("command", "transfer");
		JSONObject body = new JSONObject();
		body.put("address", destinationAddress);
		body.put("port", destinationPort);
		body.put("fileName", "outputReceived.jpg");// TODO use fileName
		body.put("numOfChunks", numOfChunks);
		body.put("index", index);
		body.put("buffer", buffer.toString());
		packet.put("body", body);
		
		return packet;
	}

	private void linkUp(String command) {
		// TODO Auto-generated method stub
		
	}

	private void listCommands() {
		Utils.println("Supported Commands: ");
		Utils.println("	CLOSE");
		Utils.println("	SHOWRT");
		Utils.println("	CHANGECOST  <IP> <Port> <Cost>"); // TODO not implemented
		Utils.println("	LINKDOWN <IP> <Port>"); // TODO not stable
		Utils.println("	LINKUP <IP> <Port>"); // TODO not implemented
		Utils.println("	TRANSFER <filename> <DestinationIP> <Port>"); // TODO not implemented
	}

	public String getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	private void close() {
		sender.stop();
		listener.stop();
		System.exit(0);
	}
	
	private void linkDown(String command) {
		String parts[] = command.split(" ");
		String addressToKickOff = parts[1];
		String portToKickOff = parts[2];
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
	
	public Hashtable<String, ArrayList<FileChunk>> getFileBuffer() {
		return fileBuffer;
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
		
		// Discover new neighbors based on each current neighbor's DV
//		System.out.println("1. Descover new neighbors...");///
		Hashtable<String, Neighbor> newNeighbors = new Hashtable<String, Neighbor>();
		for (Neighbor n: neighbors.values()) {
//			System.out.println("n = " + n);///
			if (n.getDV() != null && !n.isDown()) {
				for (String dest: n.getDV().getCosts().keySet()) {
//					System.out.println("\tpotentialDest = " + dest);///
					// Discovered a new reachable host that's not this host
					if (!dest.equals(address + ":" + port) && neighbors.get(dest) == null) {
//						System.out.println("\t\tn.cost= " + n.getCost());///
//						System.out.println("\t\tnToDext.cost()= " + n.getDV().getCostTo(dest));///
						Neighbor newNeighbor = new Neighbor(dest, n.getCost() + n.getDV().getCostTo(dest), false);
						// The next hop to this new neighbor is the current neighbor n
						newNeighbor.setNextHop(n.getSocketAddress());
						newNeighbors.put(dest, newNeighbor);
					}
				}
			}
		}
		neighbors.putAll(newNeighbors);
		
		// Reset cost involving offline hosts
//		System.out.println("2. Reset cost involving offline hosts...");///
		for (Neighbor dest: neighbors.values()) {
//			System.out.println("dest = " + dest);///
			// Break links to offline hosts by setting cost via offline server to infinity
			if (neighbors.get(dest.getNextHop()).isDown()) {
//				System.out.println("\tdest's NH is down");///
				dest.setCost(Double.MAX_VALUE);
			}
			
			// Force recomputing cost to offline hosts since its original next hop may not be able to reach it anymore
			if (dest.isDown() && !dest.getSocketAddress().equals(dest.getNextHop()) && neighbors.get(dest.getNextHop()).getDV() != null) {
//				System.out.println("\tdest is down && dest's NH is not itself && NH's dv exists");///
				double currentCostFromNextHopToDest = neighbors.get(dest.getNextHop()).getDV().getCostTo(dest.getSocketAddress());
				if (dest.getCost() < currentCostFromNextHopToDest) {
					dest.setCost(Double.MAX_VALUE);
				}
			}
//			System.out.println("\tdest = " + dest);///
		}
		
		// Recompute routing table
//		System.out.println("3. Recompute routing table...");///
		for (Neighbor dest: neighbors.values()) {
//			System.out.println("dest = " + dest);///
			// Compute new next hops
			for (Neighbor potentialNextHop: neighbors.values()) {
				if(dest.equals(potentialNextHop) && potentialNextHop.getDV() != null && !potentialNextHop.isDown()) {
//					System.out.println("\tpotentialNextHop = " + potentialNextHop.getDV());///
					double newCost = potentialNextHop.getDV().getCostTo(address + ":" + port);
//					System.out.println("\t\tdest.cost = " + dest.getCost());///
//					System.out.println("\t\tnewCost = " + newCost);///
					if (newCost < dest.getCost()) {
						dest.setCost(newCost);
						dest.setNextHop(potentialNextHop.getSocketAddress());
						changed = true;
					}
				} else if(!dest.equals(potentialNextHop) && potentialNextHop.getDV() != null  && !potentialNextHop.isDown()) {
//					System.out.println("\tpotentialNextHop = " + potentialNextHop.getDV());///
					double costFromPNHToDest = potentialNextHop.getDV().getCostTo(dest.getSocketAddress());
					double newCost = potentialNextHop.getCost() + costFromPNHToDest;
//					System.out.println("\t\tdest.cost = " + dest.getCost());///
//					System.out.println("\t\tnewCost = " + newCost);///
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
