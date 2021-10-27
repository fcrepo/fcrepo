/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.junit.Before;
import org.junit.Test;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>RdfStreamProviderTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RdfStreamProviderTest {

    private final RdfStreamProvider testProvider = new RdfStreamProvider();


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetSize() {
        assertEquals(-1, testProvider.getSize(null, null, null, null, null));
    }

    @Test
    public void testIsWriteable() {
        assertTrue("Should be able to serialize this!", testProvider
                .isWriteable(RdfNamespacedStream.class, null, null, MediaType
                        .valueOf("application/rdf+xml")));
        assertFalse("Should not be able to serialize this!", testProvider
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
    public void testWriteTo() throws WebApplicationException, IllegalArgumentException, IOException {
        final Triple t = create(createURI("info:test"), createURI("property:test"), createURI("info:test"));

        final Map<String, String> namespaces = new HashMap<>();
        try (final RdfStream rdfStream = new DefaultRdfStream(createURI("info:test"), of(t));
                final RdfNamespacedStream nsStream = new RdfNamespacedStream(rdfStream, namespaces)) {
            try (final ByteArrayOutputStream entityStream = new ByteArrayOutputStream()) {
                testProvider.writeTo(nsStream, RdfNamespacedStream.class, null, null,
                        MediaType.valueOf("application/rdf+xml"), null, entityStream);
                final byte[] result = entityStream.toByteArray();

                final Model postSerialization = createDefaultModel().read(new ByteArrayInputStream(result), null);
                assertTrue("Didn't find our triple!", postSerialization.contains(postSerialization.asStatement(t)));
            }
        }
    }

}
