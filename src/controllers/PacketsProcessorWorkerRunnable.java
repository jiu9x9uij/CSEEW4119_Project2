package controllers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import models.DistanceVector;
import models.Settings;
import models.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Worker thread that processes incoming packets.
 */
public class PacketsProcessorWorkerRunnable implements Runnable{
	private String serverText;
	private DatagramSocket socket;
	private DatagramPacket packetRecieved;
	private InetAddress destinationAddress;
	private int destinationPort;
	private byte[] dataToSend = new byte[Settings.BUFFER_SIZE];

	public PacketsProcessorWorkerRunnable(DatagramSocket socket, DatagramPacket packetRecieved, String serverText) {
		this.serverText = serverText;
		this.socket = socket;
		this.packetRecieved = packetRecieved;
		this.destinationAddress = packetRecieved.getAddress();
		this.destinationPort = packetRecieved.getPort();
	}

    public void run() {
    	System.out.println("### Packet Received " +  + System.currentTimeMillis() + " ###"); // DEBUG Request received stamp
		
    	try {
    		/* Read packet content */
    		String packetContent = new String(packetRecieved.getData(), 0, packetRecieved.getLength());
    		System.out.println("packetContent = " + packetContent); // DEBUG packetContent
    		
    		/* Execute command */
			// Parse command
			JSONObject packetContentJSON = new JSONObject(packetContent);
			String command = packetContentJSON.getString("command");
			JSONObject body = packetContentJSON.getJSONObject("body");
			System.out.println("command = " + command); // DEBUG command type
			
			// Execute corresponding command
			if (command.equals("routeUpdate")) {
				routeUpdate(body);
			}
			else if (command.equals("capitalize")) {
				System.out.println("---response---\n" + capitalize(body) + "\n------end-----");
			}
			else {
				System.out.println("Command Not Supported!"); // DEBUG command not supported
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
    	
    	System.out.println("######### Done #########"); // DEBUG Request processed
    }

    /**
     * Received new DV from neighbor, add to host dvQueue for processing
     * @param body
     */
    private void routeUpdate(JSONObject body) {
		try {
			// TODO need change
			String address = body.getString("address");
			int port = body.getInt("port");
			HostLauncher.host.getDVQueue().add(new DistanceVector(address + ":" + port));
		} catch (JSONException e) {
			Utils.println("JSONException in routeUpdate(): " + e.getMessage());
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
