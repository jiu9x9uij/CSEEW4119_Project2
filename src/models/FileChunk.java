package models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class FileChunk {
	int index;
	byte[] buffer;
	
	public FileChunk(int index, byte[] buffer) {
		this.index = index;
		this.buffer = buffer;
	}
	
	public int getIndex() {
		return index;
	}
	
	public byte[] getBuffer() {
		return buffer;
	}
	
	@Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(index).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof FileChunk)) {
    		return false;
    	}
            
        if (obj == this) {
        	return true;
        }
            
        FileChunk rhs = (FileChunk) obj;
        return new EqualsBuilder().append(index, rhs.index).isEquals();
    }
}
