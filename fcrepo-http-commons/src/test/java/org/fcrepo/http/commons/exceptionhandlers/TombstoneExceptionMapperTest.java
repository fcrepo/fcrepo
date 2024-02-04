/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author cabeer
 */
public class TombstoneExceptionMapperTest {

    @Mock
    private FedoraResource mockResource;

    private final FedoraId fedoraId = FedoraId.create("/some:uri");

    private final Instant deleteTime = Instant.now();

    private ExceptionMapper<TombstoneException> testObj;

    private static final String SERVER_URI = "http://localhost:8080/rest(.*)";

    private final HttpIdentifierConverter idConverter = new HttpIdentifierConverter(UriBuilder.fromUri(SERVER_URI));

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockResource.getFedoraId()).thenReturn(fedoraId);
        when(mockResource.getLastModifiedDate()).thenReturn(deleteTime);
        testObj = new TombstoneExceptionMapper();
    }

    @Test
    public void testUrilessException() {
        final Response response = testObj.toResponse(new TombstoneException(mockResource));
        assertEquals(GONE.getStatusCode(), response.getStatus());
        assertTombstone(response, fedoraId.getFullIdPath(), null);
    }

    @Test
    public void testExceptionWithUri() {
        final String tombstone = idConverter.toExternalId(fedoraId.asTombstone().getFullId());
        final String timemap = idConverter.toExternalId(fedoraId.resolve(FCR_VERSIONS).getFullId());
        final Response response = testObj.toResponse(new TombstoneException(mockResource, tombstone, timemap));
        assertEquals(GONE.getStatusCode(), response.getStatus());
        assertTombstone(response, fedoraId.getFullIdPath(), tombstone);
        final List<Link> links = Arrays.stream(response.getHeaderString(LINK).split(",")).map(Link::valueOf)
                .collect(Collectors.toList());
        assertLinkHeaderExists(links, timemap, "timemap");
    }

    private void assertTombstone(final Response response, final String tombstoneAt, final String tombstoneUri) {
        if (tombstoneUri == null) {
            assertNull(response.getHeaderString(LINK));
        } else {
            final List<Link> links = Arrays.stream(response.getHeaderString(LINK).split(",")).map(Link::valueOf)
                    .collect(Collectors.toList());

            assertLinkHeaderExists(links, tombstoneUri, "hasTombstone");
        }
        final String expectedString = "Discovered tombstone resource at " + tombstoneAt + ", departed at: " +
                ISO_INSTANT.withZone(UTC).format(deleteTime);
        assertEquals(expectedString, response.getEntity().toString());
    }

    private void assertLinkHeaderExists(final List<Link> links, final String uri, final String rel) {
        final URI uriUri = URI.create(uri);
        if (links.stream().noneMatch(l -> l.getUri().equals(uriUri) && l.getRel().equals(rel))) {
            fail(String.format("Did not find expected Link header with uri (%s) and rel (%s)", uri, rel));
        }
    }
}
