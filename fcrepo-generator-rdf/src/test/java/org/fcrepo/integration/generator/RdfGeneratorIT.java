
package org.fcrepo.integration.generator;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/generator.xml", "/spring-test/repo.xml",
        "/spring-test/test-container.xml"})
public class RdfGeneratorIT extends AbstractResourceIT {

    @Test
    public void testXMLObjectTriples() throws Exception {

        logger.debug("Executing testXMLObjectTriples()...");
        client.execute(postObjMethod("RdfTest1"));

        final HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest1/fcr:rdf");
        getRdfMethod.setHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());
logger.debug(content);
        String rdfAbout = "rdf:about=\"" + getRdfMethod.getURI().toString().replace("/fcr:rdf","") + "\""; 
        assertTrue("Didn't find identifier: " + rdfAbout,
                content.contains(rdfAbout));
        logger.debug("Finished testXMLObjectTriples().");

    }

    @Test
    public void testNTriplesObjectTriples() throws Exception {
        logger.debug("Executing testNTriplesObjectTriples()...");

        client.execute(postObjMethod("RdfTest2"));

        final HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest2/fcr:rdf");
        final HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());

        String rdfAbout = "rdf:about=\"" + getRdfMethod.getURI().toString().replace("/fcr:rdf","") + "\""; 
        assertTrue("Didn't find identifier: " + rdfAbout,
                content.contains(rdfAbout));
        logger.debug("Finished testNTriplesObjectTriples().");
    }

    @Test
    public void testTurtleObjectTriples() throws Exception {
        logger.debug("Executing testTurtleObjectTriples()...");
        client.execute(postObjMethod("RdfTest3"));

        final HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest3/fcr:rdf");
        getRdfMethod.setHeader("Accept", "text/turtle");

        final HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());

        String rdfAbout = "<" + getRdfMethod.getURI().toString().replace("/fcr:rdf","") + ">"; 
        assertTrue("Didn't find identifier: " + rdfAbout,
                content.contains(rdfAbout));
        logger.debug("Finished testTurtleObjectTriples().");
    }

    @Test
    public void testXMLDSTriples() throws Exception {

        logger.debug("Executing testXMLDSTriples()...");
        client.execute(postObjMethod("RdfTest4"));
        client.execute(postDSMethod("RdfTest4", "testDS", "foobar"));
        final HttpGet getRdfMethod =
                new HttpGet(serverAddress +
                        "objects/RdfTest4/testDS/fcr:rdf");
        getRdfMethod.setHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(getRdfMethod);
        int status = response.getStatusLine().getStatusCode();
        final String content = EntityUtils.toString(response.getEntity());
        if (status != 200) {
            logger.error(content);
        }
        assertEquals(200, status);

        String rdfAbout = "rdf:about=\"" + getRdfMethod.getURI().toString().replace("/fcr:rdf","") + "\""; 
        assertTrue("Didn't find identifier: " + rdfAbout,
                content.contains(rdfAbout));
        logger.debug("Finished testXMLDSTriples().");

    }

}
