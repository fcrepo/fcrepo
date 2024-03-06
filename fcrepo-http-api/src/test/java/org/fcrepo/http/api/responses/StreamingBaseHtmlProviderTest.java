/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.responses;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.google.common.collect.ImmutableMap;
import org.apache.jena.graph.Triple;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.fcrepo.config.SystemInfoConfig;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <p>BaseHtmlProviderTest class.</p>
 *
 * @author awoods
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StreamingBaseHtmlProviderTest {

    private RdfNamespacedStream testData;
    private RdfNamespacedStream testData2;
    private Map<String, String> namespaces = Map.of("fedora", "http://fedora.info/definitions/v4/repository#");

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private SystemInfoConfig mockSystemConfig;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private ResourceFactory mockResourceFactory;

    @InjectMocks
    private final StreamingBaseHtmlProvider testProvider = new StreamingBaseHtmlProvider();

    @BeforeEach
    public void setup() throws Exception {

        final URI baseUri = URI.create("http://localhost:8080/rest/");
        final UriBuilder baseUriBuilder = UriBuilder.fromUri(baseUri);
        final var subject1 = baseUri.resolve("test%3Asubject").toString();
        final var subject2 = baseUri.resolve("test%3Asubject2").toString();
        final Stream<Triple> triples = of(new Triple(createURI(subject1),
                createURI("test:predicate"),
                createLiteral("test:object")), new Triple(createURI(subject1),
                type.asNode(), FEDORA_BINARY.asNode()));
        final Stream<Triple> triples2 = of(new Triple(createURI(subject2),
                type.asNode(), FEDORA_CONTAINER.asNode()));
        @SuppressWarnings("resource")
        final DefaultRdfStream stream = new DefaultRdfStream(createURI(subject1), triples);
        @SuppressWarnings("resource")
        final DefaultRdfStream stream2 = new DefaultRdfStream(createURI(subject2), triples2);
        testData = new RdfNamespacedStream(stream, namespaces);

        testData2 = new RdfNamespacedStream(stream2, namespaces);

        when(mockUriInfo.getBaseUri()).thenReturn(baseUri);
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(baseUriBuilder);

        when(mockResource.isOriginalResource()).thenReturn(true);
        when(mockResource.getSystemTypes(false)).thenReturn(List.of(URI.create(NON_RDF_SOURCE.getURI()),
                URI.create(FEDORA_BINARY.getURI())));
        when(mockResource.isMemento()).thenReturn(false);
        when(mockResourceFactory.getResource(any(Transaction.class), any(FedoraId.class))).thenReturn(mockResource);


        //setField(testProvider, "uriInfo", mockUriInfo);
    }

    @Test
    public void testIsWriteable() {
        assertTrue(
                testProvider.isWriteable(RdfNamespacedStream.class, RdfNamespacedStream.class, null, TEXT_HTML_TYPE),
                "Gave false response to HtmlProvider.isWriteable() with correct combination of parameters");
        assertFalse(
                testProvider.isWriteable(RdfStream.class, RdfStream.class,
                                null, TEXT_HTML_TYPE),
                "Gave true response to HtmlProvider.isWriteable() with an incorrect combination of parameters");
        assertFalse(
                testProvider.isWriteable(StreamingBaseHtmlProvider.class,
                                StreamingBaseHtmlProvider.class, null, TEXT_HTML_TYPE),
                "HtmlProvider.isWriteable() should return false if asked to serialize a non-RdfNamespacedStream!");
        assertFalse(
                testProvider.isWriteable(RdfNamespacedStream.class, RdfNamespacedStream.class,
                                null, TEXT_PLAIN_TYPE),
                "HtmlProvider.isWriteable() should return false to text/plain!");
    }

    @Test
    public void testGetSize() {
        assertEquals(-1, testProvider.getSize(null, null, null, null, null),
                "Returned wrong size from HtmlProvider!");

    }

    @Test
    public void testWriteTo() throws WebApplicationException,
            IllegalArgumentException, IOException {
        final Template mockTemplate = mock(Template.class);
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(isA(Context.class), isA(Writer.class));
        setField(testProvider, "templatesMap", singletonMap(FEDORA_BINARY.getURI(),
                mockTemplate));
        testProvider.writeTo(testData, RdfNamespacedStream.class, mock(Type.class),
                new Annotation[]{}, MediaType.valueOf("text/html"),
                new MultivaluedHashMap<>(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue(results.length > 0, "Got no output from serialization!");

    }

    @Test
    public void testWriteToWithAnnotation() throws WebApplicationException,
            IllegalArgumentException, IOException {
        final Template mockTemplate = mock(Template.class);
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(isA(Context.class), isA(Writer.class));

        setField(testProvider, "templatesMap",
                ImmutableMap.of("some:file", mockTemplate));
        final HtmlTemplate mockAnnotation = mock(HtmlTemplate.class);
        when(mockAnnotation.value()).thenReturn("some:file");
        testProvider.writeTo(testData, RdfNamespacedStream.class, mock(Type.class),
                new Annotation[]{mockAnnotation}, MediaType
                        .valueOf("text/html"),
                new MultivaluedHashMap<>(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue(results.length > 0, "Got no output from serialization!");

    }

    @Test
    public void testWriteToWithParentTemplate() throws WebApplicationException,
            IllegalArgumentException, IOException {
        final Template mockTemplate = mock(Template.class);
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        doAnswer(new Answer<Object>() {

            @Override
                public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(isA(Context.class), isA(Writer.class));

        setField(testProvider, "templatesMap",
                 ImmutableMap.of(FEDORA_CONTAINER.getURI(), mockTemplate));
        testProvider.writeTo(testData2, RdfNamespacedStream.class, mock(Type.class),
                new Annotation[] {}, MediaType
                        .valueOf("text/html"),
                new MultivaluedHashMap<>(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue(results.length > 0, "Got no output from serialization!");
    }
}
