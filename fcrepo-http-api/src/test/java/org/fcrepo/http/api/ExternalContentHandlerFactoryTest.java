/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Link;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author bbpennel
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ExternalContentHandlerFactoryTest {

    @Mock
    private ExternalContentPathValidator validator;

    private ExternalContentHandlerFactory factory;

    @Before
    public void init() {
        factory = new ExternalContentHandlerFactory();
        factory.setValidator(validator);
    }

    @Test
    public void testValidLinkHeader() {
        final ExternalContentHandler handler = factory.createFromLinks(
                makeLinks("https://fedora.info/"));

        assertEquals("https://fedora.info/", handler.getURL());
        assertEquals("text/plain", handler.getContentType());
        assertEquals("proxy", handler.getHandling());
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testValidationFailure() {
        doThrow(new ExternalMessageBodyException("")).when(validator).validate(anyString());

        factory.createFromLinks(makeLinks("https://fedora.info/"));
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testMultipleExtLinkHeaders() {
        final List<String> links = makeLinks("https://fedora.info/", "https://fedora.info/");

        factory.createFromLinks(links);
    }

    private List<String> makeLinks(final String... uris) {
        return Arrays.stream(uris)
                .map(uri -> Link.fromUri(uri)
                        .rel(EXTERNAL_CONTENT.toString())
                        .param("handling", "proxy")
                        .type("text/plain")
                        .build()
                        .toString())
                .collect(Collectors.toList());
    }
}
