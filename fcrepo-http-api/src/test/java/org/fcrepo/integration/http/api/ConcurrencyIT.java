/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.integration.http.api;

import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

import com.google.common.base.Stopwatch;

/**
 * @author pwinckles
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ConcurrencyIT extends AbstractResourceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrencyIT.class);

    private static final int THREAD_COUNT = 4;

    private static ExecutorService executor;

    @BeforeAll
    public static void beforeClass() {
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @AfterAll
    public static void afterClass() {
        executor.shutdown();
    }

    @Test
    public void basicContainerPosts() {
        final var phaser = new Phaser(THREAD_COUNT + 1);
        final var testDuration = Duration.ofSeconds(15);
        final var tasks = new ArrayList<Future<Void>>(THREAD_COUNT);
        final var succeeded = new AtomicInteger(0);
        final var failed = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(executor.submit(() -> {
                phaser.arriveAndAwaitAdvance();
                final var stopwatch = Stopwatch.createStarted();

                while (stopwatch.elapsed().compareTo(testDuration) < 0) {
                    if (postCreateContainer()) {
                        succeeded.incrementAndGet();
                    } else {
                        failed.incrementAndGet();
                    }
                }
                return null;
            }));
        }

        phaser.arriveAndAwaitAdvance();

        tasks.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        final var total = succeeded.get() + failed.get();

        assertEquals(0, failed.get(),
                String.format("%s requests out of %s failed", failed.get(), total));
    }

    private boolean postCreateContainer() {
        final var post = postObjMethod();
        post.setHeader(LINK, BASIC_CONTAINER_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(post)) {
            if (Objects.equals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode())) {
                return true;
            } else {
                LOGGER.error("Concurrent request failed: {}", response.getStatusLine());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to execute request", e);
            return false;
        }
        return false;
    }

}
