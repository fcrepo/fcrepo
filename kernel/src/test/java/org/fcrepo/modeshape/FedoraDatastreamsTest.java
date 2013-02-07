package org.fcrepo.modeshape;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "/spring-test/rest.xml", "/spring-test/repo.xml" })
public class FedoraDatastreamsTest extends AbstractResourceTest {

	private static final String faulkner1 = "The past is never dead. It's not even past.";

	@Test
	public void testGetDatastreams() throws Exception {
		PostMethod pmethod = postObjMethod("asdf");
		client.executeMethod(pmethod);

		GetMethod method = new GetMethod(serverAddress
				+ "objects/asdf/datastreams");
		int status = client.executeMethod(method);
		assertEquals(200, status);
	}

	@Test
	public void testAddDatastream() throws Exception {
		PostMethod pmethod = postObjMethod("asdf");
		client.executeMethod(pmethod);

		PostMethod method = postDSMethod("asdf", "zxc");
		int status = client.executeMethod(method);
		assertEquals(201, status);
	}

	@Test
	public void testMutateDatastream() throws Exception {
		PostMethod createObjectMethod = postObjMethod("asdf2");
		Integer status = client.executeMethod(createObjectMethod);
		assertEquals("Couldn't create an object!", (Integer) 201, status);

		PostMethod createDataStreamMethod = postDSMethod("asdf2", "vcxz");
		status = client.executeMethod(createDataStreamMethod);
		assertEquals("Couldn't create a datastream!", (Integer) 201, status);

		PutMethod mutateDataStreamMethod = putDSMethod("asdf2", "vcxz");
		mutateDataStreamMethod.setRequestEntity(new StringRequestEntity(
				faulkner1, "text/plain", "UTF-8"));
		status = client.executeMethod(mutateDataStreamMethod);
		assertEquals("Couldn't mutate a datastream!", (Integer) 201, status);

		GetMethod retrieveMutatedDataStreamMethod = new GetMethod(serverAddress
				+ "objects/asdf2/datastreams/vcxz/content");
		client.executeMethod(retrieveMutatedDataStreamMethod);
		String response = retrieveMutatedDataStreamMethod
				.getResponseBodyAsString();
		logger.debug("Retrieved mutated datastream content: " + response);
		assertTrue("Datastream didn't accept mutation!", compile(faulkner1)
				.matcher(response).find());
	}

	@Test
	public void testGetDatastream() throws Exception {
		PostMethod pmethod = postObjMethod("asdf");
		client.executeMethod(pmethod);

		GetMethod method_test_get = new GetMethod(serverAddress
				+ "objects/asdf/datastreams/poiu");
		int status = client.executeMethod(method_test_get);
		assertEquals(404, status);

		PostMethod method = postDSMethod("asdf", "poiu");
		status = client.executeMethod(method);
		assertEquals(201, status);

		GetMethod method_2 = new GetMethod(serverAddress
				+ "objects/asdf/datastreams/poiu");
		status = client.executeMethod(method_2);
		assertEquals(200, status);
	}

	@Test
	public void testDeleteDatastream() throws Exception {
		PostMethod pmethod = postObjMethod("asdf");
		client.executeMethod(pmethod);

		PostMethod method = postDSMethod("asdf", "lkjh");
		int status = client.executeMethod(method);
		assertEquals(201, status);

		GetMethod method_2 = new GetMethod(serverAddress
				+ "objects/asdf/datastreams/lkjh");
		status = client.executeMethod(method_2);
		assertEquals(200, status);

		DeleteMethod dmethod = new DeleteMethod(serverAddress
				+ "objects/asdf/datastreams/lkjh");
		status = client.executeMethod(dmethod);
		assertEquals(204, status);

		GetMethod method_test_get = new GetMethod(serverAddress
				+ "objects/asdf/datastreams/lkjh");
		status = client.executeMethod(method_test_get);
		assertEquals(404, status);
	}

	@Test
	public void testGetDatastreamContent() throws Exception {
		final PostMethod createObjMethod = postObjMethod("testfoo");
		client.executeMethod(createObjMethod);
		assertEquals(201, client.executeMethod(createObjMethod));

		final PostMethod createDSMethod = postDSMethod("testfoo", "testfoozle");
		createDSMethod.setRequestEntity(new StringRequestEntity(
				"marbles for everyone", null, null));
		assertEquals(201, client.executeMethod(createDSMethod));
		GetMethod method_test_get = new GetMethod(serverAddress
				+ "objects/testfoo/datastreams/testfoozle/content");
		assertEquals(200, client.executeMethod(method_test_get));
		assertEquals("Got the wrong content back!", "marbles for everyone",
				method_test_get.getResponseBodyAsString());
	}

	@Test
	public void testMultipleDatastreams() throws Exception {
		final PostMethod createObjMethod = postObjMethod("testfoo");
		client.executeMethod(createObjMethod);
		assertEquals(201, client.executeMethod(createObjMethod));

		final PostMethod createDS1Method = postDSMethod("testfoo", "testfoozle");
		createDS1Method.setRequestEntity(new StringRequestEntity(
				"marbles for everyone", null, null));
		assertEquals(201, client.executeMethod(createDS1Method));
		final PostMethod createDS2Method = postDSMethod("testfoo",
				"testfoozle2");
		createDS2Method.setRequestEntity(new StringRequestEntity(
				"marbles for no one", null, null));
		assertEquals(201, client.executeMethod(createDS2Method));

		final GetMethod getDSesMethod = new GetMethod(serverAddress
				+ "objects/testfoo/datastreams");
		assertEquals(200, client.executeMethod(getDSesMethod));
		final String response = getDSesMethod.getResponseBodyAsString();
		assertTrue("Didn't find the first datastream!",
				compile("dsid=\"testfoozle\"", DOTALL).matcher(response).find());
		assertTrue("Didn't find the second datastream!",
				compile("dsid=\"testfoozle2\"", DOTALL).matcher(response)
						.find());
	}
}
