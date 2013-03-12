
package org.fcrepo.api.legacy;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraDatastreamsTest extends AbstractResourceTest {

    private static final String faulkner1 =
            "The past is never dead. It's not even past.";

    @Test
    public void testGetDatastreams() throws Exception {
        client.execute(postObjMethod("FedoraDatastreamsTest1"));
        final HttpGet method =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest1/datastreams");
        assertEquals(200, getStatus(method));
    }

    @Test
    public void testAddDatastream() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest2");
        assertEquals(201, getStatus(objMethod));
        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest2", "zxc", "foo");
        assertEquals(201, getStatus(method));
    }

    @Test
    public void testMutateDatastream() throws Exception {
        final HttpPost createObjectMethod =
                postObjMethod("FedoraDatastreamsTest3");
        assertEquals("Couldn't create an object!", 201,
                getStatus(createObjectMethod));

        final HttpPost createDataStreamMethod =
                postDSMethod("FedoraDatastreamsTest3", "ds1", "foo");
        assertEquals("Couldn't create a datastream!", 201,
                getStatus(createDataStreamMethod));

        final HttpPut mutateDataStreamMethod =
                putDSMethod("FedoraDatastreamsTest3", "ds1");
        mutateDataStreamMethod.setEntity(new StringEntity(faulkner1, "UTF-8"));
        assertEquals("Couldn't mutate a datastream!", 201,
                getStatus(mutateDataStreamMethod));

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest3/datastreams/ds1/content");
        assertTrue("Datastream didn't accept mutation!", faulkner1
                .equals(EntityUtils.toString(client.execute(
                        retrieveMutatedDataStreamMethod).getEntity())));
    }

    @Test
    public void testGetDatastream() throws Exception {
        client.execute(postObjMethod("FedoraDatastreamsTest4"));

        assertEquals(404, getStatus(new HttpGet(serverAddress +
                "objects/FedoraDatastreamsTest4/datastreams/ds1")));
        assertEquals(201, getStatus(postDSMethod("FedoraDatastreamsTest4",
                "ds1", "foo")));
        assertEquals(200, getStatus(new HttpGet(serverAddress +
                "objects/FedoraDatastreamsTest4/datastreams/ds1")));
    }

    @Test
    public void testDeleteDatastream() throws Exception {
        client.execute(postObjMethod("FedoraDatastreamsTest5"));

        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest5", "ds1", "foo");
        assertEquals(201, getStatus(method));

        final HttpGet method_2 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest5/datastreams/ds1");
        assertEquals(200, getStatus(method_2));

        final HttpDelete dmethod =
                new HttpDelete(serverAddress +
                        "objects/FedoraDatastreamsTest5/datastreams/ds1");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest5/datastreams/ds1");
        assertEquals(404, getStatus(method_test_get));
    }

    @Test
    public void testGetDatastreamContent() throws Exception {
        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest6");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest6", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));
        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest6/datastreams/ds1/content");
        assertEquals(200, getStatus(method_test_get));
        final HttpResponse response = client.execute(method_test_get);
        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response
                        .getEntity())));

        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df", response.getFirstHeader("ETag").getValue());



        logger.debug("Content was correct.");
    }

    @Test
    public void testRefetchingDatastreamContent() throws Exception {

        final HttpPost createObjMethod =
                postObjMethod("FedoraDatastreamsTest61");
        assertEquals(201, getStatus(createObjMethod));

        final HttpPost createDSMethod =
                postDSMethod("FedoraDatastreamsTest61", "ds1",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));


        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");


        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest61/datastreams/ds1/content");
        method_test_get.setHeader("If-None-Match","\"urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df\"");
        method_test_get.setHeader("If-Modified-Since", format.format(new Date()));

        assertEquals(304, getStatus(method_test_get));

    }

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
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest7/datastreams");
        HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find the first datastream!", compile("dsid=\"ds1\"",
                DOTALL).matcher(content).find());
        assertTrue("Didn't find the second datastream!", compile(
                "dsid=\"ds2\"", DOTALL).matcher(content).find());
    }


    @Test
    public void testAddMultipleDatastreams() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest8");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
                new HttpPost(serverAddress + "objects/FedoraDatastreamsTest8/datastreams/");

        MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        HttpResponse postResponse = client.execute(post);

        assertEquals(201, postResponse.getStatusLine().getStatusCode());


        final HttpGet getDSesMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest8/datastreams");
        HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find the first datastream!", compile("dsid=\"ds1\"",
                DOTALL).matcher(content).find());
        assertTrue("Didn't find the second datastream!", compile(
                "dsid=\"ds2\"", DOTALL).matcher(content).find());



    }

    @Test
    public void testRetrieveMultipartDatastreams() throws Exception {

        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest9");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
                new HttpPost(serverAddress + "objects/FedoraDatastreamsTest9/datastreams/");

        MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());


        // TODO: we should actually evaluate the multipart response for the things we're expecting
        final HttpGet getDSesMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest9/datastreams/__content__");
        HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find the first datastream!", compile("asdfg",
                DOTALL).matcher(content).find());
        assertTrue("Didn't find the second datastream!", compile(
                "qwerty", DOTALL).matcher(content).find());

    }

    @Test
    public void testRetrieveFIlteredMultipartDatastreams() throws Exception {

        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest10");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
                new HttpPost(serverAddress + "objects/FedoraDatastreamsTest10/datastreams/");

        MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());


        // TODO: we should actually evaluate the multipart response for the things we're expecting
        final HttpGet getDSesMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest10/datastreams/__content__?dsid=ds1");
        HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find the first datastream!", compile("asdfg",
                DOTALL).matcher(content).find());
        assertFalse("Didn't expect to find the second datastream!", compile(
                "qwerty", DOTALL).matcher(content).find());

    }
    
    @Test
    public void testCheckDatastreamFixity() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest10");
        assertEquals(201, getStatus(objMethod));
        final HttpPost method1 =
                postDSMethod("FedoraDatastreamsTest10", "zxc", "foo");
        assertEquals(201, getStatus(method1));
        final HttpGet method2 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest10/datastreams/zxc/fixity");
        assertEquals(200, getStatus(method2));
    }
}
