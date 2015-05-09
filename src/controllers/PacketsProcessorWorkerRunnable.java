package controllers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import models.DistanceVector;
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
     * Received DV update from a neighbor, update in host this neighbor's DV and routing table
     * @param body
     */
    private synchronized void routeUpdate(JSONObject body) {
		try {
			Gson gson = new Gson();
			DistanceVector dv = gson.fromJson(body.toString(), DistanceVector.class);
			System.out.println("ROUTEUPDATE from " + dv.getSocketAddress()); // DEBUG packet
			HostLauncher.host.getNeighbors().get(dv.getSocketAddress()).updateTimestamp();
			HostLauncher.host.updateDV(dv);
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
