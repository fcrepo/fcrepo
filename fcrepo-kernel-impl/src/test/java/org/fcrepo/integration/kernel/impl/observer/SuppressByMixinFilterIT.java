/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.impl.observer;

import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Set;

/**
 * <p>SuppressByMixinFilterIT class.</p>
 *
 * @author escowles
 * @since 2015-04-15
 */
@ContextConfiguration({"/spring-test/eventing-suppress.xml", "/spring-test/repo.xml"})
public class SuppressByMixinFilterIT extends AbstractIT {

    private Integer eventBusMessageCount;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Test
    public void shouldSuppressWithMixin() throws RepositoryException {

        final Session se = repository.login();
        final Node node = se.getRootNode().addNode("/object1");
        node.addMixin(FEDORA_CONTAINER);
        node.addMixin(LDP_DIRECT_CONTAINER);
        se.save();
        se.logout();

        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("Event not suppressed!", (Integer)0, eventBusMessageCount);

    }

    @Test
    public void shouldAllowWithoutMixin() throws RepositoryException {

        final Session se = repository.login();
        final Node node = se.getRootNode().addNode("/object1");
        node.addMixin(FEDORA_CONTAINER);
        se.save();
        se.logout();

        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("Wrong number of events!", (Integer)1, eventBusMessageCount);

    }

    @Subscribe
    public void countMessages(final FedoraEvent e) {
        eventBusMessageCount++;

        final Set<String> properties = e.getProperties();
        assertNotNull(properties);

        final String expected = REPOSITORY_NAMESPACE + "mixinTypes";
        assertTrue("Should contain: " + expected + properties, properties.contains(expected));
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
