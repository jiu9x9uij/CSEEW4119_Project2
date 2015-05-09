package controllers;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.DistanceVector;
import models.Neighbor;
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
	private boolean dvChanged = false;

    public ThreadPooledSender(DatagramSocket socket) {
		this.socket = socket;
		this.timestampPrev = System.nanoTime();
	}

    /**
     * Broadcast updated DV in an infinite loop.
     */
	public void run(){
        synchronized(this){
            runningThread = Thread.currentThread();
        }
        
        /* Send dv to neighbors when time out or routing table changed */
        while(!isStopped()){
            if (dvChanged || hostTimeOut()) {
            	if (dvChanged) System.out.println("DV changed");///
            	else System.out.println("Host time-out");///
            	for (Neighbor n: HostLauncher.host.getNeighbors().values()) {
                	try {
            			String socketAddress = n.getSocketAddress();
                		String parts[] = socketAddress.split(":");
                		String destinationAddress = parts[0];//"127.0.0.1"; // TODO Use parts[0]
        				int destinationPort = Integer.parseInt(parts[1]);// TODO
        				
        				JSONObject dvPacket = buildDVPacket(destinationAddress, destinationPort);
        				threadPool.execute(new PacketSenderWorkerRunnable(socket, dvPacket , destinationAddress, destinationPort, "Thread Pooled Sender"));
            		} catch (NumberFormatException e) {
            			Utils.println("NumberFormatException in run()" + e.getMessage());
            		}
            	}
            }
        }
        
        threadPool.shutdown();
//        System.out.println("Listener Stopped."); // DEBUG Thread pool stop signal
    }

	/**
	 * Calculate based on current time whether expiration time has been reached.
	 * @return
	 */
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

	/**
	 * For host to send signal for sending updated DV to neighbors.
	 */
	public void dvChanged() {
		dvChanged = true;
	}
	
	/**
	 * Encode DV into JSON format.
	 * @param destinationAddress
	 * @param destinationPort
	 * @return
	 */
	private JSONObject buildDVPacket(String destinationAddress, int destinationPort) {
		JSONObject packetContentJSON = new JSONObject();
		
		try {
			DistanceVector dv = buildDV(destinationAddress, destinationPort);
			Gson gson = new Gson();
			JSONObject body = new JSONObject(gson.toJson(dv));
			packetContentJSON.put("command", "routeUpdate");
			packetContentJSON.put("body", body);
			
			dvChanged = false;
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
    	
    	// Get costs for neighbors
    	for (Neighbor n: HostLauncher.host.getNeighbors().values()) {
    		String socketAddressNeighbor = n.getSocketAddress();
    		double cost = n.getCost();
    		String socketAddressNextHop = n.getNextHop();
    		String socketAddressDestination = destinationAddress + ":" + destinationPort;
    		
    		if (socketAddressNextHop.equals(socketAddressDestination)) {
    			cost = Double.MAX_VALUE;
    		}
    		
    		dv.add(socketAddressNeighbor, cost);
    	}
    	
    	
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