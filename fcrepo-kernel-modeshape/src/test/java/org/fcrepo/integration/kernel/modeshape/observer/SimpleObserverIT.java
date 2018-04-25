/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.api.RdfLexicon.NT_LEAF_NODE;
import static org.fcrepo.kernel.api.RdfLexicon.NT_VERSION_FILE;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_CREATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_DELETION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_MODIFICATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_RELOCATION;
import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.modeshape.services.NodeServiceImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.jena.rdf.model.Resource;

/**
 * <p>SimpleObserverIT class.</p>
 *
 * @author awoods
 * @author acoburn
 */
@ContextConfiguration({"/spring-test/eventing.xml", "/spring-test/repo.xml"})
public class SimpleObserverIT extends AbstractIT {

    private volatile List<FedoraEvent> events;

    private Integer eventBusMessageCount;

    @Inject
    private FedoraRepository repository;

    @Inject
    private EventBus eventBus;

    @Inject
    private ContainerService containerService;

    @Inject
    private BinaryService binaryService;

    @Test
    public void testEventBusPublishing() throws RepositoryException {

        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        se.getRootNode().addNode("/object1").addMixin(FEDORA_CONTAINER);
        se.getRootNode().addNode("/object2").addMixin(FEDORA_CONTAINER);
        session.commit();
        session.expire();

        // Should be two messages, for each time
        // each node becomes a Fedora object

        awaitEvent("/object1", RESOURCE_CREATION, REPOSITORY_NAMESPACE + "Container");
        awaitEvent("/object2", RESOURCE_CREATION, REPOSITORY_NAMESPACE + "Container");

        assertEquals("Where are my messages!?", (Integer) 2, eventBusMessageCount);

    }

    @Test
    public void contentEventCollapsing() throws RepositoryException, InvalidChecksumException {

        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        final JcrTools jcrTools = new JcrTools();

        final Node n = jcrTools.findOrCreateNode(se.getRootNode(), "/object3", NT_FOLDER, NT_VERSION_FILE);
        n.addMixin(FEDORA_RESOURCE);
        n.addMixin(FEDORA_BINARY);

        final String content = "test content";
        final String checksum = "1eebdf4fdc9fc7bf283031b93f9aef3338de9052";
        ((ValueFactory) se.getValueFactory()).createBinary(new ByteArrayInputStream(content.getBytes()), null);

        final Node descNode = jcrTools.findOrCreateChild(n, FEDORA_DESCRIPTION, NT_LEAF_NODE);
        descNode.addMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION);

        final FedoraBinary binary = new FedoraBinaryImpl(n);
        binary.setContent( new ByteArrayInputStream(content.getBytes()), "text/plain",
                new HashSet<>(asList(asURI(SHA1.algorithm, checksum))), "text.txt", null);

        try {
            session.commit();
        } finally {
            session.expire();
        }

        awaitEvent("/object3", RESOURCE_CREATION, REPOSITORY_NAMESPACE + "Binary");

        assertEquals("Node and content events not collapsed!", (Integer) 1, eventBusMessageCount);
    }

    @Test
    public void testMoveEvent() throws RepositoryException {

        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        final NodeService ns = new NodeServiceImpl();

        final Node n = se.getRootNode().addNode("/object4");
        n.addMixin(FEDORA_CONTAINER);
        n.addNode("/child1").addMixin(FEDORA_CONTAINER);
        n.addNode("/child2").addMixin(FEDORA_CONTAINER);
        session.commit();
        ns.moveObject(session, "/object4", "/object5");
        session.commit();
        session.expire();

        awaitEvent("/object4", RESOURCE_CREATION);
        awaitEvent("/object4/child1", RESOURCE_CREATION);
        awaitEvent("/object4/child2", RESOURCE_CREATION);
        awaitEvent("/object5", RESOURCE_RELOCATION);
        awaitEvent("/object5/child1", RESOURCE_RELOCATION);
        awaitEvent("/object5/child2", RESOURCE_RELOCATION);
        awaitEvent("/object4", RESOURCE_DELETION);
        awaitEvent("/object4/child1", RESOURCE_DELETION);
        awaitEvent("/object4/child2", RESOURCE_DELETION);

        assertEquals("Move operation didn't generate additional events", (Integer) 9, eventBusMessageCount);
    }

    @Test
    public void testMoveContainedEvent() throws RepositoryException {

        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        final NodeService ns = new NodeServiceImpl();

        final Node n = se.getRootNode().addNode("/object6");
        n.addMixin(FEDORA_CONTAINER);
        final Node child = n.addNode("/object7");
        child.addMixin(FEDORA_CONTAINER);
        child.addNode("/child1").addMixin(FEDORA_CONTAINER);
        child.addNode("/child2").addMixin(FEDORA_CONTAINER);
        session.commit();
        ns.moveObject(session, "/object6/object7", "/object6/object8");
        session.commit();
        session.expire();

        awaitEvent("/object6", RESOURCE_CREATION);
        awaitEvent("/object6/object7", RESOURCE_CREATION);
        awaitEvent("/object6/object7/child1", RESOURCE_CREATION);
        awaitEvent("/object6/object7/child2", RESOURCE_CREATION);
        awaitEvent("/object6/object8", RESOURCE_RELOCATION);
        awaitEvent("/object6/object8/child1", RESOURCE_RELOCATION);
        awaitEvent("/object6/object8/child2", RESOURCE_RELOCATION);
        awaitEvent("/object6/object7", RESOURCE_DELETION);
        awaitEvent("/object6/object7/child1", RESOURCE_DELETION);
        awaitEvent("/object6/object7/child2", RESOURCE_DELETION);
        // should produce two of these
        awaitEvent("/object6", RESOURCE_MODIFICATION);

        assertEquals("Move operation didn't generate additional events", (Integer) 12, eventBusMessageCount);
    }

    @Test
    public void testHashUriEvent() throws RepositoryException {
        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(se);

        final Container obj = containerService.findOrCreate(session, "/object9");

        final Resource subject = subjects.reverse().convert(obj);

        obj.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
            "INSERT { <" + subject + "> dc:contributor <" + subject + "#contributor> .\n" +
                "<" + subject + "#contributor> foaf:name \"some creator\" . } WHERE {}",
                obj.getTriples(subjects, PROPERTIES));

        session.commit();
        session.expire();

        // these first two are part of a single event
        awaitEvent("/object9", RESOURCE_CREATION);
        awaitEvent("/object9", RESOURCE_MODIFICATION);
        awaitEvent("/object9#contributor", RESOURCE_MODIFICATION);

        // FIXME -- it is unclear where this event is coming from; clearly from the hashURI,
        // but it doesn't seem right.
        awaitEvent("", RESOURCE_MODIFICATION);

        // FIXME -- as hinted above, there is an extra event here somehow related to the hashURI.
        assertEquals("Where are my events?", (Integer) 3, eventBusMessageCount);
    }

    @Test
    public void testDirectContainerEvent() throws RepositoryException {
        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(se);

        final Container obj1 = containerService.findOrCreate(session, "/object10");
        final Container obj2 = containerService.findOrCreate(session, "/object11");

        final Resource subject2 = subjects.reverse().convert(obj2);

        obj1.updateProperties(subjects, "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "PREFIX pcdm: <http://pcdm.org/models#>\n" +
                "INSERT { <> a ldp:DirectContainer ;\n" +
                    "ldp:membershipResource <" + subject2 + "> ;\n" +
                    "ldp:hasMemberRelation pcdm:hasMember . } WHERE {}", obj1.getTriples(subjects, PROPERTIES));

        try {
            session.commit();

            awaitEvent("/object10", RESOURCE_CREATION);
            awaitEvent("/object10", RESOURCE_MODIFICATION);
            awaitEvent("/object11", RESOURCE_CREATION);

            assertEquals("Where are my events?", (Integer) 3, eventBusMessageCount);

            final Container obj3 = containerService.findOrCreate(session, "/object10/child");
            session.commit();

            awaitEvent("/object10/child", RESOURCE_CREATION);
            awaitEvent("/object10", RESOURCE_MODIFICATION);
            awaitEvent("/object11", RESOURCE_MODIFICATION);

            assertEquals("Where are my events?", (Integer) 6, eventBusMessageCount);

            obj3.delete();
            session.commit();
        } finally {
            session.expire();
        }

        awaitEvent("/object10/child", RESOURCE_DELETION);
        awaitEvent("/object10", RESOURCE_MODIFICATION);
        awaitEvent("/object11", RESOURCE_MODIFICATION);

        assertEquals("Where are my events?", (Integer) 9, eventBusMessageCount);
    }

    @Test
    public void testIndirectContainerEvent() throws RepositoryException {
        final FedoraSession session = repository.login();
        final Session se = getJcrSession(session);
        final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(se);

        final Container obj1 = containerService.findOrCreate(session, "/object12");
        final Container obj2 = containerService.findOrCreate(session, "/object13");

        final Resource subject2 = subjects.reverse().convert(obj2);

        obj1.updateProperties(subjects, "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "PREFIX pcdm: <http://pcdm.org/models#>\n" +
                "PREFIX ore: <http://www.openarchives.org/ore/terms/>\n" +
                "INSERT { <> a ldp:IndirectContainer ;\n" +
                    "ldp:membershipResource <" + subject2 + "> ;\n" +
                    "ldp:hasMemberRelation pcdm:hasMember ;\n" +
                    "ldp:insertedContentRelation ore:proxyFor. } WHERE {}", obj1.getTriples(subjects, PROPERTIES));

        try {
            session.commit();

            awaitEvent("/object12", RESOURCE_CREATION);
            awaitEvent("/object12", RESOURCE_MODIFICATION);
            awaitEvent("/object13", RESOURCE_CREATION);

            assertEquals("Where are my events?", (Integer) 3, eventBusMessageCount);

            final Container obj3 = containerService.findOrCreate(session, "/object12/child");
            obj3.updateProperties(subjects, "PREFIX ore: <http://www.openarchives.org/ore/terms/>\n" +
                    "INSERT { <> ore:proxyFor <info:example/test> } WHERE {}", obj3.getTriples(subjects, PROPERTIES));
            session.commit();

            awaitEvent("/object12/child", RESOURCE_CREATION);
            awaitEvent("/object12", RESOURCE_MODIFICATION);
            awaitEvent("/object13", RESOURCE_MODIFICATION);

            assertEquals("Where are my events?", (Integer) 6, eventBusMessageCount);

            final Container obj4 = containerService.findOrCreate(session, "/object12/child2");
            session.commit();

            awaitEvent("/object12/child2", RESOURCE_CREATION);
            awaitEvent("/object12", RESOURCE_MODIFICATION);

            assertEquals("Where are my events?", (Integer) 8, eventBusMessageCount);

            // Update a property that is irrelevant for the indirect container
            obj4.updateProperties(subjects, "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "INSERT { <> rdfs:label \"some label\" } WHERE {}", obj4.getTriples(subjects, PROPERTIES));
            session.commit();

            awaitEvent("/object12/child2", RESOURCE_MODIFICATION);

            assertEquals("Where are my events?", (Integer) 9, eventBusMessageCount);

            // Update a property that is relevant for the indirect container
            obj4.updateProperties(subjects, "PREFIX ore: <http://www.openarchives.org/ore/terms/>\n" +
                    "INSERT { <> ore:proxyFor \"some proxy\" } WHERE {}", obj4.getTriples(subjects, PROPERTIES));
            session.commit();

            awaitEvent("/object12/child2", RESOURCE_MODIFICATION);
            awaitEvent("/object13", RESOURCE_MODIFICATION);

            assertEquals("Where are my events?", (Integer) 11, eventBusMessageCount);

            obj3.delete();
            session.commit();
        } finally {
            session.expire();
        }

        awaitEvent("/object12/child", RESOURCE_DELETION);
        awaitEvent("/object12", RESOURCE_MODIFICATION);
        awaitEvent("/object13", RESOURCE_MODIFICATION);

        assertEquals("Where are my events?", (Integer) 14, eventBusMessageCount);
    }

    private void awaitEvent(final String id, final EventType eventType, final String resourceType) {
        await().atMost(5, SECONDS).pollInterval(ONE_HUNDRED_MILLISECONDS).until(() -> events.stream().anyMatch(evt ->
                evt.getPath().equals(id) && evt.getTypes().contains(eventType)
                    && (resourceType == null || evt.getResourceTypes().contains(resourceType))));
    }

    private void awaitEvent(final String id, final EventType eventType) {
        awaitEvent(id, eventType, null);
    }

    @Subscribe
    public void countMessages(final FedoraEvent e) {
        eventBusMessageCount++;
        events.add(e);
    }

    @Before
    public void acquireConnections() {
        eventBusMessageCount = 0;
        events = new CopyOnWriteArrayList<>();
        eventBus.register(this);
    }

    @After
    public void releaseConnections() {
        eventBus.unregister(this);
    }
}
