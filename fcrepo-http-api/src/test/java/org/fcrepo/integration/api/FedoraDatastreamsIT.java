
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static junit.framework.TestCase.assertFalse;
import static org.fcrepo.services.PathService.OBJECT_PATH;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.utils.FixityResult;
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
                        "objects/FedoraDatastreamsTest1/datastreams");
        assertEquals(200, getStatus(method));

        final HttpResponse response = execute(method);

        String content = IOUtils.toString(response.getEntity().getContent());

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
                        "/FedoraDatastreamsTest2/datastreams/zxc", location);
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
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals("Couldn't mutate a datastream!", 201, response
                .getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + OBJECT_PATH.replace("/", "") +
                        "/FedoraDatastreamsTest3/datastreams/ds1", location);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest3/datastreams/ds1/content");
        assertTrue("Datastream didn't accept mutation!", faulkner1
                .equals(EntityUtils.toString(client.execute(
                        retrieveMutatedDataStreamMethod).getEntity())));
    }

    @Test
    public void testGetDatastream() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest4"));

        assertEquals(404, getStatus(new HttpGet(serverAddress +
                "objects/FedoraDatastreamsTest4/datastreams/ds1")));
        assertEquals(201, getStatus(postDSMethod("FedoraDatastreamsTest4",
                "ds1", "foo")));
        HttpResponse response =
                execute(new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest4/datastreams/ds1"));
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

        SimpleDateFormat format =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest61/datastreams/ds1/content");
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
    public void testModifyMultipleDatastreams() throws Exception {
        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest8");

        assertEquals(201, getStatus(objMethod));

        final HttpPost createDSVOIDMethod =
                postDSMethod("FedoraDatastreamsTest8", "ds_void",
                        "marbles for everyone");
        assertEquals(201, getStatus(createDSVOIDMethod));

        final HttpPost post =
                new HttpPost(serverAddress +
                        "objects/FedoraDatastreamsTest8/datastreams?delete=ds_void");

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

        assertFalse("Found the deleted datastream!", compile(
                "dsid=\"ds_void\"", DOTALL).matcher(content).find());

    }

    @Test
    public void testRetrieveMultipartDatastreams() throws Exception {

        final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest9");
        assertEquals(201, getStatus(objMethod));
        final HttpPost post =
                new HttpPost(serverAddress +
                        "objects/FedoraDatastreamsTest9/datastreams/");

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
                        "objects/FedoraDatastreamsTest10/datastreams/");

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

        assertTrue("Didn't find the first datastream!",
                compile("asdfg", DOTALL).matcher(content).find());
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
        HttpResponse response = execute(method2);
        assertEquals(200, response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        JAXBContext context = JAXBContext.newInstance(DatastreamFixity.class);
        Unmarshaller um = context.createUnmarshaller();
        DatastreamFixity fixity =
                (DatastreamFixity) um.unmarshal(new java.io.StringReader(
                        content));
        int cache = 0;
        for (FixityResult status : fixity.statuses) {
            logger.debug("Verifying cache {} :", cache++);
            assertFalse(status.status.contains(BAD_CHECKSUM));
            logger.debug("Checksum matched");
            assertFalse(status.status.contains(BAD_SIZE));
            logger.debug("DS size matched");
            assertTrue("Didn't find the store identifier!", compile(
                    "infinispan", DOTALL).matcher(status.storeIdentifier)
                    .find());
            logger.debug("cache store matched");
        }
    }

    @Test
    public void testBatchDeleteDatastream() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest11"));
        final HttpPost method1 =
                postDSMethod("FedoraDatastreamsTest11", "ds1", "foo1");
        assertEquals(201, getStatus(method1));
        final HttpPost method2 =
                postDSMethod("FedoraDatastreamsTest11", "ds2", "foo2");
        assertEquals(201, getStatus(method2));

        final HttpDelete dmethod =
                new HttpDelete(serverAddress +
                        "objects/FedoraDatastreamsTest11/datastreams?dsid=ds1&dsid=ds2");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get1 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest11/datastreams/ds1");
        assertEquals(404, getStatus(method_test_get1));
        final HttpGet method_test_get2 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest11/datastreams/ds2");
        assertEquals(404, getStatus(method_test_get2));
    }
}
