package org.fcrepo.integration.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.utils.FixityResult;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static junit.framework.TestCase.assertFalse;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.utils.FixityResult.FixityState.BAD_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FedoraFixityIT extends AbstractResourceIT {
	@Test
	public void testCheckDatastreamFixity() throws Exception {
		final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest11");
		assertEquals(201, getStatus(objMethod));
		final HttpPost method1 =
				postDSMethod("FedoraDatastreamsTest11", "zxc", "foo");
		assertEquals(201, getStatus(method1));
		final HttpGet method2 =
				new HttpGet(serverAddress +
									"objects/FedoraDatastreamsTest11/zxc/fcr:fixity");
		final HttpResponse response = execute(method2);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final HttpEntity entity = response.getEntity();
		final String content = EntityUtils.toString(entity);
		final JAXBContext context =
				JAXBContext.newInstance(DatastreamFixity.class);
		final Unmarshaller um = context.createUnmarshaller();
		final DatastreamFixity fixity =
				(DatastreamFixity) um.unmarshal(new java.io.StringReader(
																				content));
		int cache = 0;
		for (final FixityResult status : fixity.statuses) {
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
}
