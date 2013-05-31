/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.integration.observer;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.AbstractIT;
import org.fcrepo.observer.FedoraEvent;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date Mar 20, 2013
 */
@ContextConfiguration({"/spring-test/eventing.xml", "/spring-test/repo.xml"})
public class SimpleObserverIT extends AbstractIT {

    private Integer eventBusMessageCount;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    /**
     * @todo Add Documentation.
     */
    @Test
    public void TestEventBusPublishing() throws RepositoryException {

        final Session se = repository.login();
        se.getRootNode().addNode("/object1")
            .addMixin(FedoraJcrTypes.FEDORA_OBJECT);
        se.getRootNode().addNode("/object2")
            .addMixin(FedoraJcrTypes.FEDORA_OBJECT);
        se.save();
        se.logout();

        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Should be two messages, for each time
        // each node becomes a Fedora object

        assertEquals("Where are my messages!?", (Integer) 2,
                eventBusMessageCount);

    }

    /**
     * @todo Add Documentation.
     */
    @Subscribe
    public void countMessages(final FedoraEvent e) {
        eventBusMessageCount++;
    }

    /**
     * @todo Add Documentation.
     */
    @Before
    public void acquireConnections() {
        eventBusMessageCount = 0;
        eventBus.register(this);
    }

    /**
     * @todo Add Documentation.
     */
    @After
    public void releaseConnections() {
        eventBus.unregister(this);
    }
}
