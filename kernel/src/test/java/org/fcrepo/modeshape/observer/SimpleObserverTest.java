package org.fcrepo.modeshape.observer;

import static junit.framework.Assert.assertEquals;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.fcrepo.modeshape.AbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@ContextConfiguration({ "/spring-test/eventing.xml", "/spring-test/repo.xml" })
public class SimpleObserverTest extends AbstractTest {

	private Integer eventBusMessageCount;
	@Inject
	private Repository repository;

	@Inject
	private EventBus eventBus;

	@Test
	public void TestEventBusPublishing() throws RepositoryException {

		Session se = repository.login();
		se.getRootNode().addNode("/object1").addMixin("fedora:object");
		se.getRootNode().addNode("/object2").addMixin("fedora:object");
		se.save();
		se.logout();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Should be two messages, for each time
		// each node becomes a Fedora object

		assertEquals("Where are my messages!?", (Integer) 2,
				eventBusMessageCount);

	}

	@Subscribe
	public void countMessages(Event e) {
		eventBusMessageCount++;
	}

	@Before
	public void acquireConnections() {
		eventBusMessageCount = 0;
		eventBus.register(this);
	}

	@After
	public void releaseConnections() {
		eventBus.unregister(this);
	}
}
