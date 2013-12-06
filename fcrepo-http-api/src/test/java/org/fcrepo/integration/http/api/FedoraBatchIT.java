/**
 * Copyright 2013 DuraSpace, Inc.
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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static junit.framework.TestCase.assertFalse;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraBatchIT extends AbstractResourceIT {

    @Test
    public void testMultipleDatastreams() throws Exception {
        final String pid = randomUUID().toString();
        createObject(pid);

        createDatastream(pid, "ds1", "marbles for everyone");
        createDatastream(pid, "ds2", "marbles for no one");

        final HttpGet getDSesMethod =
                new HttpGet(serverAddress + pid);
        final GraphStore result = getGraphStore(getDSesMethod);

        logger.debug("Received triples: \n{}", result.toString());
        final String subjectURI = serverAddress + pid;

        assertTrue("Didn't find the first datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds2")));
    }

    @Test
    public void testModifyMultipleDatastreams() throws Exception {
        final String pid = randomUUID().toString();

        createObject(pid);
        createDatastream(pid, "ds_void", "marbles for everyone");

        final HttpPost post =
            new HttpPost(serverAddress
                    + pid + "/fcr:batch");
        final MultipartEntityBuilder multiPartEntityBuilder = MultipartEntityBuilder.create();
        multiPartEntityBuilder.addTextBody(".", "INSERT { <> <http://purl.org/dc/elements/1.1/title> 'xyz' } WHERE { }", ContentType.parse(contentTypeSPARQLUpdate));
        multiPartEntityBuilder.addTextBody("obj1","<>  <http://purl.org/dc/elements/1.1/title> 'obj1-title' ", ContentType.parse(contentTypeTurtle));
        multiPartEntityBuilder.addTextBody("ds1","asdfg", TEXT_PLAIN);
        multiPartEntityBuilder.addTextBody("ds2", "qwerty", TEXT_PLAIN);
        multiPartEntityBuilder.addTextBody("delete[]", "ds_void");
        post.setEntity(multiPartEntityBuilder.build());

        final HttpResponse postResponse = client.execute(post);

        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        final HttpGet getDSesMethod =
            new HttpGet(serverAddress + pid);
        final GraphStore result = getGraphStore(getDSesMethod);

        final String subjectURI = serverAddress + pid;

        assertTrue("Didn't find the title! ", result.contains(ANY, createURI(subjectURI), createURI("http://purl.org/dc/elements/1.1/title"), createLiteral("xyz")));
        assertTrue("Didn't find the object title! ", result.contains(ANY, createURI(subjectURI + "/obj1"), createURI("http://purl.org/dc/elements/1.1/title"), createLiteral("obj1-title")));
        assertTrue("Didn't find the first datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds2")));
        assertFalse("Found the deleted datastream! ", result.contains(Node.ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds_void")));

    }

    @Test
    public void testRetrieveMultipartDatastreams() throws Exception {
        final String pid = randomUUID().toString();
        createObject(pid);

        final HttpPost post =
            new HttpPost(serverAddress
                    + pid + "/fcr:batch/");
        final MultipartEntityBuilder multiPartEntityBuilder =
            MultipartEntityBuilder.create().addTextBody("ds1", "asdfg", TEXT_PLAIN).addTextBody("ds2",
                                                                                                   "qwerty", TEXT_PLAIN);

        post.setEntity(multiPartEntityBuilder.build());

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the
        // things we're expecting
        final HttpGet getDSesMethod =
            new HttpGet(serverAddress
                    + pid + "/fcr:batch");
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find the first datastream!",
                compile("asdfg", DOTALL).matcher(content).find());
        assertTrue("Didn't find the second datastream!", compile("qwerty",
                DOTALL).matcher(content).find());

    }

    @Test
    public void testRetrieveFIlteredMultipartDatastreams() throws Exception {
        final String pid = randomUUID().toString();
        createObject(pid);

        final HttpPost post =
            new HttpPost(serverAddress
                    + pid + "/fcr:batch");

        final MultipartEntityBuilder multiPartEntityBuilder =
            MultipartEntityBuilder.create().addTextBody("ds1", "asdfg", TEXT_PLAIN).addTextBody("ds2",
                                                                                                   "qwerty", TEXT_PLAIN);

        post.setEntity(multiPartEntityBuilder.build());

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the
        // things we're expecting
        final HttpGet getDSesMethod =
            new HttpGet(serverAddress
                    + pid + "/fcr:batch?child=ds1");
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find the first datastream!",
                compile("asdfg", DOTALL).matcher(content).find());
        assertFalse("Didn't expect to find the second datastream!", compile(
                "qwerty", DOTALL).matcher(content).find());

    }

    @Test
    public void testBatchDeleteDatastream() throws Exception {
        final String pid = randomUUID().toString();
        createObject(pid);

        createDatastream(pid, "ds1", "foo1");
        createDatastream(pid, "ds2", "foo2");

        final HttpDelete dmethod =
            new HttpDelete(
                    serverAddress
                            + pid + "/fcr:batch?child=ds1&child=ds2");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get1 =
            new HttpGet(serverAddress + pid + "/ds1");
        assertEquals(404, getStatus(method_test_get1));
        final HttpGet method_test_get2 =
            new HttpGet(serverAddress + pid + "/ds2");
        assertEquals(404, getStatus(method_test_get2));
    }
}
