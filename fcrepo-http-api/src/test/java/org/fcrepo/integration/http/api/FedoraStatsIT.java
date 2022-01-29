/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
        return serverAddress + "fcr:stats?";
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

    @Test
    public void testGetStats() throws Exception {

        final String searchUrl = getStatsEndpoint();

        long currentCount = 0;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final StatsResults result = objectMapper.readValue(response.getEntity().getContent(), StatsResults.class);
            assertNotNull(result);
            currentCount = result.getResourceCount();
        }


        final var resourceCount = 3l;
        final var resources = createResources((int) resourceCount);

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final StatsResults result = objectMapper.readValue(response.getEntity().getContent(), StatsResults.class);
            assertNotNull(result);
            assertEquals(resourceCount + currentCount, result.getResourceCount().longValue());
        }
    }
}
