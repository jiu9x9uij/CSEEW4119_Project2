package models;

import java.util.Hashtable;

import org.json.JSONObject;


public class DistanceVector {
	private Hashtable<String, Double> costs;
	
	public DistanceVector() {
		this.costs = new Hashtable<String, Double>();
	}
	
	public void add(String socketAddress, double cost) {
		costs.put(socketAddress, cost);
	}
	
	public double getCostTo(String socketAddress) {
		return costs.get(socketAddress);
	}
	
	public JSONObject toJSON() {
		JSONObject dv = new JSONObject();
		
		dv.put("cost", costs);
		
		return dv;
	}
}
