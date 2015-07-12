/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.Test;

/**
 * This "test" is a utility for collecting the timing of concurrent operations operations.
 * It takes roughly 2 minutes to complete and should only be run if the timing metrics are wanted.
 * In order to activate this utility, the following System Property must be set:
 * &lt;p/&gt;
 * mvn -Dfcrepo.test.http.concurrent install
 *
 * @author lsitu
 */
public class FedoraCrudConcurrentIT extends AbstractResourceIT {

    private static final String TEST_ACTIVATION_PROPERTY = "fcrepo.test.http.concurrent";

    @Test
    public void testConcurrentIngest() throws Exception {
        setLogger();

        if (System.getProperty(TEST_ACTIVATION_PROPERTY) == null) {
            logger.info("Not running tests because system property not set: {}", TEST_ACTIVATION_PROPERTY);
            return;
        }

        final int[] numThreadsToTest = {2, 4, 8, 16, 32};
        logger.info("# Start CRUD concurrent performance testing...");
        for (int i = 0; i < numThreadsToTest.length; i++) {
            startCrudConcurrentPerformanceTest(numThreadsToTest[i]);
        }
    }

    /**
     * Test CRUD concurrent access performance:
     * create/update/delete object, create/update/delete content file
     *
     * @param numThreads
     * @throws Exception
     */
    private void startCrudConcurrentPerformanceTest(final int numThreads) throws Exception {
        String pid = null;

        // Tasks to run
        final List<HttpRunner> tasks = new ArrayList<>();
        final List<String> pids = new ArrayList<>();

        // Create object
        logger.info("# Starting " + numThreads + " concurrent threads to create object...");
        for (int i = 0; i < numThreads; i++) {
            pid = getRandomUniqueId();
            pids.add(pid);
            final String taskName = "Thread " + (i + 1) + " to create object " + pid;
            final HttpRequestBase request = postObjMethod("/");
            request.addHeader("Slug", pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(CREATED.getStatusCode());
            tasks.add(task);
        }
        startThreads(tasks);
        long totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to CREATE object: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);

        tasks.clear();
        // Update objects
        logger.info("# Starting " + numThreads + " concurrent threads to update object...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to update object";
            final HttpPatch request = patchObjMethod(pid);
            request.addHeader("Content-Type", "application/sparql-update");
            final String subjectUri = request.getURI().toString();
            final BasicHttpEntity e = new BasicHttpEntity();
            e.setContent(new ByteArrayInputStream(
                    ("INSERT { <" + subjectUri + "> <http://purl.org/dc/elements/1.1/title> "
                            + "\"Title: " + taskName + pid + "\" } WHERE {}"
                    ).getBytes()));
            request.setEntity(e);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to UPDATE object: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);


        tasks.clear();
        // Ingest new content
        logger.info("# Starting " + numThreads + " concurrent threads to inget content...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to ingest content file to object";
            final HttpRequestBase request = putDSMethod(pid, "ds", "This is a content file: " + taskName + pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(CREATED.getStatusCode());
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to INGEST content file: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);


        tasks.clear();
        // Update content
        logger.info("# Starting " + numThreads + " concurrent threads to update content...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to update content file in object";
            final HttpRequestBase request = putDSMethod(pid,
                                                        "ds",
                                                        "This is an updated content file: " + taskName + pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to UPDATE content file: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);


        tasks.clear();
        // Retrieve content
        logger.info("# Starting " + numThreads + " concurrent threads to retrieve content...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to retrieve content file in object";
            final HttpRequestBase request = getDSMethod(pid, "ds");
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(200);
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to RETRIEVE content file: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);


        tasks.clear();
        // Delete content file
        logger.info("# Starting " + numThreads + " concurrent threads to delete content file...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to delete content file in object";
            final HttpRequestBase request = deleteObjMethod(pid + "/ds");
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to DELETE content file: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);


        tasks.clear();
        // Retrieve objects
        logger.info("# Starting " + numThreads + " concurrent threads to retrieve object...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to retrieve object";
            final HttpGet request = getObjMethod(pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(200);
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to RETRIEVE object: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);


        tasks.clear();
        // Delete objects
        logger.info("# Starting " + numThreads + " concurrent threads to delete object...");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to delete object";
            final HttpRequestBase request = deleteObjMethod(pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks);
        totalResponseTime = getTotalResponseTime(numThreads, tasks);
        logger.info("** Average response time for {} concurrent threads to DELETE object: {} ms",
                    numThreads,
                    totalResponseTime / numThreads);

    }

    private static long getTotalResponseTime(final int numThreads,
                                      final List<HttpRunner> tasks) throws InterruptedException {
        Thread.sleep(1000);
        long totalResponseTime = 0;
        for (int i = 0; i < numThreads; i++) {
            totalResponseTime += tasks.get(i).responseTime;
        }
        return totalResponseTime;
    }

    private static void startThreads(final List<HttpRunner> tasks) throws InterruptedException {
        final int taskSize = tasks.size();
        for (int i = 0; i < taskSize; i++) {

            final Thread thread = new Thread(tasks.get(i));
            thread.run();
            thread.join();
        }
    }

    /**
     * Task to run http request for CRUD concurrent performance test.
     *
     * @author lsitu
     */
    class HttpRunner implements Runnable {

        private HttpClient httpClient = null;

        private HttpResponse response = null;

        private HttpRequestBase request = null;

        private String taskName = null;

        private long responseTime = 0;

        private int statusCode = 0;

        private int expectedStatusCode = 0;

        public HttpRunner(final HttpRequestBase request, final String taskName) {
            this.taskName = taskName;
            this.request = request;
            // Use its own HttpClient instance to make sure each performance test
            // won't affected by a single HttpClient instance with multiple connections.
            httpClient = createClient();
        }

        @Override
        public void run() {
            try {
                final long startTime = System.currentTimeMillis();
                response = httpClient.execute(request);
                final long endTime = System.currentTimeMillis();
                responseTime = endTime - startTime;
                statusCode = response.getStatusLine().getStatusCode();
                logger.info("{} {} with status {} in {} ms.",
                            taskName, request.getURI().toString(),
                            statusCode, String.valueOf(responseTime));
                assertEquals(taskName + " exited abnormally.", expectedStatusCode, statusCode);
            } catch (IOException e) {
                logger.error("Error {} {} got IOException: {}", taskName, request.getURI().toString(), e.getMessage());
            } finally {
                request.releaseConnection();
            }
        }

        public HttpResponse getResponse() {
            return response;
        }


        public HttpRequestBase getRequest() {
            return request;
        }


        public int getStatusCode() {
            return statusCode;
        }


        public void setExpectedStatusCode(final int expectedStatusCode) {
            this.expectedStatusCode = expectedStatusCode;
        }

    }
}
