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
public class DVSenderWorkerRunnable implements Runnable{
	private String serverText;
	private DatagramSocket socket;
	private InetAddress destinationAddress;
	private int destinationPort;
	private byte[] dataToSend = new byte[Settings.BUFFER_SIZE];

	public DVSenderWorkerRunnable(DatagramSocket socket,/* TODO DV ,*/ InetAddress destinationAddress, int destinationPort, String serverText) {
		this.serverText = serverText;
		this.socket = socket;
		this.destinationAddress = destinationAddress;
		this.destinationPort = destinationPort;
	}

    public void run() {
    	try {
    		System.out.println("### Sending Packet " +  + System.currentTimeMillis() + " ###"); // DEBUG Request received stamp
			
        	/* TODO Build DV packet*/
    		JSONObject packetContentJSON = new JSONObject();
    		packetContentJSON.put("command", "routeUpdate");
    		JSONObject body = new JSONObject();
    		body.put("address", socket.getLocalPort()); // TODO probably has problem, need test on CLIC
    		body.put("port", socket.getLocalPort());
    		body.put("distanceVector", "DV"); // TODO design DV
    		packetContentJSON.put("body", body);
    		System.out.println("packetContent = " + packetContentJSON); // DEBUG packetContentJSON
        	dataToSend = packetContentJSON.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, destinationAddress, destinationPort);
			socket.send(sendPacket);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	System.out.println("######### Done #########"); // DEBUG Request processed
    }
}
