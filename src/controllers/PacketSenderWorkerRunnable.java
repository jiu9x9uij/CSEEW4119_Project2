package controllers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import models.SettingsNOTUSED;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Worker thread that processes incoming packets.
 */
public class PacketSenderWorkerRunnable implements Runnable{
	private String serverText;
	private DatagramSocket socket;
	private JSONObject packetContentJSON;
	private String destinationAddress;
	private int destinationPort;
	private byte[] dataToSend = new byte[HostLauncher.BUFFER_SIZE];

	public PacketSenderWorkerRunnable(DatagramSocket socket, JSONObject packetContentJSON, String destinationAddress, int destinationPort, String serverText) {
		this.serverText = serverText;
		this.socket = socket;
		this.packetContentJSON = packetContentJSON;
		this.destinationAddress = destinationAddress;
		this.destinationPort = destinationPort;
	}

    public void run() {
//    	System.out.println("### Sending Packet " +  + System.currentTimeMillis() + " ###"); // DEBUG Request received stamp
		
    	try {
    		System.out.println("packetContent = " + packetContentJSON); // DEBUG packetContentJSON
//    		System.out.println("\tSending dv to " + destinationAddress + ":" + destinationPort + " " + packetContentJSON); // DEBUG destinationAddress
        	
			dataToSend = packetContentJSON.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(destinationAddress), destinationPort);
			socket.send(sendPacket);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
//    	System.out.println("######### Done #########\n"); // DEBUG Request processed
    }
}
