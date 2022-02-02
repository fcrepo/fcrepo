/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.fcrepo.stats.api.StatsResults;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author dbernstein
 * @since 01/28/2022
 */
@TestExecutionListeners(
        listeners = {TestIsolationExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraStatsIT extends AbstractResourceIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            httpPost.addHeader("Slug", URLEncoder.encode(pid, StandardCharsets.UTF_8));
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
    public void testGetStats() throws Exception {

        final String statsUrl = getStatsEndpoint();

        long currentCount = 0;
        try (final CloseableHttpResponse response = execute(new HttpGet(statsUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final StatsResults result = objectMapper.readValue(response.getEntity().getContent(), StatsResults.class);
            assertNotNull(result);
            currentCount = result.getAll().getResourceCount();
        }


        final var resourceCount = 3l;
        final var resources = createResources((int) resourceCount);

        try (final CloseableHttpResponse response = execute(new HttpGet(statsUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final StatsResults result = objectMapper.readValue(response.getEntity().getContent(), StatsResults.class);
            assertNotNull(result);
            assertEquals(resourceCount + currentCount, result.getAll().getResourceCount()
                    .longValue());
        }
    }

    @Test
    public void testGetStatsWithBinaries() throws Exception {
        final String statsUrl = getStatsEndpoint();
        final var imageCount = 3;
        final var textCount = 4;
        createBinaries("text", textCount, "hello world",
                new BasicHeader("Content-Type", "text/plain"));
        createBinaries("text", imageCount, "image-data",
                new BasicHeader("Content-Type", "image/jpg"));

        try (final CloseableHttpResponse response = execute(new HttpGet(statsUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final StatsResults result = objectMapper.readValue(response.getEntity().getContent(), StatsResults.class);
            assertNotNull(result);

            assertEquals(imageCount,
                    result.getMimetypes().stream().filter(x -> x.getMimetype().equals("image/jpg"))
                            .map(x -> x.getResourceCount()).findFirst().get().intValue());
            assertEquals(textCount,
                    result.getMimetypes().stream().filter(x -> x.getMimetype().equals("text/plain"))
                            .map(x -> x.getResourceCount()).findFirst().get().intValue());

            assertEquals(imageCount + textCount, result.getBinaries().getResourceCount().longValue());
        }
    }

    @Test
    public void testGetStatsFilteredByMimetype() throws Exception {
        createBinaries("text", 1, "hello world",
                new BasicHeader("Content-Type", "text/plain"));
        createBinaries("image", 1, "image-data",
                new BasicHeader("Content-Type", "image/jpg"));
        createBinaries("audio", 1, "audio-data",
                new BasicHeader("Content-Type", "audio/mp4"));

        final String statsUrl = getStatsEndpoint() + "?mime_type=text/plain&mime_type=audio/mp4";
        final var statsGet = new HttpGet(URI.create(statsUrl));
        try (final CloseableHttpResponse response = execute(statsGet)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final StatsResults result = objectMapper.readValue(response.getEntity().getContent(), StatsResults.class);
            assertNotNull(result);

            assertEquals(2, result.getMimetypes().size());
            assertEquals(1,
                    result.getMimetypes().stream().filter(x -> x.getMimetype().equals("text/plain")).count());
            assertEquals(1,
                    result.getMimetypes().stream().filter(x -> x.getMimetype().equals("audio/mp4")).count());

        }
    }
}
