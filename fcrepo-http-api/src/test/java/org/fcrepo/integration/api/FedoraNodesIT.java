
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.junit.Test;

public class FedoraNodesIT extends AbstractResourceIT {

    @Test
    public void testIngest() throws Exception {
        final HttpPost method = postObjMethod("FedoraObjectsTest1");
        final HttpResponse response = client.execute(method);
        assertEquals(201, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals("Got wrong Location header for ingest!", serverAddress +
                OBJECT_PATH + "/FedoraObjectsTest1", location);
    }

    @Test
    public void testIngestWithNew() throws Exception {
        final HttpPost method = postObjMethod("fcr:new");
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        int status = response.getStatusLine().getStatusCode();
        if (201 != status) {
            logger.error(content);
        }
        assertEquals(201, status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("new object did not mint a PID", !content.endsWith("/fcr:new"));

		assertEquals("Object wasn't created!", 200,
							getStatus(new HttpGet(serverAddress + content)));
    }

    @Test
    public void testDeleteObject() throws Exception {
        assertEquals(201, getStatus(postObjMethod("FedoraObjectsTest3")));
        assertEquals(204, getStatus(new HttpDelete(serverAddress +
                "objects/FedoraObjectsTest3")));
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(serverAddress +
                        "objects/FedoraObjectsTest3")));
    }

    @Test
    public void testIngestWithLabel() throws Exception {
        final HttpPost method =
                postObjMethod("FedoraObjectsTest4", "label=Awesome_Object");
        final HttpResponse response = client.execute(method);
        assertEquals(201, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());

        final ObjectProfile obj = getObject("FedoraObjectsTest4");
        assertEquals("Wrong label!", "Awesome_Object", obj.objLabel);
    }

	@Test
	public void testGetObjectInXML() throws Exception {
		client.execute(postObjMethod("FedoraDescribeTest1"));
		final HttpGet getObjMethod =
				new HttpGet(serverAddress + "objects/FedoraDescribeTest1");
		getObjMethod.addHeader("Accept", "text/xml");
		final HttpResponse response = client.execute(getObjMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Retrieved object profile:\n" + content);
		assertTrue("Object had wrong PID!", compile(
														   "pid=\"FedoraDescribeTest1\"").matcher(content).find());
	}


	@Test
	public void testGetDatastreamInXML() throws Exception {
		client.execute(postObjMethod("FedoraDescribeTest2"));
		client.execute(postDSMethod("FedoraDescribeTest2", "ds1", "qwertypoiuytrewqadfghjklmnbvcxz"));
		final HttpGet getObjMethod =
				new HttpGet(serverAddress + "objects/FedoraDescribeTest2/ds1");
		getObjMethod.addHeader("Accept", "text/xml");
		final HttpResponse response = client.execute(getObjMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Retrieved datastream profile:\n" + content);
	}

	@Test
	public void testGetObjectGraph() throws Exception {
		client.execute(postObjMethod("FedoraDescribeTestGraph"));
		final HttpGet getObjMethod =
				new HttpGet(serverAddress + "objects/FedoraDescribeTestGraph");
		getObjMethod.addHeader("Accept", "application/n-triples");
		final HttpResponse response = client.execute(getObjMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());

		assertTrue("Didn't find an expected ntriple", compile("<info:fedora/objects/FedoraDescribeTestGraph> <http://www.jcp.org/jcr/1.0mixinTypes> \"fedora:object\" \\.",
																	DOTALL).matcher(content).find());

		logger.debug("Retrieved object graph:\n" + content);

	}




}
