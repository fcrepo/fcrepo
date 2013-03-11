
package org.fcrepo.generator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration({"/spring-test/generator.xml", "/spring-test/repo.xml"})
public class RdfGeneratorTest extends AbstractResourceTest {

    @Test
    public void testXMLObjectTriples() throws Exception {

        logger.debug("Executing testXMLObjectTriples()...");
        client.execute(postObjMethod("RdfTest1"));

        HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest1/rdf");
        getRdfMethod.setHeader("Accept", TEXT_XML);
        HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find identifier!", compile("identifier", DOTALL)
                .matcher(content).find());
        logger.debug("Finished testXMLObjectTriples().");

    }

    @Test
    public void testNTriplesObjectTriples() throws Exception {
        logger.debug("Executing testNTriplesObjectTriples()...");

        client.execute(postObjMethod("RdfTest2"));

        HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest2/rdf");
        HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find identifier!", compile("identifier", DOTALL)
                .matcher(content).find());
        logger.debug("Finished testNTriplesObjectTriples().");
    }

    @Test
    public void testTurtleObjectTriples() throws Exception {
        logger.debug("Executing testTurtleObjectTriples()...");
        client.execute(postObjMethod("RdfTest3"));

        HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest3/rdf");
        getRdfMethod.setHeader("Accept", "text/turtle");

        HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find identifier!", compile("identifier", DOTALL)
                .matcher(content).find());

        logger.debug("Finished testTurtleObjectTriples().");
    }

}
