package controllers.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import models.Utils;

public class UDPClient {

	public static void main(String[] args) {
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] dataSend = new byte[1024];
			byte[] dataReceive = new byte[1024];
			
			/* Get user input */
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
//			String sentence =  inFromUser.readLine();
			String sentence = "{\"command\":capitalize, \"body\":{\"msg\":\"ahaha\"}}";
			System.out.println(sentence);///
			
			/* Send to server */
			dataSend = sentence.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(dataSend, dataSend.length, IPAddress, 9876);
			clientSocket.send(sendPacket);
			
			/* Print server response */
			DatagramPacket receivePacket = new DatagramPacket(dataReceive, dataReceive.length);
			clientSocket.receive(receivePacket);
			String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			Utils.println("FROM SERVER:" + modifiedSentence);
			
			clientSocket.close();
			
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
