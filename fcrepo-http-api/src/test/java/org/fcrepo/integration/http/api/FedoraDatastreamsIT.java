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
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static junit.framework.TestCase.assertFalse;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraDatastreamsIT extends AbstractResourceIT {

    @Test
    public void testMultipleDatastreams() throws Exception {
        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest7");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDS1Method =
                postDSMethod("FedoraDatastreamsTest7", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDS1Method));
        final HttpPost createDS2Method =
                postDSMethod("FedoraDatastreamsTest7", "ds2",
                        "marbles for no one");
        assertEquals(201, getStatus(createDS2Method));

        final HttpGet getDSesMethod =
                new HttpGet(serverAddress + "FedoraDatastreamsTest7");
        getDSesMethod.addHeader("Accept", "text/n3");
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final GraphStore result =
            parseTriples(response.getEntity().getContent());
        logger.debug("Received triples: \n{}", result.toString());
        final String subjectURI = serverAddress + "FedoraDatastreamsTest7";

        assertTrue("Didn't find the first datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds2")));
    }

    @Test
    public void testModifyMultipleDatastreams() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest8");

        assertEquals(201, getStatus(objMethod));

        final HttpPost createDSVOIDMethod =
            postDSMethod("FedoraDatastreamsTest8", "ds_void",
                    "marbles for everyone");
        assertEquals(201, getStatus(createDSVOIDMethod));

        final HttpPost post =
            new HttpPost(serverAddress
                    + "FedoraDatastreamsTest8/fcr:datastreams?delete=ds_void");

        final MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        final HttpResponse postResponse = client.execute(post);

        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        final HttpGet getDSesMethod =
            new HttpGet(serverAddress + "FedoraDatastreamsTest8");
        getDSesMethod.addHeader("Accept", "text/n3");
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String subjectURI = serverAddress + "FedoraDatastreamsTest8";
        final GraphStore result =
            parseTriples(response.getEntity().getContent());
        assertTrue("Didn't find the first datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds2")));
        assertFalse("Found the deleted datastream! ", result.contains(Node.ANY,
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds_void")));

    }

    @Test
    public void testRetrieveMultipartDatastreams() throws Exception {

        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest9");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
            new HttpPost(serverAddress
                    + "FedoraDatastreamsTest9/fcr:datastreams/");

        final MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the
        // things we're expecting
        final HttpGet getDSesMethod =
            new HttpGet(serverAddress
                    + "FedoraDatastreamsTest9/fcr:datastreams");
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

        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest10");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
            new HttpPost(serverAddress
                    + "FedoraDatastreamsTest10/fcr:datastreams");

        final MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the
        // things we're expecting
        final HttpGet getDSesMethod =
            new HttpGet(serverAddress
                    + "FedoraDatastreamsTest10/fcr:datastreams?dsid=ds1");
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
        execute(postObjMethod("FedoraDatastreamsTest12"));
        final HttpPost method1 =
            postDSMethod("FedoraDatastreamsTest12", "ds1", "foo1");
        assertEquals(201, getStatus(method1));
        final HttpPost method2 =
            postDSMethod("FedoraDatastreamsTest12", "ds2", "foo2");
        assertEquals(201, getStatus(method2));

        final HttpDelete dmethod =
            new HttpDelete(
                    serverAddress
                            + "FedoraDatastreamsTest12/fcr:datastreams?dsid=ds1&dsid=ds2");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get1 =
            new HttpGet(serverAddress + "FedoraDatastreamsTest12/ds1");
        assertEquals(404, getStatus(method_test_get1));
        final HttpGet method_test_get2 =
            new HttpGet(serverAddress + "FedoraDatastreamsTest12/ds2");
        assertEquals(404, getStatus(method_test_get2));
    }
}
