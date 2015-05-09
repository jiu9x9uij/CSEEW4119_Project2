package models;

import java.util.Hashtable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONObject;

import com.google.gson.Gson;


public class DistanceVector {
	private String socketAddress;
	private Hashtable<String, Double> costs;
	
	public DistanceVector(String socketAddress) {
		this.socketAddress = socketAddress;
		this.costs = new Hashtable<String, Double>();
	}
	
	public String getSocketAddress() {
		return socketAddress;
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
	
	@Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(socketAddress).append(costs).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof DistanceVector)) {
    		return false;
    	}
            
        if (obj == this) {
        	return true;
        }
            
        DistanceVector rhs = (DistanceVector) obj;
        return new EqualsBuilder().append(socketAddress, rhs.socketAddress).append(costs, rhs.costs).isEquals();
    }
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		
		Gson gson = new Gson();
		s.append(gson.toJson(this));
		
		return s.toString();
	}
}
