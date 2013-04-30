package org.fcrepo.integration.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FedoraDescribeIT extends AbstractResourceIT {

	@Test
	public void testGetObjectInXML() throws Exception {
		client.execute(postObjMethod("FedoraDescribeTest1"));
		final HttpGet getObjMethod =
				new HttpGet(serverAddress + "objects/FedoraDescribeTest1/fcr:describe");
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
				new HttpGet(serverAddress + "objects/FedoraDescribeTest2/ds1/fcr:describe");
		final HttpResponse response = client.execute(getObjMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Retrieved datastream profile:\n" + content);
	}


}
