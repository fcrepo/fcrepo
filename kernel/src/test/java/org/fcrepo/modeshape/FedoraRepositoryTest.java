package org.fcrepo.modeshape;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "/spring-test/rest.xml", "/spring-test/repo.xml" })
public class FedoraRepositoryTest extends AbstractResourceTest {

	@Test
	public void testDescribeModeshape() throws Exception {
		GetMethod method = new GetMethod(serverAddress + "describe/modeshape");
		int status = client.executeMethod(method);
		assertEquals(200, status);
	}

	@Test
	public void testGetObjects() throws Exception {
		GetMethod method = new GetMethod(serverAddress + "objects");
		int status = client.executeMethod(method);
		assertEquals(200, status);
	}

	@Test
	public void testDescribe() throws Exception {
		GetMethod method = new GetMethod(serverAddress + "describe");
		method.addRequestHeader("Accept", TEXT_XML);
		int status = client.executeMethod(method);
		assertEquals(200, status);
		final String description = method.getResponseBodyAsString();
		logger.debug("Found the repository description:\n" + description);
		assertTrue(
				"Failed to find a proper repo version",
				compile("<repositoryVersion>.*?</repositoryVersion>").matcher(
						description).find());
	}

	@Test
	public void testDescribeSize() throws Exception {
		GetMethod describeMethod = new GetMethod(serverAddress + "describe");
		describeMethod.addRequestHeader("Accept", TEXT_XML);
		int status = client.executeMethod(describeMethod);
		assertEquals(200, status);
		String description = describeMethod.getResponseBodyAsString();
		logger.debug("Found a repository description:\n" + description);
		Matcher check = compile("<repositorySize>([0-9]+)</repositorySize>",
				DOTALL).matcher(description);
		Long oldSize = null;
		while (check.find()) {
			oldSize = new Long(check.group(1));
		}

		PostMethod createObjMethod = postObjMethod("fdhgsldfhg");
		assertEquals(201, client.executeMethod(createObjMethod));

		GetMethod newDescribeMethod = new GetMethod(serverAddress + "describe");
		newDescribeMethod.addRequestHeader("Accept", TEXT_XML);
		status = client.executeMethod(newDescribeMethod);
		assertEquals(200, status);
		String newDescription = newDescribeMethod.getResponseBodyAsString();
		logger.debug("Found another repository description:\n" + newDescription);
		Matcher newCheck = compile("<repositorySize>([0-9]+)</repositorySize>",
				DOTALL).matcher(newDescription);
		Long newSize = null;
		while (newCheck.find()) {
			newSize = new Long(newCheck.group(1));
		}
		logger.debug("Old size was: " + oldSize + " and new size was: "
				+ newSize);
		assertTrue("No increment in size occurred when we expected one!",
				oldSize < newSize);
	}
}
