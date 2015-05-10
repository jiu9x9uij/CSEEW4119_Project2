package controllers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import models.DistanceVector;
import models.FileChunk;
import models.FileChunkComparator;
import models.Neighbor;
import models.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Worker thread that processes incoming packets.
 */
public class PacketsProcessorWorkerRunnable implements Runnable{
	private String serverText;
	private DatagramSocket socket;
	private DatagramPacket packetRecieved;
	private InetAddress destinationAddress;
	private int destinationPort;
	private byte[] dataToSend = new byte[HostLauncher.BUFFER_SIZE];

	public PacketsProcessorWorkerRunnable(DatagramSocket socket, DatagramPacket packetRecieved, String serverText) {
		this.serverText = serverText;
		this.socket = socket;
		this.packetRecieved = packetRecieved;
		this.destinationAddress = packetRecieved.getAddress();
		this.destinationPort = packetRecieved.getPort();
	}

    public void run() {
//    	System.out.println("### Packet Received " +  + System.currentTimeMillis() + " ###"); // DEBUG Request received stamp
		
    	try {
    		/* Read packet content */
    		String packetContent = new String(packetRecieved.getData(), 0, packetRecieved.getLength());
//    		System.out.println("packetContent = " + packetContent); // DEBUG packetContent
    		
    		/* Execute command */
			// Parse command
			JSONObject packetContentJSON = new JSONObject(packetContent);
			String command = packetContentJSON.getString("command");
			JSONObject body = packetContentJSON.getJSONObject("body");
//			System.out.println("command = " + command); // DEBUG command type
			
			// Execute corresponding command
			if (command.equals("routeUpdate")) {
				routeUpdate(body);
			} else if (command.equals("linkDown")) {
				linkDown(body);
			} else if (command.equals("transfer")) {
				transfer(body);
			} else if (command.equals("capitalize")) {
				System.out.println("---response---\n" + capitalize(body) + "\n------end-----");
			} else {
				System.out.println("Command Not Supported!"); // DEBUG command not supported
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
    	
//    	System.out.println("######### Done #########\n"); // DEBUG Request processed
    }

    /**
     * Received file chunk.
     * If this host is destination, pass to upper level.
     * Otherwise, forward packet.
     */
    private void transfer(JSONObject body) {
    	String destAddress = body.getString("address");
		int destPort = body.getInt("port");
		
		if (HostLauncher.host.getAddress().equals(destAddress)
				&& HostLauncher.host.getPort() == destPort) {
			// This host is destination, process packet
			int chunkIndex = body.getInt("index");
			int numOfChunks = body.getInt("numOfChunks");
			String fileName = body.getString("fileName");
			byte buffer[] = body.getString("buffer").getBytes();
			
			System.out.println("Received File chunk " + (chunkIndex+1) + " out of " + numOfChunks);///
			
			ArrayList<FileChunk> fileBufferList = HostLauncher.host.getFileBuffer().get(fileName);
			if (fileBufferList == null) {
				System.out.println(" new fileBufferList for file " + fileName);///
				fileBufferList = new ArrayList<FileChunk>();
				HostLauncher.host.getFileBuffer().put(fileName, fileBufferList);
			}
			
			fileBufferList.add(new FileChunk(chunkIndex, buffer));
			
			System.out.println(" fileBufferList.size() = " +  fileBufferList.size());///
			if (fileBufferList.size() == numOfChunks) {
				Collections.sort(fileBufferList, new FileChunkComparator());;
				try {
					FileOutputStream fos = new FileOutputStream(fileName);
					for (FileChunk fc: HostLauncher.host.getFileBuffer().get(fileName)) {
						System.out.println("Outputing chunk " + fc.getIndex());///
						fos.write(fc.getBuffer());
					}
					fos.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			// Forward packet
			System.out.println("Forward packet to " + destAddress + ":" + destPort);///
			String nextHopSocketAddress = HostLauncher.host.getNeighbors().get(destAddress + ":" + destPort).getNextHop();
			String parts[] = nextHopSocketAddress.split(":");
			String nextHopAddress = parts[0];
			int nextHopPort = Integer.parseInt(parts[1]);
			
			JSONObject packet = new JSONObject();
			packet.put("command", "transfer");
			packet.put("body", body);
			
			new PacketSenderWorkerRunnable(socket, packet, nextHopAddress, nextHopPort, "Forward file packet").run();
		}
	}

	/**
     * Received DV update from a neighbor, update in host this neighbor's DV and routing table
     * @param body
     */
    private synchronized void routeUpdate(JSONObject body) {
		try {
			Gson gson = new Gson();
			DistanceVector dv = gson.fromJson(body.toString(), DistanceVector.class);
			System.out.println("ROUTEUPDATE from " + dv.getSocketAddress()); // DEBUG packet
			Neighbor neighbor = HostLauncher.host.getNeighbors().get(dv.getSocketAddress());
			if (neighbor != null) {
				HostLauncher.host.getNeighbors().get(dv.getSocketAddress()).updateTimestamp();
				HostLauncher.host.updateDV(dv);
			}
		} catch (JSONException e) {
			Utils.println("JSONException in routeUpdate(): " + e.getMessage());
		} catch (JsonSyntaxException e) {
			Utils.println("JsonSyntaxException in routeUpdate(): " + e.getMessage());
		}
		
		
	}
    
    /**
     * Received linkDown notification from a neighbor, 
     * update linkDown info for specified host.
     * @param body
     */
    private synchronized void linkDown(JSONObject body) {
    	String socketAddressNewDown = body.getString("socketAddress");
    	System.out.println("LINKDOWN for " + socketAddressNewDown); // DEBUG packet
		Neighbor hostNewDown = HostLauncher.host.getNeighbors().get(socketAddressNewDown);
    	if (hostNewDown != null && !hostNewDown.isDown()) {
    		System.out.print("[notification] ");///
    		hostNewDown.linkDown();
		}
	}

	/**
     * TEST Capitalize incoming message and echo it.
     * @param body
     * @return
     */
	private String capitalize(JSONObject body) {
		String msg = body.getString("msg");
		return msg.toUpperCase();
	}
}
