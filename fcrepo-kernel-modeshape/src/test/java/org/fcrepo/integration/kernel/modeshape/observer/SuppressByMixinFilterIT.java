/*
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
package org.fcrepo.integration.kernel.modeshape.observer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.junit.Assert.assertFalse;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.observer.FedoraEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;


/**
 * <p>SuppressByMixinFilterIT class.</p>
 *
 * @author escowles
 * @author ajs6f
 * @since 2015-04-15
 */
@ContextConfiguration({"/spring-test/eventing-suppress.xml", "/spring-test/repo.xml"})
public class SuppressByMixinFilterIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    private final Set<FedoraEvent> eventsReceived = new HashSet<>();

    @Test
    public void shouldSuppressWithMixin() throws RepositoryException {

        final Session se = repository.login();
        try {
            // add node with suppressed mixin ldp:DirectContainer
            final Node node = se.getRootNode().addNode("/object1");
            node.addMixin(FEDORA_CONTAINER);
            node.addMixin(LDP_DIRECT_CONTAINER);
            se.save();
            // add second node without suppressed mixin
            se.getRootNode().addNode("/object2").addMixin(FEDORA_CONTAINER);
            se.save();
        } finally {
            se.logout();
        }
        // should only see second node
        waitForEvent("/object2");
        assertFalse("Event not suppressed!", eventsReceived.contains("/object1"));
    }

    @Test
    public void shouldAllowWithoutMixin() throws RepositoryException {

        final Session se = repository.login();
        try {
            se.getRootNode().addNode("/object3").addMixin(FEDORA_CONTAINER);
            se.save();
        } finally {
            se.logout();
        }
        // should see one message
        waitForEvent("/object3");
    }

    @Before
    public void acquireConnections() {
        eventBus.register(this);
    }

    @After
    public void releaseConnections() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void count(final FedoraEvent e) {
        eventsReceived.add(e);
    }

    private void waitForEvent(final String id) {
        await().atMost(5, SECONDS).pollInterval(ONE_SECOND).until(() ->
            eventsReceived.stream().anyMatch(e -> e.getPath().equals(id)));
    }
}
