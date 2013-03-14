package org.fcrepo.jaxb.search;

import java.util.List;

/**
 * A search results object returned from a FedoraFieldSearch
 * @author Vincent Nguyen
 */
public class FieldSearchResult {

	private List<ObjectFields> objectFieldsList;
	
	private int start;
	
	private int end;
	
	private int size;
	
	public FieldSearchResult(List<ObjectFields> objectFieldsList, int start, int end, int size) {
		this.objectFieldsList = objectFieldsList;
		this.start = start;
		this.end = end;
		this.size = size;
	}

	public List<ObjectFields> getObjectFieldsList() {
		return objectFieldsList;
	}
	
	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}
	
	public int getEnd() {
		return end;
	}
	
	public final int getSize() {
		return size;
	}
}
