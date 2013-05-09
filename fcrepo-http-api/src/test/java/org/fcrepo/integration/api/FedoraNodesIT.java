
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;

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

		assertTrue("Didn't find an expected ntriple", compile("<info:fedora/objects/FedoraDescribeTestGraph> <info:fedora/fedora-system:def/internal#mixinTypes> \"fedora:object\" \\.",
																	DOTALL).matcher(content).find());

		logger.debug("Retrieved object graph:\n" + content);

	}


	@Test
	public void testUpdateObjectGraph() throws Exception {
		client.execute(postObjMethod("FedoraDescribeTestGraphUpdate"));
		final HttpPost updateObjectGraphMethod =
				new HttpPost(serverAddress + "objects/FedoraDescribeTestGraphUpdate");
		updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
		BasicHttpEntity e = new BasicHttpEntity();
		e.setContent(new ByteArrayInputStream("INSERT { <info:fedora/objects/FedoraDescribeTestGraphUpdate> <http://purl.org/dc/terms/identifier> \"this is an identifier\" } WHERE {}".getBytes()));
		updateObjectGraphMethod.setEntity(e);
		final HttpResponse response = client.execute(updateObjectGraphMethod);
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

	}

	@Test
	public void testUpdateObjectGraphWithProblems() throws Exception {
		client.execute(postObjMethod("FedoraDescribeTestGraphUpdateBad"));
		final HttpPost getObjMethod =
				new HttpPost(serverAddress + "objects/FedoraDescribeTestGraphUpdateBad");
		getObjMethod.addHeader("Content-Type", "application/sparql-update");
		BasicHttpEntity e = new BasicHttpEntity();
		e.setContent(new ByteArrayInputStream("INSERT { <info:fedora/objects/FedoraDescribeTestGraphUpdateBad> <info:fedora/fedora-system:def/internal#uuid> \"00e686e2-24d4-40c2-92ce-577c0165b158\" } WHERE {}\n".getBytes()));
		getObjMethod.setEntity(e);
		final HttpResponse response = client.execute(getObjMethod);
		assertEquals(403, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());


		logger.debug("Got update response:\n" + content);

	}



}
