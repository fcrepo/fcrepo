package org.fcrepo.integration.generator;


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
public class RdfGeneratorIT extends AbstractResourceIT {

    @Test
    public void testJcrPropertiesBasedTriples() throws Exception {
        getStatus(postObjMethod("RdfTest1"));

        HttpGet getRdfMethod =
                new HttpGet(serverAddress + "objects/RdfTest1/rdf");
        getRdfMethod.setHeader("Accept", TEXT_XML);
        HttpResponse response = client.execute(getRdfMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find purl!", compile("purl", DOTALL).matcher(
                content).find());

        assertTrue("Didn't find identifier!", compile("identifier",
                DOTALL).matcher(content).find());
    }
}
