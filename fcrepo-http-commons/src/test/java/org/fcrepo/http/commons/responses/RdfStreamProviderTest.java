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
package org.fcrepo.http.commons.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * <p>RdfStreamProviderTest class.</p>
 *
 * @author ajs6f
 */
public class RdfStreamProviderTest {

    private RdfStreamProvider testProvider = new RdfStreamProvider();

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testProvider.registerMimeTypes();
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                mockNamespaceRegistry);
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[] {});
    }

    @Test
    public void testGetSize() {
        assertEquals(-1, testProvider.getSize(null, null, null, null, null));
    }

    @Test
    public void testIsWriteable() {
        assertTrue("Should be able to serialize this!", testProvider
                .isWriteable(RdfStream.class, null, null, MediaType
                        .valueOf("application/rdf+xml")));
        assertFalse("Should not be able to serialize this!", testProvider
                .isWriteable(RdfStreamProviderTest.class, null, null, MediaType
                        .valueOf("application/rdf+xml")));
        assertFalse("Should not be able to serialize this!", testProvider
                .isWriteable(RdfStream.class, null, null, MediaType
                        .valueOf("text/html")));
    }

    @Test
    public void testWriteTo() throws WebApplicationException,
                             IllegalArgumentException, IOException {
        final Triple t =
            create(createURI("info:test"), createURI("property:test"),
                    createURI("info:test"));
        final RdfStream rdfStream = new RdfStream(t).session(mockSession);
        byte[] result;
        try (ByteArrayOutputStream entityStream = new ByteArrayOutputStream();) {
            testProvider.writeTo(rdfStream, RdfStream.class, null, null,
                    MediaType.valueOf("application/rdf+xml"), null,
                    entityStream);
            result = entityStream.toByteArray();
        }
        final Model postSerialization =
            createDefaultModel().read(new ByteArrayInputStream(result), null);
        assertTrue("Didn't find our triple!", postSerialization
                .contains(postSerialization.asStatement(t)));
    }

}
