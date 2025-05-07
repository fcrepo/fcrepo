/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.responses;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.config.SystemInfoConfig;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.search.api.PaginationInfo;
import org.fcrepo.search.api.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

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
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;

import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * <p>BaseHtmlProviderTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SearchResultProviderTest {

    private final SearchResultProvider testProvider = new SearchResultProvider();

    private SearchResult testData;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private SystemInfoConfig mockSystemInfoConfig;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private FedoraPropsConfig mockFedoraPropsConfig;

    @BeforeEach
    public void setup() throws Exception {

        final var namespaces = Map.of("test", "info");
        final var base_uri = "http://localhost:8080/rest/";
        final URI baseUri = URI.create(base_uri);
        final UriBuilder baseUriBuilder = UriBuilder.fromUri(baseUri);
        final List<Map<String, Object>> items = List.of(Map.of("foo","bar"));
        final var pagination = new PaginationInfo(1, 0, 1);
        testData = new SearchResult(items, pagination);

        when(mockUriInfo.getBaseUri()).thenReturn(baseUri);
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(baseUriBuilder);
        when(mockSystemInfoConfig.getGitCommit()).thenReturn("some-commit");
        when(mockSystemInfoConfig.getImplementationVersion()).thenReturn("some-version");
        when(mockRequest.getHeader(ATOMIC_ID_HEADER)).thenReturn(null);

        when(mockFedoraPropsConfig.getVelocityLog()).thenReturn(Path.of("/logs"));
        final var ocflProps = new OcflPropsConfig();
        ocflProps.setAutoVersioningEnabled(true);
        setField(testProvider, "fedoraPropsConfig", mockFedoraPropsConfig);

        setField(testProvider, "uriInfo", mockUriInfo);
        setField(testProvider, "systemInfoConfig", mockSystemInfoConfig);
        setField(testProvider, "request", mockRequest);
        testProvider.init();
    }

    @Test
    public void testIsWriteable() {
        assertFalse(
                testProvider.isWriteable(RdfNamespacedStream.class, RdfNamespacedStream.class, null,
                        TEXT_HTML_TYPE),
                "Gave true response to SearchResultProvider.isWriteable() that contained legitimate combination of " +
                        "parameters");
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
        testProvider.writeTo(testData, RdfNamespacedStream.class, mock(Type.class),
                new Annotation[]{}, MediaType.valueOf("text/html"),
                new MultivaluedHashMap<>(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue(results.length > 0, "Got no output from serialization!");

    }

}
