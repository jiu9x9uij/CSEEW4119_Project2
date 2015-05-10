package models;

import java.util.Comparator;

public class FileChunkComparator implements Comparator<FileChunk> {
	@Override
	public int compare(FileChunk c1, FileChunk c2) {
		Integer index1 = c1.getIndex();
		Integer index2 = c2.getIndex();

		return index1.compareTo(index2);
	}
	
}