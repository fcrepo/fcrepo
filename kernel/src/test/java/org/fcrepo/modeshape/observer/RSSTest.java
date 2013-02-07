package org.fcrepo.modeshape.observer;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.fcrepo.modeshape.AbstractResourceTest;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "/spring-test/eventing.xml", "/spring-test/repo.xml" })
@DirtiesContext
public class RSSTest extends AbstractResourceTest {

	@Test
	public void testRSS() throws Exception {
		PostMethod createObjMethod = new PostMethod(serverAddress
				+ "objects/RSSTESTPID");
		assertEquals(201, client.executeMethod(createObjMethod));

		GetMethod getRSSMethod = new GetMethod(serverAddress + "/rss");
		assertEquals(200, client.executeMethod(getRSSMethod));
		String response = getRSSMethod.getResponseBodyAsString();
		logger.debug("Retrieved RSS feed:\n" + response);
		assertTrue("Didn't find the test PID in RSS!",
				compile("RSSTESTPID", DOTALL).matcher(response).find());

	}
}
