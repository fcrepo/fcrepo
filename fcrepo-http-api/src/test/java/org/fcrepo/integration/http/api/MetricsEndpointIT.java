/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;
import java.net.ConnectException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Integration test for the prometheus metrics endpoint.
 * @author whikloj
 */
@TestExecutionListeners(
        listeners = {TestIsolationExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class MetricsEndpointIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(MetricsEndpointIT.class);

    static {
        System.setProperty("fcrepo.metrics.enable", "true");
    }

    @AfterAll
    public static void cleanup() {
        // Reset the property so it doesn't impact other tests
        System.clearProperty("fcrepo.metrics.enable");
    }

    @Test
    public void testGetMetrics() throws IOException {
        final HttpGet get = new HttpGet(serverAddress + "/prometheus");
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(SC_OK, getStatus(response), "Metrics endpoint should return 200 OK");
            // Additional assertions can be added here to validate the content of the metrics
        } catch (final NoHttpResponseException | ConnectException e) {
            LOGGER.error("Failed to connect to metrics endpoint", e);
            throw new RuntimeException("Metrics endpoint is not available", e);
        }
    }
}
