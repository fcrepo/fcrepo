package org.fcrepo.modeshape.observer;

import static com.google.common.collect.Collections2.filter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.eventbus.EventBus;

public class SimpleObserver implements EventListener {

	final private Integer eventTypes = Event.NODE_ADDED + Event.NODE_REMOVED
			+ Event.NODE_MOVED + Event.PROPERTY_ADDED + Event.PROPERTY_CHANGED
			+ Event.PROPERTY_REMOVED;

	@Inject
	private Repository repository;

	@Inject
	private EventBus eventBus;

	@Inject
	private EventFilter eventFilter;

	final private Logger logger = LoggerFactory.getLogger(SimpleObserver.class);

	@PostConstruct
	public void buildListener() throws RepositoryException {
		Session session = repository.login();
		session.getWorkspace()
				.getObservationManager()
				.addEventListener(this, eventTypes, "/", true, null, null,
						false);
		session.save();
		session.logout();
	}

	// it's okay to suppress type-safety warning here,
	// because we know that EventIterator only produces
	// Events, like an Iterator<Event>
	@SuppressWarnings("unchecked")
	@Override
	public void onEvent(EventIterator events) {
		for (Event e : filter(new Builder<Event>().addAll(events).build(),
				eventFilter)) {
			logger.debug("Putting event: " + e.toString() + " on the bus.");
			eventBus.post(e);
		}
	}

}
