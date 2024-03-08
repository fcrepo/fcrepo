/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.responses;

import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Stream.of;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.jena.graph.Triple;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.config.SystemInfoConfig;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * <p>BaseHtmlProviderTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamingBaseHtmlProviderTest {

    private final StreamingBaseHtmlProvider testProvider = new StreamingBaseHtmlProvider();

    private RdfNamespacedStream testData;
    private RdfNamespacedStream testData2;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private SystemInfoConfig mockSystemInfoConfig;

    @Mock
    private ResourceFactory mockResourceFactory;

    @Mock
    private FedoraResource mockResource1;

    @Mock
    private FedoraResource mockResource2;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private FedoraPropsConfig mockFedoraPropsConfig;

    @Before
    public void setup() throws Exception {

        final var namespaces = Map.of("test", "info");
        final var base_uri = "http://localhost:8080/rest/";
        final var external_id1 = base_uri + "subject";
        final var external_id2 = base_uri + "subject2";
        final var internal_id1 = FEDORA_ID_PREFIX + "/subject";
        final var internal_id2 = FEDORA_ID_PREFIX + "/subject2";
        final Stream<Triple> triples = of(new Triple(createURI(external_id1), createURI("test:predicate"),
                createLiteral("test:object")), new Triple(createURI(external_id1), type.asNode(),
                FEDORA_BINARY.asNode()));
        final Stream<Triple> triples2 = of(new Triple(createURI(external_id2), type.asNode(),
                FEDORA_CONTAINER.asNode()));
        @SuppressWarnings("resource")
        final DefaultRdfStream stream = new DefaultRdfStream(createURI(external_id1), triples);
        @SuppressWarnings("resource")
        final DefaultRdfStream stream2 = new DefaultRdfStream(createURI(external_id2), triples2);
        testData = new RdfNamespacedStream(stream, namespaces);

        testData2 = new RdfNamespacedStream(stream2, namespaces);

        final URI baseUri = URI.create(base_uri);
        final UriBuilder baseUriBuilder = UriBuilder.fromUri(baseUri);
        when(mockUriInfo.getBaseUri()).thenReturn(baseUri);
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(baseUriBuilder);
        when(mockSystemInfoConfig.getGitCommit()).thenReturn("some-commit");
        when(mockSystemInfoConfig.getImplementationVersion()).thenReturn("some-version");
        when(mockRequest.getHeader(ATOMIC_ID_HEADER)).thenReturn(null);

        when(mockResource1.isOriginalResource()).thenReturn(true);
        when(mockResource1.getDescribedResource()).thenReturn(mockResource1);
        when(mockResource2.isOriginalResource()).thenReturn(true);
        when(mockResource2.getDescribedResource()).thenReturn(mockResource2);

        when(mockResourceFactory.getResource(org.mockito.ArgumentMatchers.any(Transaction.class),
                eq(FedoraId.create(internal_id1)))).thenReturn(mockResource1);
        when(mockResourceFactory.getResource(org.mockito.ArgumentMatchers.any(Transaction.class),
                eq(FedoraId.create(internal_id2)))).thenReturn(mockResource2);

        when(mockFedoraPropsConfig.getVelocityLog()).thenReturn(Path.of("/logs"));
        final var ocflProps = new OcflPropsConfig();
        ocflProps.setAutoVersioningEnabled(true);
        setField(testProvider, "ocflPropsConfig", ocflProps);
        setField(testProvider, "fedoraPropsConfig", mockFedoraPropsConfig);

        setField(testProvider, "uriInfo", mockUriInfo);
        setField(testProvider, "systemInfoConfig", mockSystemInfoConfig);
        setField(testProvider, "request", mockRequest);
        setField(testProvider, "resourceFactory", mockResourceFactory);
        testProvider.init();
    }

    @Test
    public void testIsWriteable() {
        assertTrue(
                "Gave false response to HtmlProvider.isWriteable() that contained legitimate combination of parameters",
                testProvider.isWriteable(RdfNamespacedStream.class, RdfNamespacedStream.class,
                        null, TEXT_HTML_TYPE));
        assertFalse(
                "Gave true response to HtmlProvider.isWriteable() with an incorrect combination of parameters",
                testProvider.isWriteable(RdfStream.class, RdfStream.class,
                        null, TEXT_HTML_TYPE));
        assertFalse(
                "HtmlProvider.isWriteable() should return false if asked to serialize a non-RdfNamespacedStream!",
                testProvider.isWriteable(StreamingBaseHtmlProvider.class,
                        StreamingBaseHtmlProvider.class, null, TEXT_HTML_TYPE));
        assertFalse(
                "HtmlProvider.isWriteable() should return false to text/plain!",
                testProvider.isWriteable(RdfNamespacedStream.class, RdfNamespacedStream.class,
                        null, TEXT_PLAIN_TYPE));
    }

    @Test
    public void testGetSize() {
        assertEquals("Returned wrong size from HtmlProvider!", testProvider
                .getSize(null, null, null, null, null), -1);

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
        setField(testProvider, "templatesMap", singletonMap(FEDORA_BINARY.getURI(), mockTemplate));
        testProvider.writeTo(testData, RdfNamespacedStream.class, mock(Type.class),
                new Annotation[]{}, MediaType.valueOf("text/html"),
                new MultivaluedHashMap<>(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue("Got no output from serialization!", results.length > 0);

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
        assertTrue("Got no output from serialization!", results.length > 0);

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
        assertTrue("Got no output from serialization!", results.length > 0);
    }
}
