
package org.fcrepo.generator;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/generator.xml", "/spring-test/repo.xml"})
public class DublinCoreTest extends AbstractResourceTest {

    @Test
    public void testJcrPropertiesBasedOaiDc() throws Exception {
        getStatus(postObjMethod("DublinCoreTest1"));

        HttpGet getWorstCaseOaiMethod =
                new HttpGet(serverAddress + "objects/DublinCoreTest1/oai_dc");
        getWorstCaseOaiMethod.setHeader("Accept", TEXT_XML);
        HttpResponse response = client.execute(getWorstCaseOaiMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find oai_dc!", compile("oai_dc", DOTALL).matcher(
                content).find());

        assertTrue("Didn't find dc:identifier!", compile("dc:identifier",
                DOTALL).matcher(content).find());
    }

    @Test
    public void testWellKnownPathOaiDc() throws Exception {
        client.execute(postObjMethod("DublinCoreTest2"));
        client.execute(postDSMethod("DublinCoreTest2", "DC",
                "marbles for everyone"));

        HttpGet getWorstCaseOaiMethod =
                new HttpGet(serverAddress + "objects/DublinCoreTest2/oai_dc");
        getWorstCaseOaiMethod.setHeader("Accept", TEXT_XML);
        HttpResponse response = client.execute(getWorstCaseOaiMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find our datastream!", compile(
                "marbles for everyone", DOTALL).matcher(content).find());
    }
}
