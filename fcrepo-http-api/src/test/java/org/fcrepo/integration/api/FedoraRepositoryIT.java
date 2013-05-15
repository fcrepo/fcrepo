
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraRepositoryIT extends AbstractResourceIT {

    @Test
    public void testDescribeModeshape() throws Exception {
        assertEquals(200, getStatus(new HttpGet(serverAddress +
                "fcr:describe/modeshape")));
    }

    @Test
    public void testGetObjects() throws Exception {
        assertEquals(200, getStatus(new HttpGet(serverAddress + "objects")));
    }

    @Test
    public void testDescribe() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:describe");
        method.addHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found the repository description:\n" + description);
        assertTrue("Failed to find a proper repo version", compile(
                "<.*repositoryVersion>.*?<.*repositoryVersion>").matcher(
                description).find());
    }

    @Test
    public void testDescribeJSON() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:describe");
        method.addHeader("Accept", APPLICATION_JSON);
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found the repository description:\n" + description);
        assertTrue("Failed to find a proper repo version", compile(
                "\"repositoryVersion\"\\s*:\\s*\".*\"").matcher(
                description).find());
    }

    @Test
    public void testDescribeHtml() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:describe");
        method.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found the repository description:\n" + description);
        assertTrue("Failed to find a proper repo version", compile(
                "Number Of Objects").matcher(description).find());
    }

    @Test
    public void testDescribeSize() throws Exception {
        final HttpGet describeMethod = new HttpGet(serverAddress + "fcr:describe");
        describeMethod.addHeader("Accept", TEXT_XML);
        HttpResponse response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String description = EntityUtils.toString(response.getEntity());
        logger.debug("Found a repository description:\n" + description);
        final Matcher check =
                compile("<.*repositorySize>([0-9]+)<.*repositorySize>", DOTALL)
                        .matcher(description);
        Long oldSize = null;
        while (check.find()) {
            oldSize = new Long(check.group(1));
        }

        assertEquals(201, getStatus(postObjMethod("fdhgsldfhg")));
        assertEquals(201, getStatus(postDSMethod("fdhgsldfhg", "asdf", "1234")));

        final HttpGet newDescribeMethod =
                new HttpGet(serverAddress + "fcr:describe");
        newDescribeMethod.addHeader("Accept", TEXT_XML);
        response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String newDescription =
                EntityUtils.toString(response.getEntity());
        logger.debug("Found another repository description:\n" + newDescription);
        final Matcher newCheck =
                compile("<.*repositorySize>([0-9]+)<.*repositorySize>", DOTALL)
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
    
    /**
     * Given a directory at:
     *  test-objects/FileSystem1/
     *                          /ds1
     *                          /ds2
     *                          /TestSubdir/
     * and a projection of test-objects as fedora:/files,
     * then I should be able to retrieve an object from fedora:/files/FileSystem1
     * that lists a child object at fedora:/files/FileSystem1/TestSubdir
     * and lists datastreams ds1 and ds2
     */
    @Test
    public void testGetProjectedNode() throws Exception {
        HttpGet method = new HttpGet(serverAddress + "files/FileSystem1");
        HttpResponse response = execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());

        GraphStore result = TestHelpers.parseTriples(response.getEntity().getContent());
        assertTrue("Didn't find the first datastream! ", result.contains(Node.ANY, Node.createURI("info:fedora/files/FileSystem1"), Node.ANY, Node.createURI("info:fedora/files/FileSystem1/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(Node.ANY, Node.createURI("info:fedora/files/FileSystem1"), Node.ANY, Node.createURI("info:fedora/files/FileSystem1/ds2")));
        assertTrue("Didn't find the first object! ", result.contains(Node.ANY, Node.createURI("info:fedora/files/FileSystem1"), Node.ANY, Node.createURI("info:fedora/files/FileSystem1/TestSubdir")));

    }

}
