/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static java.util.stream.Stream.of;
import static com.google.common.util.concurrent.Futures.addCallback;
import static jakarta.ws.rs.core.MediaType.valueOf;
import static jakarta.json.Json.createReader;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.apache.jena.graph.NodeFactory.createLiteralByValue;
import static org.apache.jena.graph.NodeFactory.createLiteralLang;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.RdfStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;


/**
 * <p>RdfStreamStreamingOutputTest class.</p>
 *
 * @author ajs6f
 */
@ExtendWith(MockitoExtension.class)
public class RdfStreamStreamingOutputTest {

    private RdfStreamStreamingOutput testRdfStreamStreamingOutput;

    private static final Triple triple = create(createURI("info:testSubject"),
            createURI("info:testPredicate"), createURI("info:testObject"));

    private final RdfStream testRdfStream = new DefaultRdfStream(triple.getSubject(), of(triple));

    private final Map<String, String> testNamespaces = new HashMap<>();

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
            final String s = output.toString(StandardCharsets.UTF_8);
            assertTrue(s.replaceAll("\\s+", " ").contains("PREFIX a: <info:>"));
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
                createURI("info:testPredicate"), createLiteralByValue("2014-01-01T01:02:03Z", XSDdateTime)));
    }

    @Test
    public void testWriteWithLanguageLiteral() throws IOException {
        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"),
                createLiteralLang("french string", "fr")));
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
                LOGGER.debug("Got exception: {}", e.getMessage());
                assertInstanceOf(RiotException.class, e, "Got wrong kind of exception!");
            }
        };
        addCallback(testRdfStreamStreamingOutput, callback, MoreExecutors.directExecutor());
        assertThrows(WebApplicationException.class, () -> {
            try (final OutputStream mockOutputStream = mock(OutputStream.class, (Answer<Object>) invocation -> {
                throw new RiotException("Expected.");
            })) {
                testRdfStreamStreamingOutput.write(mockOutputStream);
            }
        });
    }

    @Test
    public void testJsonLdExpanded() throws IOException {
        final MediaType mediaType = new MediaType("application", "ld+json",
                Map.of("profile", "http://www.w3.org/ns/json-ld#expanded"));
        jsonLdTest("http://manu.sporny.org/", "expanded.jsonld", "expanded-expected.jsonld", mediaType);
    }

    @Test
    public void testJsonLdFlattened() throws IOException {
        final MediaType mediaType = new MediaType("application", "ld+json",
                Map.of("profile", "http://www.w3.org/ns/json-ld#flattened"));
        jsonLdTest("http://me.markus-lanthaler.com/", "flattened.jsonld", "flattened-expected.jsonld", mediaType);
    }

    @Test
    public void testJsonLdCompacted() throws IOException {
        final MediaType mediaType = new MediaType("application", "ld+json",
                Map.of("profile", "http://www.w3.org/ns/json-ld#compacted"));
        jsonLdTest("http://manu.sporny.org/", "compacted.jsonld", "compacted-expected.jsonld", mediaType);
    }

    /**
     * Test JSON-LD serialization.
     * @param id The node ID
     * @param sourceDoc The source JSON-LD document
     * @param expectedDoc The expected JSON-LD document
     * @param mediaType The media type
     * @throws IOException If an error occurs getting the documents as streams
     */
    private void jsonLdTest(final String id, final String sourceDoc, final String expectedDoc,
                            final MediaType mediaType) throws IOException {
        // Load the input JSON-LD
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(sourceDoc)) {
            final Model model = createDefaultModel();
            RDFParser.create().source(input).lang(Lang.JSONLD11).parse(model);

            // Prepare the RDF stream
            final RdfStream testStream = new DefaultRdfStream(
                    NodeFactory.createURI(id),
                    model.getGraph().find().toList().stream()
            );

            // Set up namespaces and media type for flattened JSON-LD
            final Map<String, String> namespaces = new HashMap<>();
            namespaces.put("foaf", "http://xmlns.com/foaf/0.1/");

            // Serialize using RdfStreamStreamingOutput
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            new RdfStreamStreamingOutput(testStream, namespaces, mediaType).write(output);
            LOGGER.debug("Output was: {}", output.toString(StandardCharsets.UTF_8));

            // Parse the output JSON-LD
            final JsonReader reader = createReader(new ByteArrayInputStream(output.toByteArray()));
            final JsonStructure resultObj = reader.read();
            reader.close();

            // Load the expected output JSON-LD
            try (InputStream expectedInput = getClass().getClassLoader().getResourceAsStream(expectedDoc)) {
                final JsonReader expectedReader = createReader(expectedInput);
                final var expectedObj = expectedReader.read();
                expectedReader.close();

                // Compare the @graph arrays (order-insensitive)
                assertJsonMatch(expectedObj, resultObj);
            }
        }
    }

    /**
     * Assert that two JSON objects match, ignoring order.
     * TODO: This may not be the best way to do this. I'm not sure the matching and traversal is correct.
     * @param expected The expected JSON object
     * @param actual The actual JSON object
     */
    private static void assertJsonMatch(final JsonValue expected, final JsonValue actual) {
        if (expected.getValueType() != actual.getValueType()) {
            throw new AssertionError("Type mismatch: expected " + expected.getValueType()
                    + " but got " + actual.getValueType());
        }
        switch (expected.getValueType()) {
            case OBJECT -> assertJsonObjectsMatch(expected.asJsonObject(), actual.asJsonObject());
            case ARRAY  -> assertJsonArraysMatch(expected.asJsonArray(),  actual.asJsonArray());
            default     -> {
                if (!expected.equals(actual)) {
                    throw new AssertionError("Value mismatch: expected " + expected + " but got " + actual);
                }
            }
        }
    }

    private static void assertJsonObjectsMatch(final JsonObject expected, final JsonObject actual) {
        if (expected.size() != actual.size()) {
            throw new AssertionError("Object size mismatch: expected " + expected.size() + " got " + actual.size());
        }
        for (var e : expected.entrySet()) {
            if (!actual.containsKey(e.getKey())) {
                throw new AssertionError("Missing key in actual: " + e.getKey());
            }
            assertJsonMatch(e.getValue(), actual.get(e.getKey()));
        }
    }

    private static void assertJsonArraysMatch(final JsonArray expected, final JsonArray actual) {
        if (expected.size() != actual.size()) {
            throw new AssertionError("Array size mismatch: expected " + expected.size() + " got " + actual.size());
        }
        for (int i = 0; i < expected.size(); i++) {
            assertJsonMatch(expected.get(i), actual.get(i));
        }
    }
}
