package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraChildrenIT  extends AbstractResourceIT {

	@Test
	public void testGetObjects() throws Exception {
		client.execute(postObjMethod("FedoraChildrenTest1"));
		final HttpGet getObjMethod =
				new HttpGet(serverAddress + "objects/FedoraChildrenTest1/fcr:children");
		final HttpResponse response = client.execute(getObjMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Retrieved object children:\n" + content);
		assertEquals("[]", content);
	}
}
