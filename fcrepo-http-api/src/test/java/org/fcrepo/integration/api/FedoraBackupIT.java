package org.fcrepo.integration.api;


import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FedoraBackupIT extends AbstractResourceIT {

	@Test
	public void shouldRoundTripBackups() throws IOException {
		final String objName = "FedoraBackupITObject";

		// set up the object
		client.execute(postObjMethod(objName));
		client.execute(postDSMethod(objName, "testDS", "stuff"));

		// back it up
		logger.debug("Attempting to back up repository");
		final HttpPost backupMethod =
				new HttpPost(serverAddress + "fcr:backup");
		HttpResponse response = client.execute(backupMethod);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Back up directory was {}", content);

		// delete it
		client.execute(new HttpDelete(serverAddress + "objects/FedoraBackupITObject"));
		response = client.execute(new HttpGet(serverAddress + "objects/FedoraBackupITObject"));
		assertEquals(404, response.getStatusLine().getStatusCode());

		// try to restore it
        final HttpPost restoreMethod =
                new HttpPost(serverAddress + "fcr:restore");
		assertEquals("Couldn't import!", 204, getStatus(restoreMethod));

		//check that we made it
		response = client.execute(new HttpGet(serverAddress + "objects/FedoraBackupITObject"));
		assertEquals(200, response.getStatusLine().getStatusCode());

    }

}

