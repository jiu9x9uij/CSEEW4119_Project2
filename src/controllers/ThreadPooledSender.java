package controllers;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.Settings;

public class ThreadPooledSender implements Runnable{
	protected DatagramSocket socket;
	protected boolean isStopped = false;
	protected Thread runningThread= null;
	protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
	private long timestampPrev;

    public ThreadPooledSender(DatagramSocket socket) {
		this.socket = socket;
		this.timestampPrev = System.nanoTime();
	}

	public void run(){
        synchronized(this){
            runningThread = Thread.currentThread();
        }
        
        /* Listen to incoming packets until stopped */
        while(!isStopped()){
            if (DVChanged() || hostTimeOut()) {
            	InetAddress destinationAddress;
				try {
					destinationAddress = InetAddress.getByName("127.0.0.1");
//					InetAddress destinationAddress = socket.getLocalAddress();// TODO
					int destinationPort = 6789;// TODO
					System.out.println("Address: " + destinationAddress + " port: " + destinationPort);///
					threadPool.execute(new DVSenderWorkerRunnable(socket,/* TODO DV ,*/ destinationAddress, destinationPort, "Thread Pooled Sender"));
//	            	System.out.println("time out");///
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

            }
        }
        
        threadPool.shutdown();
//        System.out.println("Listener Stopped.");///
    }


    private boolean hostTimeOut() {
    	boolean result;
    	
    	long timestamp = System.nanoTime();
    	if (NANOSECONDS.toSeconds(timestamp - timestampPrev) >= Settings.TIME_OUT) {
			result = true;
			timestampPrev = timestamp;
		}
		else {
			result = false;
		}
		
    	return result;
	}

	private boolean DVChanged() {
		// TODO Auto-generated method stub
		return false;
	}

	private synchronized boolean isStopped() {
        return isStopped;
    }

    public synchronized void stop(){
        isStopped = true;
        socket.close();
    }
}