package controllers;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.DistanceVector;
import models.Neighbor;
import models.SettingsNOTUSED;
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
	private boolean dvChanged = false, notifyLinkDown = false;
	private String socketAddressNewDown = null;

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
        
//        int i = 0;///
        while(!isStopped()){
        	if (notifyLinkDown) {
        		notifyLinkDown = false;
//        		if (i > 30) break;///
//            	System.out.println(i++);///
        		System.out.println("Notify link-down");///
        		for (Neighbor n: HostLauncher.host.getNeighbors().values()) {
        			System.out.println("\t" + n.getSocketAddress() + " online? " + !n.isDown());///
        			if (!n.isDown()) {
            			try {
                			String socketAddress = n.getSocketAddress();
                    		String parts[] = socketAddress.split(":");
                    		String destinationAddress = parts[0];
            				int destinationPort = Integer.parseInt(parts[1]);
            				
            				JSONObject linkDownPacket = buildLinkDownPacket();
                    		threadPool.execute(new PacketSenderWorkerRunnable(socket, linkDownPacket , destinationAddress, destinationPort, "Thread Pooled Sender"));
                		} catch (NumberFormatException e) {
                			Utils.println("NumberFormatException in run()" + e.getMessage());
                		}
            		}
            	}
        	}
        	
        	/* Send dv to online neighbors when time out or routing table changed */
            if (dvChanged || hostTimeOut()) {
//            	if (i > 30) break;///
            	if (dvChanged) System.out.println("DV changed");///
            	else {
            		System.out.println("Host time-out");///
            		HostLauncher.host.showRoutingTable("\t");///
            	}
            	dvChanged = false;
            	for (Neighbor n: HostLauncher.host.getNeighbors().values()) {
            		if (!n.isDown()) {
            			try {
                			String socketAddress = n.getSocketAddress();
                    		String parts[] = socketAddress.split(":");
                    		String destinationAddress = parts[0];
            				int destinationPort = Integer.parseInt(parts[1]);
            				
            				JSONObject dvPacket = buildDVPacket(destinationAddress, destinationPort);
            				threadPool.execute(new PacketSenderWorkerRunnable(socket, dvPacket , destinationAddress, destinationPort, "Thread Pooled Sender"));
                		} catch (NumberFormatException e) {
                			Utils.println("NumberFormatException in run()" + e.getMessage());
                		}
            		}
            	}
            }
        }
        
        threadPool.shutdown();
//        System.out.println("Listener Stopped."); // DEBUG Thread pool stop signal
    }
	
	private synchronized JSONObject buildLinkDownPacket() {
		JSONObject packetContentJSON = new JSONObject();
		
		try {
			packetContentJSON.put("command", "linkDown");
			JSONObject body = new JSONObject();
			body.put("socketAddress", socketAddressNewDown);
			packetContentJSON.put("body", body);
			
			notifyLinkDown = false;
		} catch (JSONException e) {
			Utils.println("JSONException - buildLinkDownPacket(): " + e.getMessage());
		}
		
		return packetContentJSON;
	}

	/**
	 * Send signal to notify every online host about a host going down.
	 * Reset timer.
	 * @param socketAddress
	 */
	public synchronized void notifyLinkDown(String socketAddress) {
		notifyLinkDown = true;
		socketAddressNewDown = socketAddress;
		
		timestampPrev = System.nanoTime();
	}

	/**
	 * Calculate based on current time whether expiration time has been reached.
	 * @return
	 */
	private boolean hostTimeOut() {
    	boolean result;
    	
    	long timestamp = System.nanoTime();
    	if (NANOSECONDS.toSeconds(timestamp - timestampPrev) >= HostLauncher.TIME_OUT) {
			result = true;
			timestampPrev = timestamp;
		}
		else {
			result = false;
		}
		
    	return result;
	}

	/**
	 * Send signal for sending updated DV to neighbors.
	 * Reset timer.
	 */
	public synchronized void dvChanged() {
		dvChanged = true;
		
		timestampPrev = System.nanoTime();
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
    	String address = HostLauncher.host.getAddress(); // TODO Use InetAddress.getLocalHost().getHostAddress()
    	int port = socket.getLocalPort();
    	dv = new DistanceVector(address + ":" + port);
    	
    	// Get costs for neighbors
    	for (Neighbor n: HostLauncher.host.getNeighbors().values()) {
    		String socketAddressNeighbor = n.getSocketAddress();
    		double cost = n.getCost();
    		String socketAddressNextHop = n.getNextHop();
    		String socketAddressDestination = destinationAddress + ":" + destinationPort;
    		
    		// If destination is the next hop from host to n (poison reverse), set cost to infinity
    		// If next hop is down, set cost to infinity
    		if ((!socketAddressNeighbor.equals(socketAddressDestination) && socketAddressNextHop.equals(socketAddressDestination))
    			|| HostLauncher.host.getNeighbors().get(socketAddressNextHop).isDown()) {
    			cost = Double.MAX_VALUE;
    		}
    		
//    		System.out.println("# socketAddressNeighbor = " + socketAddressNeighbor);///
//    		System.out.println("# socketAddressNextHop = " + socketAddressNextHop);///
//    		System.out.println("# socketAddressDestination = " + socketAddressDestination);///
//    		System.out.println("# cost = " + cost + "\n");///
    		
    		
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