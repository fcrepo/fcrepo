package org.fcrepo.utils;

import javax.jcr.NodeIterator;
import javax.jcr.RangeIterator;

import org.fcrepo.Datastream;

public class DatastreamIterator implements RangeIterator {
	private final NodeIterator nodes;
	
	public DatastreamIterator(NodeIterator nodes) {
		this.nodes = nodes;
	}
	
	public Datastream nextDatastream() {
		return new Datastream(nodes.nextNode());
	}

	@Override
	public boolean hasNext() {
		return nodes.hasNext();
	}

	@Override
	public Object next() {
		return new Datastream(nodes.nextNode());
	}

	@Override
	public void remove() {
		nodes.remove();
	}

	@Override
	public void skip(long skipNum) {
		this.nodes.skip(skipNum);
	}

	@Override
	public long getSize() {
		return nodes.getSize();
	}

	@Override
	public long getPosition() {
		return this.nodes.getPosition();
	}

}
