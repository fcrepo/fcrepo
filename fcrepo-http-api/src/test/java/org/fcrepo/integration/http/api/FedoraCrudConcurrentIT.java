/**
 * Copyright 2014 DuraSpace, Inc.
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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.Test;

public class FedoraCrudConcurrentIT extends AbstractResourceIT {

    @Test
    public void testConcurrentIngest() throws Exception {
        int[] numThreadsToTest = {2, 4, 8, 16, 32};
        logger.info("# Start CRUD concurrent performance testing...");
        for (int i=0; i < numThreadsToTest.length; i++) {
            startCrudConcurrentPerformanceTest(numThreadsToTest[i]);
        }
    }

    /**
     * Test CRUD concurrent access performance:
     * create/update/delete object, create/update/delete content file
     * @param numThreads
     * @throws Exception
     */
    protected void startCrudConcurrentPerformanceTest(int numThreads) throws Exception {
        String pid = null;

        // Tasks to run
        final List<HttpRunner> tasks = new ArrayList<>();
        final List<String> pids = new ArrayList<>();

        // Create object
        logger.info("# Start testing with " + numThreads + " concurrent threads to create object:");
        for (int i = 0; i < numThreads; i++) {
            pid = getRandomUniquePid();
            pids.add(pid);
            final String taskName = "Thread " + (i + 1) + " to create object " + pid;
            final HttpRequestBase request = postObjMethod("/");
            request.addHeader("Slug", pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(CREATED.getStatusCode());
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);

        tasks.clear();
        // Update objects
        logger.info("# Start testing with " + numThreads + " concurrent threads to update object:");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to update object";
            final HttpPatch request = patchObjMethod (pid);
            request.addHeader("Content-Type", "application/sparql-update");
            final String subjectUri = request.getURI().toString();
            final BasicHttpEntity e = new BasicHttpEntity();
            e.setContent(new ByteArrayInputStream(
                        ("INSERT { <" + subjectUri + "> <http://purl.org/dc/elements/1.1/title> "
                                + "\"Ttile: " + taskName + pid + "\" } WHERE {}"
                        ).getBytes()));
            request.setEntity(e);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);

        tasks.clear();
        // Ingest new content
        logger.info("# Start testing with " + numThreads + " concurrent threads to inget content:");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to ingest content file to object";
            final HttpRequestBase request = postDSMethod(pid, "ds", "This is a content file: " + taskName + pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(CREATED.getStatusCode());
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);

        tasks.clear();
        // Update content
        logger.info("# Start testing with " + numThreads + " concurrent threads to update content:");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to update content file in object";
            final HttpRequestBase request = putDSMethod(pid, "ds", "This is an updated content file: " + taskName + pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);

        tasks.clear();
        // Delete content file
        logger.info("# Start testing with " + numThreads + " concurrent threads to delete content file:");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to delete content file in object";
            final HttpRequestBase request = deleteObjMethod(pid + "/ds");
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);


        tasks.clear();
        // Retrieve objects
        logger.info("# Start testing with " + numThreads + " concurrent threads to retrieve object:");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to retrieve object";
            final HttpGet request = getObjMethod (pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(200);
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);

        tasks.clear();
        // Delete objects
        logger.info("# Start testing with " + numThreads + " concurrent threads to delete object:");
        for (int i = 0; i < numThreads; i++) {
            pid = pids.get(i);
            final String taskName = "Thread " + (i + 1) + " to delete object";
            final HttpRequestBase request = deleteObjMethod(pid);
            final HttpRunner task = new HttpRunner(request, taskName);
            task.setExpectedStatusCode(204);
            tasks.add(task);
        }
        startThreads(tasks) ;
        Thread.sleep(1000);
    }

    private void startThreads(List<HttpRunner> tasks) throws InterruptedException{
        int taskSize = tasks.size();
        for (int i = 0; i < taskSize; i++) {

            final Thread thread = new Thread(tasks.get(i));
            thread.run();
            thread.join();
        }
    }
}
