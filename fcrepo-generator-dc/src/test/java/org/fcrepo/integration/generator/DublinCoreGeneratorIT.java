
package org.fcrepo.integration.generator;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;

@ContextConfiguration({"/spring-test/test-container.xml"})
public class DublinCoreGeneratorIT extends AbstractResourceIT {

    @Test
    public void testJcrPropertiesBasedOaiDc() throws Exception {
		final int status = getStatus( postObjMethod("DublinCoreTest1"));
		assertEquals(201, status);
		final HttpPost post = postObjMethod("DublinCoreTest1");
		post.setHeader("Content-Type", "application/sparql-update");
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContent(new ByteArrayInputStream("INSERT { <info:fedora/objects/DublinCoreTest1> <http://purl.org/dc/terms/identifier> \"this is an identifier\" } WHERE {}".getBytes()));
		post.setEntity(entity);
		assertEquals(204, getStatus( post));
        final HttpGet getWorstCaseOaiMethod =
                new HttpGet(serverOAIAddress + "objects/DublinCoreTest1/oai_dc");
        getWorstCaseOaiMethod.setHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(getWorstCaseOaiMethod);

        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());
		logger.debug("Got content: {}", content);
        assertTrue("Didn't find oai_dc!", compile("oai_dc", DOTALL).matcher(
                content).find());

        assertTrue("Didn't find dc:identifier!", compile("dc:identifier",
                DOTALL).matcher(content).find());
    }

    @Test
    public void testWellKnownPathOaiDc() throws Exception {
        HttpResponse response =
                client.execute(postObjMethod("DublinCoreTest2"));
        assertEquals(201, response.getStatusLine().getStatusCode());
        response =
                client.execute(postDSMethod("DublinCoreTest2", "DC",
                        "marbles for everyone"));
        int status = response.getStatusLine().getStatusCode();
        if (status != 201) {
            System.err.println(EntityUtils.toString(response.getEntity()));
        }
        assertEquals(201, status);

        final HttpGet getWorstCaseOaiMethod =
                new HttpGet(serverOAIAddress + "objects/DublinCoreTest2/oai_dc");
        getWorstCaseOaiMethod.setHeader("Accept", TEXT_XML);
        response = client.execute(getWorstCaseOaiMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find our datastream!", compile(
                "marbles for everyone", DOTALL).matcher(content).find());
    }
}
