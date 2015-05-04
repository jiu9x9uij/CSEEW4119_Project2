package controllers;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.DistanceVector;
import models.Settings;
import models.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

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
				String destinationAddress = "127.0.0.1"; // TODO
				int destinationPort = 6789;// TODO
				System.out.println("Address: " + destinationAddress + " port: " + destinationPort);///
				JSONObject dvPacket = buildDVPacket(destinationAddress, destinationPort);
				threadPool.execute(new PacketSenderWorkerRunnable(socket, dvPacket , destinationAddress, destinationPort, "Thread Pooled Sender"));
//	            System.out.println("time out");///

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
	
	private JSONObject buildDVPacket(String destinationAddress, int destinationPort) {
		JSONObject packetContentJSON = new JSONObject();
		
		try {
			DistanceVector dv = buildDV(destinationAddress, destinationPort);
			Gson gson = new Gson();
			JSONObject body = new JSONObject(gson.toJson(dv));
			packetContentJSON.put("command", "routeUpdate");
			packetContentJSON.put("body", body);
		} catch (JSONException e) {
			Utils.println("JSONException - buildDVPacket(): " + e.getMessage());
		}
		
		return packetContentJSON;
	}
	
	/**
     * Build DV of this host from its current neighbors.
	 * @param destinationPort 
	 * @param destinationAddress 
     * @return Newest DV of this host
     */
    private DistanceVector buildDV(String destinationAddress, int destinationPort) {
    	DistanceVector dv;
    	
    	// Initialize
    	String address = "127.0.0.1"; // TODO Get address from socket
    	int port = socket.getLocalPort();
    	dv = new DistanceVector(address + ":" + port);
    	
    	// TODO Get costs for neighbors
    	
    	
    	
    	return dv;
    }

	private synchronized boolean isStopped() {
        return isStopped;
    }

    public synchronized void stop(){
        isStopped = true;
        socket.close();
    }
}