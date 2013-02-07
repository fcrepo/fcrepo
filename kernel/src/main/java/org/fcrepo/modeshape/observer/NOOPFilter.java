package org.fcrepo.modeshape.observer;

import javax.jcr.observation.Event;

public class NOOPFilter implements EventFilter {
	@Override
	public boolean apply(Event event) {
		return true;
	}
}
