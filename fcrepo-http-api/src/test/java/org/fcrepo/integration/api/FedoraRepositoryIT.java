
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams.DatastreamElement;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.junit.Test;

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
                "<repositoryVersion>.*?</repositoryVersion>").matcher(
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
                compile("<repositorySize>([0-9]+)</repositorySize>", DOTALL)
                        .matcher(description);
        Long oldSize = null;
        while (check.find()) {
            oldSize = new Long(check.group(1));
        }

        assertEquals(201, getStatus(postObjMethod("fdhgsldfhg")));
        assertEquals(201, getStatus(postDSMethod("fdhgsldfhg", "asdf", "1234")));

        final HttpGet newDescribeMethod =
                new HttpGet(serverAddress + "describe");
        newDescribeMethod.addHeader("Accept", TEXT_XML);
        response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String newDescription =
                EntityUtils.toString(response.getEntity());
        logger.debug("Found another repository description:\n" + newDescription);
        final Matcher newCheck =
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
        method.addHeader("Accept-Mixin", "fcr:object");
        HttpResponse response = execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        Collection<String> childNames = parseChildren(response.getEntity());
        assertEquals(1, childNames.size());
        assertEquals("TestSubdir", childNames.iterator().next());
        method = new HttpGet(serverAddress + "files/FileSystem1");
        method.addHeader("Accept-Mixin", "fcr:datastream");
        response = execute(method);
        childNames = parseDatastreams(response.getEntity());
        assertEquals(2, childNames.size());
        assertTrue(childNames.contains("ds1"));
        assertTrue(childNames.contains("ds2"));
    }
    
    static Collection<String> parseChildren(HttpEntity entity) throws IOException {
        String body = EntityUtils.toString(entity);
        System.err.println(body);
        String [] names = body.replace("[","").replace("]", "").trim().split(",\\s?");
        return Arrays.asList(names);
    }
    
    static Collection<String> parseDatastreams(HttpEntity entity) throws Exception {
        String body = EntityUtils.toString(entity);
        System.err.println(body);
        final JAXBContext context =
                JAXBContext.newInstance(ObjectDatastreams.class);
        final Unmarshaller um = context.createUnmarshaller();
        final ObjectDatastreams objectDs =
                (ObjectDatastreams) um.unmarshal(new java.io.StringReader(
                        body));
        Set<DatastreamElement> dss = objectDs.datastreams;
        ArrayList<String> result = new ArrayList<String>(dss.size());
        for (DatastreamElement ds: dss) {
            result.add(ds.dsid);
        }
        return result;

    }

}
