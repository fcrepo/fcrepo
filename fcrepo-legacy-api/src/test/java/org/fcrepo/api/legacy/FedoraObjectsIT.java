
package org.fcrepo.api.legacy;

import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathValuesEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.custommonkey.xmlunit.jaxp13.Validator;
import org.junit.Test;

public class FedoraObjectsIT extends AbstractResourceIT {

    @Test
    public void testIngest() throws Exception {
        final HttpPost method = postObjMethod("FedoraObjectsTest1");
        final HttpResponse response = client.execute(method);
        assertEquals(201, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
    }

    @Test
    public void testIngestWithNew() throws Exception {
        final HttpPost method = postObjMethod("new");
        final HttpResponse response = client.execute(method);
        assertEquals(201, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
    }

    @Test
    public void testGetObjectInXML() throws Exception {
        client.execute(postObjMethod("FedoraObjectsTest2"));
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + "objects/FedoraObjectsTest2");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved object profile:\n" + content);
        assertXpathExists("/access:objectProfile/@pid", content);
        logger.debug("Found PID attribute on object profile.");
        assertXpathValuesEqual("'FedoraObjectsTest2'",
                "/access:objectProfile/@pid", content);
        logger.debug("PID attribute on object profile has correct value.");
    }

    @Test
    public void testObjectProfileResponseIsValid() throws Exception {
        client.execute(postObjMethod("FedoraObjectsTest3"));
        final HttpGet method =
                new HttpGet(serverAddress + "objects/FedoraObjectsTest3");
        method.addHeader("Accept", TEXT_XML);
        final HttpResponse response = client.execute(method);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String profile = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved object profile for validation: {}", profile);
        final Validator v = new Validator();
        logger.debug("Using objectProfile schema from: " +
                this.getClass().getResource("/xsd/objectProfile.xsd")
                        .toString());
        v.addSchemaSource(new StreamSource(new File(this.getClass()
                .getResource("/xsd/objectProfile.xsd").getFile())));
        for (Object e : v.getInstanceErrors(new StreamSource(
                new ByteArrayInputStream(profile.getBytes())))) {
            logger.debug("Found SAXParseException in objectProfile response: " +
                    e.toString());
        }
        assertTrue("Not a valid Fedora object description!", v
                .isInstanceValid(new StreamSource(new ByteArrayInputStream(
                        profile.getBytes()))));
        logger.debug("Found valid Fedora object description.");
    }

    @Test
    public void testDeleteObject() throws Exception {
        assertEquals(201, getStatus(postObjMethod("FedoraObjectsTest4")));
        assertEquals(204, getStatus(new HttpDelete(serverAddress +
                "objects/FedoraObjectsTest4")));
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(serverAddress +
                        "objects/FedoraObjectsTest4")));
    }

}
