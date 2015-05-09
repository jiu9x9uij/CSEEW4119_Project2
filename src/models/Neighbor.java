package models;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.gson.Gson;

import controllers.HostLauncher;


public class Neighbor {
	String socketAddress;
	double cost, costBackup;
	DistanceVector dv;
	String nextHop;
	private long timestampLastDV;
	
	public Neighbor(String address, double cost) {
		this.socketAddress = address;
		this.cost = cost;
		this.costBackup = -1;
		this.dv = null;
		this.nextHop = address;
		this.timestampLastDV = System.nanoTime();
	}
	
	public String getSocketAddress() {
		return socketAddress;
	}
	
	public double getCost() {
		return cost;
	}
	
	public void setCost(double cost) {
		this.cost = cost;
	}
	
	public synchronized boolean isDown() {
		boolean result;
		
		long timestamp = System.nanoTime();
//    	System.out.println("	*costBackup = " + costBackup);///
		if (costBackup == -1) {
			// If last received DV from this neighbor 3*TIME_OUT ago, take its link down
			if (NANOSECONDS.toSeconds(timestamp - timestampLastDV) >= 3*HostLauncher.TIME_OUT) {
				System.out.print("[isDown()] ");///
				if (linkDown()); // NOTE Just waiting for linkDown to finish
				result = true;
			} else {
				result = false;
			}
		} else {
			result = true;
		}
		
		return result;
	}
	
	public synchronized boolean linkDown() {
		boolean finished = false;
		
		System.out.println("linkDown " + socketAddress);///
		costBackup = cost;
		if (nextHop.equals(socketAddress)) {
			cost = Double.MAX_VALUE;
		}
		HostLauncher.host.updateRoutingTable();
		// TODO Notify neighbors
		HostLauncher.host.notifyLinkDown(socketAddress);
		
		finished = true;
		return finished;
	}
	
	public synchronized void linkUp() {
		cost = costBackup;
		costBackup = -1;
		timestampLastDV = System.nanoTime();
		HostLauncher.host.updateRoutingTable();
		// TODO Notify neighbors
	}
	
	public DistanceVector getDV() {
		return dv;
	}
	
	public void setDV(DistanceVector dv) {
		this.dv = dv;
	}
	
	public String getNextHop() {
		return nextHop;
	}
	
	public void setNextHop(String nextHop) {
		this.nextHop = nextHop;
	}
	
	public void updateTimestamp() {
		this.timestampLastDV = System.nanoTime();
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		
		Gson gson = new Gson();
		s.append(gson.toJson(this));
		
		return s.toString();
	}
}
