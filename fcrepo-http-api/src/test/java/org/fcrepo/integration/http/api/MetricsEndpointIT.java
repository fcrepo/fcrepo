/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.io.IOException;
import java.net.ConnectException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Integration test for the prometheus metrics endpoint.
 * @author whikloj
 */
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                TestIsolationExecutionListener.class,
                DirtyContextBeforeAndAfterClassTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class MetricsEndpointIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(MetricsEndpointIT.class);

    static {
        // Ensure the metrics property is set before tests run
        System.setProperty("fcrepo.metrics.enabled", "true");
    }

    @BeforeEach
    public void setup() throws IOException {
        await().atMost(15, java.util.concurrent.TimeUnit.SECONDS)
                .pollInterval(30, java.util.concurrent.TimeUnit.MILLISECONDS)
                .until(() -> {
                    try {
                        final HttpGet get = new HttpGet(serverAddress);
                        try (final CloseableHttpResponse response = execute(get)) {
                            return getStatus(response) == SC_OK;
                        }
                    } catch (final NoHttpResponseException | ConnectException e) {
                        LOGGER.debug("Waiting for fedora to become available");
                        return false;
                    }
                });
        // Now that fedora has started, clear the property so it won't impact other tests
        System.clearProperty("fcrepo.metrics.enabled");
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
