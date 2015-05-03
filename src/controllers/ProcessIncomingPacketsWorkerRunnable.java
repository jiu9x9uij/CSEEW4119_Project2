package controllers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import models.Settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Worker thread that processes incoming packets.
 */
public class ProcessIncomingPacketsWorkerRunnable implements Runnable{
	private String serverText;
	private DatagramSocket socket;
	private DatagramPacket packetRecieved;
	private InetAddress destinationAddress;
	private int destinationPort;
	private byte[] dataToSend = new byte[Settings.BUFFER_SIZE];

	public ProcessIncomingPacketsWorkerRunnable(DatagramSocket socket, DatagramPacket packetRecieved, String serverText) {
		this.serverText = serverText;
		this.socket = socket;
		this.packetRecieved = packetRecieved;
		this.destinationAddress = packetRecieved.getAddress();
		this.destinationPort = packetRecieved.getPort();
	}

    public void run() {
    	try {
    		/* Read packet content */
    		String packetContent = new String(packetRecieved.getData(), 0, packetRecieved.getLength());
    		System.out.println("### Packet Received " +  + System.currentTimeMillis() + " ###"); // DEBUG Request received stamp
    		System.out.println("packetContent = " + packetContent); // DEBUG packetContent
    		
    		/* Execute command */
			// Parse command
			JSONObject packetContentJSON = new JSONObject(packetContent);
			String command = packetContentJSON.getString("command");
			JSONObject body = packetContentJSON.getJSONObject("body");
			System.out.println("command = " + command); // DEBUG command type
			
			// Execute corresponding command
			String response = null; // TODO not sure if a response is necessary, keeping for debug purpose
			if (command.equals("capitalize")) {
				response = capitalize(body);
			}
			else {
				System.out.println("Command Not Supported!"); // DEBUG command not supported
			}
        	
        	/* Respond to command requester */
        	System.out.println("---response---\n" + response + "\n------end-----"); // DEBUG Response content
        	dataToSend = response.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, destinationAddress, destinationPort);
//			socket.send(sendPacket);
		} catch (JSONException e) {
			e.printStackTrace();
		}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
    	
    	System.out.println("######### Done #########"); // DEBUG Request processed
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
