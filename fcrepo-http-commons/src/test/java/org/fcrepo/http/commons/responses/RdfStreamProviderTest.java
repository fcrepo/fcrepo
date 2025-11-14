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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <p>RdfStreamProviderTest class.</p>
 *
 * @author ajs6f
 */
@ExtendWith(MockitoExtension.class)
public class RdfStreamProviderTest {

    private final RdfStreamProvider testProvider = new RdfStreamProvider();

    private static final RdfNamespaceRegistry registry = new RdfNamespaceRegistry();

    @BeforeAll
    public static void beforeClass() throws Exception {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("test", "http://example.org/test#");
        registry.setNamespaces(namespaces);
    }

    @BeforeEach
    public void setUp() {
        setField(testProvider, "registry", registry);
    }

    @Test
    public void testGetSize() {
        assertEquals(-1, testProvider.getSize(null, null, null, null, null));
    }

    @Test
    public void testIsWriteable() {
        assertTrue(testProvider.isWriteable(RdfNamespacedStream.class, null, null, MediaType
                        .valueOf("application/rdf+xml")), "Should be able to serialize this!");
        assertFalse(testProvider
                        .isWriteable(RdfStream.class, null, null, MediaType
                                .valueOf("application/rdf+xml")), "Should not be able to serialize this!");
        assertFalse(testProvider
                        .isWriteable(RdfStreamProviderTest.class, null, null, MediaType
                                .valueOf("application/rdf+xml")), "Should not be able to serialize this!");
        assertFalse(testProvider
                        .isWriteable(RdfStream.class, null, null, MediaType
                                .valueOf("text/html")), "Should not be able to serialize this!");
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
                assertTrue(postSerialization.contains(postSerialization.asStatement(t)), "Didn't find our triple!");
            }
        }
    }

}
