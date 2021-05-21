/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.integration.http.api;

import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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

    @BeforeClass
    public static void beforeClass() {
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @AfterClass
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

        assertEquals(String.format("%s requests out of %s failed", failed.get(), total),
                0, failed.get());
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
