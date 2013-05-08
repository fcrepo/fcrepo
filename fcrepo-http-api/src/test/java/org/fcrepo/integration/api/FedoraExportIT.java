package org.fcrepo.integration.api;


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraExportIT extends AbstractResourceIT {

	@Test
	public void shouldRoundTripOneObject() throws IOException {
		final String objName = "JcrXmlSerializerIT1";

		// set up the object
		client.execute(postObjMethod(objName));
		client.execute(postDSMethod(objName, "testDS", "stuff"));

		// export it
		logger.debug("Attempting to export: " + objName);
		final HttpGet getObjMethod =
				new HttpGet(serverAddress + "objects/JcrXmlSerializerIT1" + "/fcr:export");
		HttpResponse response = client.execute(getObjMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		logger.debug("Successfully exported: " + objName);
		final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Found exported object: " + content);

		// delete it
		client.execute(new HttpDelete(serverAddress + "objects/JcrXmlSerializerIT1"));
		response = client.execute(new HttpGet(serverAddress + "objects/JcrXmlSerializerIT1"));
		assertEquals(404, response.getStatusLine().getStatusCode());

		// try to import it
		final HttpPost importMethod = new HttpPost(serverAddress + "objects/fcr:import");
		importMethod.setEntity(new StringEntity(content));
		assertEquals("Couldn't import!", 201, getStatus(importMethod));

		//check that we made it
		response = client.execute(new HttpGet(serverAddress + "objects/JcrXmlSerializerIT1"));
		assertEquals(200, response.getStatusLine().getStatusCode());
	}

}

