package org.fcrepo.modeshape;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "/spring-test/rest.xml", "/spring-test/repo.xml" })
public class FedoraObjectsTest extends AbstractResourceTest {

	@Test
	public void testIngest() throws Exception {
		PostMethod method = postObjMethod("asdf");
		assertEquals(201, client.executeMethod(method));
	}

	@Test
	public void testGetObjectInXML() throws Exception {
		PostMethod createObjMethod = postObjMethod("fdsa");
		client.executeMethod(createObjMethod);

		GetMethod getObjMethod = new GetMethod(serverAddress
				+ "objects/fdsa");
		assertEquals(200, client.executeMethod(getObjMethod));
		String response = getObjMethod.getResponseBodyAsString();
		logger.debug("Retrieved object profile:\n" + response);
		assertTrue("Object had wrong PID!",
				compile("pid=\"fdsa\"").matcher(response).find());
	}

	@Test
	public void testDeleteObject() throws Exception {
		PostMethod createObjmethod = postObjMethod("asdf");
		assertEquals(201, client.executeMethod(createObjmethod));

		DeleteMethod delMethod = new DeleteMethod(serverAddress
				+ "objects/asdf");
		assertEquals(204, client.executeMethod(delMethod));

		GetMethod getMethod = new GetMethod(serverAddress + "objects/asdf");
		assertEquals("Object wasn't really deleted!", 404,
				client.executeMethod(getMethod));
	}

}
