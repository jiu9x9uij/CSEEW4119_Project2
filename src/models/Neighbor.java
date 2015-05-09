package models;

import com.google.gson.Gson;


public class Neighbor {
	String socketAddress;
	double cost;
	DistanceVector dv;
	String nextHop;
	
	public Neighbor(String address, double cost) {
		this.socketAddress = address;
		this.cost = cost;
		this.dv = null;
		this.nextHop = address;
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
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		
		Gson gson = new Gson();
		s.append(gson.toJson(this));
		
		return s.toString();
	}
}
