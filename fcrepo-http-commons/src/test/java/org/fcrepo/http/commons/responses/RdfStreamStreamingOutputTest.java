/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static com.google.common.util.concurrent.Futures.addCallback;
import static jakarta.ws.rs.core.MediaType.valueOf;
import static java.util.stream.Stream.of;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_TYPE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RiotException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <p>RdfStreamStreamingOutputTest class.</p>
 *
 * @author ajs6f
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class RdfStreamStreamingOutputTest {

    private RdfStreamStreamingOutput testRdfStreamStreamingOutput;

    private static final Triple triple = create(createURI("info:testSubject"),
            createURI("info:testPredicate"), createURI("info:testObject"));

    @Mock
    private Node mockNode;

    private final RdfStream testRdfStream = new DefaultRdfStream(triple.getSubject(), of(triple));

    private final Map<String, String> testNamespaces = new HashMap<>();

    @Mock
    private RdfStream mockRdfStream;

    private final MediaType testMediaType = valueOf("application/rdf+xml");

    private static final Logger LOGGER =
            getLogger(RdfStreamStreamingOutputTest.class);

    @BeforeEach
    public void setUp() {
        testRdfStreamStreamingOutput =
            new RdfStreamStreamingOutput(testRdfStream, testNamespaces, testMediaType);
    }

    @Test
    public void testWrite() throws IOException {
        assertOutputContainsTriple(triple);
    }

    public void assertOutputContainsTriple(final Triple expected) throws IOException {
        try (final RdfStream input = new DefaultRdfStream(expected.getSubject(), of(expected));
                final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, testNamespaces, testMediaType).write(output);
            try ( final InputStream resultStream = new ByteArrayInputStream(output.toByteArray())) {
                final Model result = createDefaultModel().read(resultStream, null);
                assertTrue(result.contains(result.asStatement(expected)), "Didn't find our test triple!");
            }
        }
    }

    @Test
    public void testWriteWithNamespace() throws IOException {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("a", "info:");
        try (final RdfStream input = new DefaultRdfStream(triple.getSubject(), of(triple));
                final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, namespaces, TURTLE_TYPE).write(output);
            final String s = output.toString("UTF-8");
            assertTrue(s.replaceAll("\\s+", " ").contains("@prefix a: <info:>"));
        }
    }

    @Test
    public void testWriteWithTypedObject() throws IOException {
        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"),
                createTypedLiteral(0).asNode()));
    }

    @Test
    public void testWriteWithBlankSubject() throws IOException {
        try (final RdfStream input = new DefaultRdfStream(createResource().asNode(), of(create(createResource()
                .asNode(), createURI("info:testPredicate"), createTypedLiteral(0).asNode())));
                final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, testNamespaces, testMediaType).write(output);
            try (final InputStream resultStream = new ByteArrayInputStream(output.toByteArray())) {
                final Model result = createDefaultModel().read(resultStream, null);
                assertTrue(result.contains(null, createProperty("info:testPredicate"), createTypedLiteral(0)));
            }
        }
    }


    @Test
    public void testWriteWithBlankObject() throws IOException {
        final Stream<Triple> triples =
                of(create(createResource().asNode(), createURI("info:testPredicate"), createResource().asNode()));
        try (final RdfStream input = new DefaultRdfStream(createResource().asNode(), triples);
                final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, testNamespaces, testMediaType).write(output);
            try (final InputStream resultStream = new ByteArrayInputStream(output.toByteArray())) {
                final Model result = createDefaultModel().read(resultStream, null);
                assertTrue(result.contains(null, createProperty("info:testPredicate"), (RDFNode) null));
            }
        }
    }

    @Test
    public void testWriteWithDatetimeObject() throws IOException {
        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"), createLiteral("2014-01-01T01:02:03Z", XSDdateTime)));
    }

    @Test
    public void testWriteWithLanguageLiteral() throws IOException {
        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"),
                createLiteral("french string", "fr")));
    }

    @Test
    public void testWriteWithException() throws IOException {

        final FutureCallback<Void> callback = new FutureCallback<>() {

            @Override
            public void onSuccess(final Void v) {
                throw new AssertionError("Should never happen!");
            }

            @Override
            public void onFailure(final Throwable e) {
                LOGGER.debug("Got exception: " + e.getMessage());
                assertInstanceOf(RiotException.class, e, "Got wrong kind of exception!");
            }
        };
        addCallback(testRdfStreamStreamingOutput, callback, MoreExecutors.directExecutor());
        final OutputStream mockOutputStream = mock(OutputStream.class, (Answer<Object>) invocation -> {
            throw new RiotException("Expected.");});
        assertThrows(WebApplicationException.class, () -> testRdfStreamStreamingOutput.write(mockOutputStream));
    }
}
