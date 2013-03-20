
package org.fcrepo.integration.api.legacy;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//import java.io.File;
import java.util.regex.Matcher;

//import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
//import org.custommonkey.xmlunit.jaxp13.Validator;
import org.junit.Test;

public class FedoraRepositoryIT extends AbstractResourceIT {

    @Test
    public void testDescribeModeshape() throws Exception {
        assertEquals(200, getStatus(new HttpGet(serverAddress +
                "describe/modeshape")));
    }

    @Test
    public void testGetObjects() throws Exception {
        assertEquals(200, getStatus(new HttpGet(serverAddress + "objects")));
    }

    @Test
    public void testDescribe() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "describe");
        method.addHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found the repository description:\n" + description);
        assertXpathExists("/access:fedoraRepository/access:repositoryVersion",
                description);
        logger.debug("Found repository version element.");
    }

    // TODO we can't use this test for now because our responses are not XML 
    // valid according to fcrepo3 XML schemata
/*    @Test
    public void testDescribeResponseIsValid() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "describe");
        method.addHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final Validator v = new Validator();
        logger.debug("Using describeRepository schema from: " +
                this.getClass().getResource("/xsd/fedoraRepository.xsd")
                        .toString());
        v.addSchemaSource(new StreamSource(new File(this.getClass()
                .getResource("/xsd/fedoraRepository.xsd").getFile())));
        for (Object e : v.getSchemaErrors()) {
            logger.debug("Found SAXParseException in XML Schema: " +
                    e.toString());
        }
        for (Object e : v.getInstanceErrors(new StreamSource(response
                .getEntity().getContent()))) {
            logger.debug("Found SAXParseException in describeRepository response: " +
                    e.toString());

        }
        assertTrue("Not a valid Fedora Repository descripion!", v
                .isInstanceValid(new StreamSource(response.getEntity()
                        .getContent())));
        logger.debug("Found valid Fedora Repository descripion.");
    }*/

    @Test
    public void testDescribeSize() throws Exception {
        final HttpGet describeMethod = new HttpGet(serverAddress + "describe");
        describeMethod.addHeader("Accept", TEXT_XML);
        HttpResponse response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found a repository description:\n" + description);
        final Matcher check =
                compile("<repositorySize>([0-9]+)</repositorySize>", DOTALL)
                        .matcher(description);
        Long oldSize = null;
        while (check.find()) {
            oldSize = new Long(check.group(1));
        }

        assertEquals(201, getStatus(postObjMethod("fdhgsldfhg")));

        HttpGet newDescribeMethod = new HttpGet(serverAddress + "describe");
        newDescribeMethod.addHeader("Accept", TEXT_XML);
        response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String newDescription =
                EntityUtils.toString(response.getEntity());
        logger.debug("Found another repository description:\n" + newDescription);
        Matcher newCheck =
                compile("<repositorySize>([0-9]+)</repositorySize>", DOTALL)
                        .matcher(newDescription);
        Long newSize = null;
        while (newCheck.find()) {
            newSize = new Long(newCheck.group(1));
        }
        logger.debug("Old size was: " + oldSize + " and new size was: " +
                newSize);
        assertTrue("No increment in size occurred when we expected one!",
                oldSize < newSize);
    }
}
