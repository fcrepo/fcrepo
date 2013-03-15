
package org.fcrepo.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.fcrepo.api.AbstractResourceTest;
import org.junit.Test;

public class FedoraRepositoryTest extends AbstractResourceTest {

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
        assertTrue("Failed to find a proper repo version", compile(
                "<repositoryVersion>.*?</repositoryVersion>").matcher(
                description).find());
    }

    @Test
    public void testDescribeHtml() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "describe");
        method.addHeader("Accept", TEXT_HTML_TYPE.toString());
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found the repository description:\n" + description);
        assertTrue("Failed to find a proper repo version", compile(
                "Number Of Objects: ").matcher(
                description).find());
    }

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
        final String newDescription = EntityUtils.toString(response.getEntity());
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
