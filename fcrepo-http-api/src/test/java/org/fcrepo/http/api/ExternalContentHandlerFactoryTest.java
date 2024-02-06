/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;

import jakarta.ws.rs.core.Link;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExternalContentHandlerFactoryTest {

    @Mock
    private ExternalContentPathValidator validator;

    private ExternalContentHandlerFactory factory;

    @BeforeEach
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

    @Test
    public void testValidationFailure() {
        doThrow(new ExternalMessageBodyException("")).when(validator).validate(anyString());

        assertThrows(ExternalMessageBodyException.class,
                () -> factory.createFromLinks(makeLinks("https://fedora.info/")));
    }

    @Test
    public void testMultipleExtLinkHeaders() {
        final List<String> links = makeLinks("https://fedora.info/", "https://fedora.info/");

        assertThrows(ExternalMessageBodyException.class, () -> factory.createFromLinks(links));
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
