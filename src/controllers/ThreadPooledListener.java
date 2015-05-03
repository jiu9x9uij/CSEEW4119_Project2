package controllers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.Settings;

import org.json.JSONException;
import org.json.JSONObject;

public class ThreadPooledListener implements Runnable{
	protected DatagramSocket socket;
	protected boolean isStopped = false;
	protected Thread runningThread= null;
	protected ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public ThreadPooledListener(DatagramSocket socket) {
		this.socket = socket;
	}

	public void run(){
        synchronized(this){
            runningThread = Thread.currentThread();
        }
        
        /* Listen to incoming packets until stopped */
        while(!isStopped()){
            try {
            	// When a packet is received, process it in a worker thread
            	byte[] receiveData = new byte[Settings.BUFFER_SIZE];
            	DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(packet);
				threadPool.execute(new ProcessIncomingPacketsWorkerRunnable(socket, packet, "Thread Pooled Listener"));
            } catch (IOException e) {
                if(isStopped()) {
//                    System.out.println("IOException Listener Stopped.");///
                    break;
                }
                throw new RuntimeException("Error receiving incoming packets.", e);
            }
        }
        
        threadPool.shutdown();
//        System.out.println("Listener Stopped.");///
    }


    private synchronized boolean isStopped() {
        return isStopped;
    }

    public synchronized void stop(){
        isStopped = true;
        socket.close();
    }
}