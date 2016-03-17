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
import static com.jayway.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.observer.EventType.NODE_ADDED;
import static org.fcrepo.kernel.api.observer.EventType.NODE_MOVED;
import static org.fcrepo.kernel.api.observer.EventType.NODE_REMOVED;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_ADDED;
import static org.fcrepo.kernel.api.observer.EventType.PROPERTY_CHANGED;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.junit.Assert.assertEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>SimpleObserverIT class.</p>
 *
 * @author awoods
 * @author acoburn
 */
@ContextConfiguration({"/spring-test/eventing.xml", "/spring-test/repo.xml"})
public class SimpleObserverIT extends AbstractIT {

    private volatile List<FedoraEvent> events;

    private int eventBusMessageCount;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Inject
    private ContainerService containerService;

    @Inject
    private NodeService nodeService;

    @Test
    public void testEventBusPublishing() throws RepositoryException {

        final Session se = repository.login();
        se.getRootNode().addNode("/object1").addMixin(FEDORA_CONTAINER);
        se.getRootNode().addNode("/object2").addMixin(FEDORA_CONTAINER);
        se.save();
        se.logout();
        await().atMost(5, SECONDS).pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> eventBusMessageCount == 2);
        // Should be two messages, for each time each node becomes a Fedora object
        assertEquals("Where are my messages!?", 2, eventBusMessageCount);

    }

    @Test
    public void contentEventCollapsing() throws RepositoryException, InvalidChecksumException {

        final Session session = repository.login();
        try {
            final JcrTools jcrTools = new JcrTools();

            final FedoraResource container = nodeService.findOrCreate(session, "/object3");

            final String content = "test content";
            final String checksum = "1eebdf4fdc9fc7bf283031b93f9aef3338de9052";

            new FedoraBinaryImpl(jcrTools.findOrCreateChild(container.getNode(), JCR_CONTENT, NT_RESOURCE)).setContent(
                    new ByteArrayInputStream(content.getBytes()), "text/plain",
                    asURI("SHA-1", checksum), "text.txt", null);

            session.save();
        } finally {
            session.logout();
        }
        await().atMost(5, SECONDS).pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> eventBusMessageCount > 0);

        assertEquals("Node and content events not collapsed!", 1, eventBusMessageCount);
    }

    @Test
    public void testMoveEvent() throws RepositoryException {

        final Session session = repository.login();
        try {
            final Node n = session.getRootNode().addNode("/object4");
            n.addMixin(FEDORA_CONTAINER);
            n.addNode("/child1").addMixin(FEDORA_CONTAINER);
            n.addNode("/child2").addMixin(FEDORA_CONTAINER);
            session.save();
            nodeService.moveObject(session, "/object4", "/object5");
            session.save();
        } finally {
            session.logout();
        }

        awaitEvent("/object4", NODE_ADDED);
        awaitEvent("/object4/child1", NODE_ADDED);
        awaitEvent("/object4/child2", NODE_ADDED);
        awaitEvent("/object5", NODE_MOVED);
        awaitEvent("/object5/child1", NODE_MOVED);
        awaitEvent("/object5/child2", NODE_MOVED);
        awaitEvent("/object4", NODE_REMOVED);
        awaitEvent("/object4/child1", NODE_REMOVED);
        awaitEvent("/object4/child2", NODE_REMOVED);

        assertEquals("Move operation didn't generate additional events", 9, eventBusMessageCount);
    }

    @Test
    public void testMoveContainedEvent() throws RepositoryException {

        final Session session = repository.login();
        try {
            final Node n = session.getRootNode().addNode("/object6");
            n.addMixin(FEDORA_CONTAINER);
            final Node child = n.addNode("/object7");
            child.addMixin(FEDORA_CONTAINER);
            child.addNode("/child1").addMixin(FEDORA_CONTAINER);
            child.addNode("/child2").addMixin(FEDORA_CONTAINER);
            session.save();
            nodeService.moveObject(session, "/object6/object7", "/object6/object8");
            session.save();
        } finally {
            session.logout();
        }

        awaitEvent("/object6", NODE_ADDED);
        awaitEvent("/object6/object7", NODE_ADDED);
        awaitEvent("/object6/object7/child1", NODE_ADDED);
        awaitEvent("/object6/object7/child2", NODE_ADDED);
        awaitEvent("/object6/object8", NODE_MOVED);
        awaitEvent("/object6/object8/child1", NODE_MOVED);
        awaitEvent("/object6/object8/child2", NODE_MOVED);
        awaitEvent("/object6/object7", NODE_REMOVED);
        awaitEvent("/object6/object7/child1", NODE_REMOVED);
        awaitEvent("/object6/object7/child2", NODE_REMOVED);
        // should produce two of these
        awaitEvent("/object6", PROPERTY_CHANGED);

        assertEquals("Move operation didn't generate additional events", 12, eventBusMessageCount);
    }

    @Test
    public void testHashUriEvent() throws RepositoryException {
        final Session session = repository.login();
        try {
            final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(session);

            final Container obj = containerService.findOrCreate(session, "/object9");

            final Resource subject = subjects.reverse().convert(obj);

            obj.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "INSERT { <" + subject + "> dc:contributor <" + subject + "#contributor> .\n" +
                    "<" + subject + "#contributor> foaf:name \"some creator\" . } WHERE {}",
                    obj.getTriples(subjects, PROPERTIES));

            session.save();
        } finally {
            session.logout();
        }

        // these first two should be part of a single event
        awaitEvent("/object9", NODE_ADDED);
        awaitEvent("/object9", PROPERTY_ADDED);
        awaitEvent("/object9#contributor", PROPERTY_ADDED);

        assertEquals("Where are my events?", 2, eventBusMessageCount);
    }


    private void awaitEvent(final String id, final EventType eventType) {
        await().atMost(5, SECONDS).pollInterval(ONE_SECOND).until(() ->
            events.stream().anyMatch(evt -> evt.getPath().equals(id) && evt.getTypes().contains(eventType)));
    }

    @Subscribe
    public void countMessages(final FedoraEvent e) {
        eventBusMessageCount++;
        events.add(e);
    }

    @Before
    public void acquireConnections() {
        eventBusMessageCount = 0;
        events = new ArrayList<>();
        eventBus.register(this);
    }

    @After
    public void releaseConnections() {
        eventBus.unregister(this);
    }
}
