/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static java.net.URLEncoder.encode;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.fcrepo.stats.api.AggregatedRepositoryStatsResults;
import org.fcrepo.stats.api.RepositoryStatsByMimeTypeResults;
import org.fcrepo.stats.api.RepositoryStatsByRdfTypeResults;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Integration tests for the stats endpoint
 *
 * @author dbernstein
 * @since 01/28/2022
 */
@TestExecutionListeners(
        listeners = {TestIsolationExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraRepositoryStatsIT extends AbstractResourceIT {

    public static final String BINARIES_SUBPATH = "/binaries";
    private static final String RDF_TYPES_SUBPATH = "/rdf-types" ;
    private static Logger LOGGER = LoggerFactory.getLogger(FedoraRepositoryStatsIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String getStatsEndpoint() {
        return serverAddress + "fcr:stats";
    }

    private List<String> createResources(final int count) throws IOException {
        return createResources(getRandomUniqueId(), count);
    }

    private List<String> createResources(final String prefix, final int count) throws IOException {
        final var resources = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            try (final CloseableHttpResponse response = createObject(prefix + "-" + String.format("%05d", i))) {
                resources.add(getLocation(response));
            }
        }
        return resources;
    }

    private List<String> createBinaries(final String prefix, final int count, final String content,
                                        final Header... header) throws IOException {
        final var resources = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            try (final CloseableHttpResponse response = createObject(prefix + "-" + String.format("%05d", i),
                    content, header)) {
                resources.add(getLocation(response));
            }
        }
        return resources;
    }

    private CloseableHttpResponse createObject(final String pid, final String body, final Header... headers) {
        final HttpPost httpPost = postObjMethod("/");
        if (isNotEmpty(pid)) {
            httpPost.addHeader("Slug", encode(pid, StandardCharsets.UTF_8));
        }

        for (Header header : headers) {
            httpPost.addHeader(header);
        }

        try {
            httpPost.setEntity(new StringEntity(body));
        } catch (UnsupportedEncodingException e) {
        }

        try {
            final CloseableHttpResponse response = execute(httpPost);
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            return response;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetStatsEmptyRepo() throws Exception {

        final String statsUrl = getStatsEndpoint();
        try (final CloseableHttpResponse response = execute(new HttpGet(statsUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final AggregatedRepositoryStatsResults result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    AggregatedRepositoryStatsResults.class);
            assertNotNull(result);
            assertEquals(0, result.getResourceCount()
                    .longValue());
        }
    }

    @Test
    public void testGetStats() throws Exception {

        final String statsUrl = getStatsEndpoint();
        final var newResourcesCount = 3l;
        createResources((int) newResourcesCount);

        try (final CloseableHttpResponse response = execute(new HttpGet(statsUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final AggregatedRepositoryStatsResults result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    AggregatedRepositoryStatsResults.class);
            assertNotNull(result);
            assertEquals(newResourcesCount, result.getResourceCount()
                    .longValue());
        }
    }

    private void testNotAcceptable(final String url) throws Exception {
        final var get = new HttpGet(url);
        get.addHeader("Accept", "text/plain");
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(NOT_ACCEPTABLE.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testGetStatsNotAcceptable() throws Exception {
        testNotAcceptable(getStatsEndpoint());
        testNotAcceptable(getBinaryStatsEndpoint());
        testNotAcceptable(getRdfTypesStatsEndpoint());
    }

    private int getResourceCountByMimetype(final RepositoryStatsByMimeTypeResults results, final String mimeType) {
        return results.getMimeTypes().stream().filter(x -> x.getMimeType().equals(mimeType))
                .findFirst()
                .get()
                .getResourceCount()
                .intValue();
    }

    @Test
    public void testGetStatsByMimeTypeUnfiltered() throws Exception {
        final var textContent = "hello world 1234";
        final var textCount = 1;
        final var imageContent = "image-data 12345";
        final var imageCount = 2;
        final var audioContent = "audio-data 123456";
        final var audioCount = 3;
        createBinaries("text", textCount, textContent,
                new BasicHeader("Content-Type", "text/plain"));
        createBinaries("image", imageCount, imageContent,
                new BasicHeader("Content-Type", "image/jpg"));
        createBinaries("audio", audioCount, audioContent,
                new BasicHeader("Content-Type", "audio/mp4"));

        final String statsUrl = getBinaryStatsEndpoint();
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final var result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    RepositoryStatsByMimeTypeResults.class);
            assertNotNull(result);
            LOGGER.info("result={}", result);
            assertEquals(3, result.getMimeTypes().size());
            assertEquals(textCount + imageCount + audioCount, result.getResourceCount().intValue());
            assertEquals(textCount, getResourceCountByMimetype(result, "text/plain"));
            assertEquals(imageCount, getResourceCountByMimetype(result, "image/jpg"));
            assertEquals(audioCount, getResourceCountByMimetype(result, "audio/mp4"));

            final var expectedBytes =
                    (textContent.getBytes().length * textCount) + (imageContent.getBytes().length * imageCount) +
                            (audioContent.getBytes().length * audioCount);
            assertEquals(expectedBytes, result.getByteCount().intValue());
        }
    }

    @Test
    public void testGetStatsFilteredByMimetype() throws Exception {
        createBinaries("text", 1, "hello world",
                new BasicHeader("Content-Type", "text/plain"));
        createBinaries("image", 1, "image-data",
                new BasicHeader("Content-Type", "image/jpg"));
        createBinaries("audio", 2, "audio-data",
                new BasicHeader("Content-Type", "audio/mp4"));

        final String statsUrl = getBinaryStatsEndpoint() + "?mime_type=text/plain&mime_type=audio/mp4";
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final var result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    RepositoryStatsByMimeTypeResults.class);
            assertNotNull(result);
            LOGGER.info("result={}", result);
            assertEquals(2, result.getMimeTypes().size());
            assertEquals(3, result.getResourceCount().intValue());
            assertTrue(result.getByteCount().intValue() > 0);
            assertEquals(1, getResourceCountByMimetype(result, "text/plain"));
            assertEquals(2, getResourceCountByMimetype(result, "audio/mp4"));
        }
    }

    @Test
    public void testGetStatsFilteredByNonExistentMimetype() throws Exception {
        createBinaries("text", 1, "hello world",
                new BasicHeader("Content-Type", "text/plain"));

        final String statsUrl = getBinaryStatsEndpoint() + "?mime_type=text/blah";
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final var result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    RepositoryStatsByMimeTypeResults.class);
            assertNotNull(result);
            assertEquals(0, result.getMimeTypes().size());
            assertEquals(0, result.getResourceCount().intValue());
            assertEquals(0, result.getByteCount().intValue());
        }
    }

    @Test
    public void testGetStatsFilteredByBlankMimetype() throws Exception {
        final String statsUrl = getBinaryStatsEndpoint() + "?mime_type=";
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    private String getBinaryStatsEndpoint() {
        return getStatsEndpoint() + BINARIES_SUBPATH;
    }

    private String getRdfTypesStatsEndpoint() {
        return getStatsEndpoint() + RDF_TYPES_SUBPATH;
    }

    @Test
    public void testGetStatsbyRdfType() throws Exception {
        createResources(1);
        final var textContent = "hello world";
        createBinaries("text", 1, textContent,
                new BasicHeader("Content-Type", "text/plain"));

        final String statsUrl = getRdfTypesStatsEndpoint();
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final var result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    RepositoryStatsByRdfTypeResults.class);
            assertNotNull(result);
            LOGGER.info("result={}", result);
            final var containerTypes = List.of("http://www.w3.org/ns/ldp#BasicContainer",
                    "http://www.w3.org/ns/ldp#Container");
            for (String ctype : containerTypes) {
                assertEquals(1, getResourceCountByRdfType(result, ctype));
            }
            // Binary description is also an RDFSource, along with the container
            final var rdfSourceTypes = List.of("http://www.w3.org/ns/ldp#RDFSource");
            for (String rtype : rdfSourceTypes) {
                assertEquals(2, getResourceCountByRdfType(result, rtype));
            }
            final var binaryTypes = List.of("http://www.w3.org/ns/ldp#NonRDFSource");
            for (String btype : binaryTypes) {
                assertEquals(1, getResourceCountByRdfType(result, btype));
            }
            assertEquals(3, getResourceCountByRdfType(result, "http://www.w3.org/ns/ldp#Resource"));
        }
    }

    @Test
    public void testGetStatsFilteredByBlankRdfType() throws Exception {
        final String statsUrl = getRdfTypesStatsEndpoint() + "?rdf_type=";
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testGetStatsFilteredByRdfType() throws Exception {
        //create container
        createResources(1);
        //create a binary
        final var content = "hello world";
        createBinaries("text", 1, content,
                new BasicHeader("Content-Type", "text/plain"));

        final var binaryType = "http://www.w3.org/ns/ldp#NonRDFSource";

        //get filtered stats list based on binaries only
        final String statsUrl = getRdfTypesStatsEndpoint() + "?rdf_type=" + encode(binaryType, "UTF-8");
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final var result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    RepositoryStatsByRdfTypeResults.class);
            assertNotNull(result);
            LOGGER.info("result={}", result);
            //assertEquals(6, result.getResourceTypes().size());
            final var containerTypes = Arrays.asList("http://www.w3.org/ns/ldp#BasicContainer",
                    "http://www.w3.org/ns/ldp#Container", "http://www.w3.org/ns/ldp#RDFSource");

            //verify no container types were returned.
            for (String ctype : containerTypes) {
                assertEquals(0, getResourceCountByRdfType(result, ctype));
            }
            assertEquals(1, getResourceCountByRdfType(result, "http://www.w3.org/ns/ldp#NonRDFSource"));
        }
    }

    @Test
    public void testGetStatsFilteredByNonExistentRdfType() throws Exception {
        //create container
        createResources(1);
        //create a binary
        createBinaries("text", 1, "hello-world",
                new BasicHeader("Content-Type", "text/plain"));

        final var binaryType = "not-an-rdf-type";

        //get filtered stats list based on binaries only
        final String statsUrl = getRdfTypesStatsEndpoint() + "?rdf_type=" + encode(binaryType,
                "UTF-8");
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final var result = OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                    RepositoryStatsByRdfTypeResults.class);
            assertNotNull(result);
            assertEquals(0, result.getRdfTypes().size());
        }
    }

    private int getResourceCountByRdfType(final RepositoryStatsByRdfTypeResults results, final String rdfTypeUri) {
        final var first = results.getRdfTypes().stream().filter(x -> x.getResourceType().equals(rdfTypeUri))
                .findFirst();
        return first.isPresent() ? first.get().getResourceCount().intValue() : 0;
    }
}
