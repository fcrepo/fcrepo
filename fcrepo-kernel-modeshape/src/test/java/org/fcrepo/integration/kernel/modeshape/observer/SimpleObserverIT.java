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
package org.fcrepo.integration.kernel.modeshape.observer;

import static java.lang.Thread.sleep;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Set;

/**
 * <p>SimpleObserverIT class.</p>
 *
 * @author awoods
 */
@ContextConfiguration({"/spring-test/eventing.xml", "/spring-test/repo.xml"})
public class SimpleObserverIT extends AbstractIT {

    private Integer eventBusMessageCount;

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Test
    public void TestEventBusPublishing() throws RepositoryException {

        final Session se = repository.login();
        se.getRootNode().addNode("/object1").addMixin(FEDORA_CONTAINER);
        se.getRootNode().addNode("/object2").addMixin(FEDORA_CONTAINER);
        se.save();
        se.logout();

        try {
            sleep(500);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Should be two messages, for each time
        // each node becomes a Fedora object

        assertEquals("Where are my messages!?", (Integer) 2,
                eventBusMessageCount);

    }

    @Test
    public void contentEventCollapsing() throws RepositoryException, InvalidChecksumException {

        final Session se = repository.login();
        final JcrTools jcrTools = new JcrTools();

        final Node n = jcrTools.findOrCreateNode(se.getRootNode(), "/object3", NT_FOLDER, NT_FILE);
        n.addMixin(FEDORA_RESOURCE);

        final String content = "test content";
        final String checksum = "1eebdf4fdc9fc7bf283031b93f9aef3338de9052";
        ((ValueFactory) se.getValueFactory()).createBinary(new ByteArrayInputStream(content.getBytes()), null);

        final Node contentNode = jcrTools.findOrCreateChild(n, JCR_CONTENT, NT_RESOURCE);
        contentNode.addMixin(FEDORA_BINARY);
        final FedoraBinary binary = new FedoraBinaryImpl(contentNode);
        binary.setContent( new ByteArrayInputStream(content.getBytes()), "text/plain",
                asURI("SHA-1", checksum), "text.txt", null);

        se.save();
        se.logout();

        try {
            sleep(500);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("Node and content events not collapsed!", (Integer) 1, eventBusMessageCount);

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
