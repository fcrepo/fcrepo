
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.Test;

public class FedoraDatastreamsIT extends AbstractResourceIT {

    private static final String faulkner1 =
            "The past is never dead. It's not even past.";

    @Test
    public void testGetDatastreams() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest1"));

        final HttpPost dsmethod =
                postDSMethod("FedoraDatastreamsTest1", "zxc", "foo");
        assertEquals(201, getStatus(dsmethod));

        final HttpGet method =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest1/fcr:datastreams");
        assertEquals(200, getStatus(method));

        final HttpResponse response = execute(method);

        final String content =
                IOUtils.toString(response.getEntity().getContent());

        logger.info(content);

        assertTrue("Found the wrong XML tag", compile("<datastream ", DOTALL)
                .matcher(content).find());
    }

    @Test
    public void testAddDatastream() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest2");
        assertEquals(201, getStatus(objMethod));
        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest2", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + OBJECT_PATH.replace("/", "") +
                        "/FedoraDatastreamsTest2/zxc", location);
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
        final HttpResponse response = client.execute(mutateDataStreamMethod);
        int status = response.getStatusLine().getStatusCode();
        if (status != 201) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals("Couldn't mutate a datastream!", 201, status);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + OBJECT_PATH.replace("/", "") +
                        "/FedoraDatastreamsTest3/ds1", location);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest3/ds1/fcr:content");
        assertTrue("Datastream didn't accept mutation!", faulkner1
                .equals(EntityUtils.toString(client.execute(
                        retrieveMutatedDataStreamMethod).getEntity())));
    }

    @Test
    public void testGetDatastream() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest4"));

        assertEquals(404, getStatus(new HttpGet(serverAddress +
                "objects/FedoraDatastreamsTest4/ds1")));
        assertEquals(201, getStatus(postDSMethod("FedoraDatastreamsTest4",
                "ds1", "foo")));
        final HttpResponse response =
                execute(new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest4/ds1"));
        assertEquals(EntityUtils.toString(response.getEntity()), 200, response
                .getStatusLine().getStatusCode());
    }

    @Test
    public void testDeleteDatastream() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest5"));

        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest5", "ds1", "foo");
        assertEquals(201, getStatus(method));

        final HttpGet method_2 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest5/ds1");
        assertEquals(200, getStatus(method_2));

        final HttpDelete dmethod =
                new HttpDelete(serverAddress +
                        "objects/FedoraDatastreamsTest5/ds1");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest5/ds1");
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
                        "objects/FedoraDatastreamsTest6/ds1/fcr:content");
        assertEquals(200, getStatus(method_test_get));
        final HttpResponse response = client.execute(method_test_get);
        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response.getEntity())));

        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df",
                response.getFirstHeader("ETag").getValue().replace("\"", ""));

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

        final SimpleDateFormat format =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest61/ds1/fcr:content");
        method_test_get.setHeader("If-None-Match",
                "\"urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df\"");
        method_test_get.setHeader("If-Modified-Since", format
                .format(new Date()));

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
                        "objects/FedoraDatastreamsTest7?mixin=" + FedoraJcrTypes.FEDORA_DATASTREAM);
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        Collection<String> result = TestHelpers.parseChildren(response.getEntity());
        assertTrue("Didn't find the first datastream! ", result.contains("ds1"));
        assertTrue("Didn't find the second datastream! ", result.contains("ds2"));
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
                new HttpPost(serverAddress +
                        "objects/FedoraDatastreamsTest8/fcr:datastreams?delete=ds_void");

        final MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        final HttpResponse postResponse = client.execute(post);

        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        final HttpGet getDSesMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest8/fcr:datastreams");
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find the first datastream!", compile("dsid=\"ds1\"",
                DOTALL).matcher(content).find());
        assertTrue("Didn't find the second datastream!", compile(
                "dsid=\"ds2\"", DOTALL).matcher(content).find());

        assertFalse("Found the deleted datastream!", compile(
                "dsid=\"ds_void\"", DOTALL).matcher(content).find());

    }

    @Test
    public void testRetrieveMultipartDatastreams() throws Exception {

        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest9");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
                new HttpPost(serverAddress +
                        "objects/FedoraDatastreamsTest9/fcr:datastreams/");

        final MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the things we're expecting
        final HttpGet getDSesMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest9/fcr:datastreams/__content__");
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
                new HttpPost(serverAddress +
                        "objects/FedoraDatastreamsTest10/fcr:datastreams");

        final MultipartEntity multiPartEntity = new MultipartEntity();
        multiPartEntity.addPart("ds1", new StringBody("asdfg"));
        multiPartEntity.addPart("ds2", new StringBody("qwerty"));

        post.setEntity(multiPartEntity);

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the things we're expecting
        final HttpGet getDSesMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest10/fcr:datastreams/__content__?dsid=ds1");
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
                new HttpDelete(serverAddress +
                        "objects/FedoraDatastreamsTest12/fcr:datastreams?dsid=ds1&dsid=ds2");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get1 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest12/ds1");
        assertEquals(404, getStatus(method_test_get1));
        final HttpGet method_test_get2 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest12/ds2");
        assertEquals(404, getStatus(method_test_get2));
    }
}
