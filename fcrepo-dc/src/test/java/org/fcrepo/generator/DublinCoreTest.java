
package org.fcrepo.generator;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/generator.xml", "/spring-test/repo.xml"})
public class DublinCoreTest {

    protected static final int SERVER_PORT = 8080;

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + "/";

    protected final HttpClient client = new HttpClient();

    protected static Logger logger;

    @BeforeClass
    public static void setLogger() {
        logger = LoggerFactory.getLogger(DublinCoreTest.class);
    }

    @Test
    public void testJcrPropertiesBasedOaiDc() throws Exception {
        PostMethod createObjMethod =
                new PostMethod(serverAddress + "objects/fdsa");
        client.executeMethod(createObjMethod);

        GetMethod getWorstCaseOaiMethod =
                new GetMethod(serverAddress + "objects/fdsa/oai_dc");
        getWorstCaseOaiMethod.setRequestHeader("Accept", TEXT_XML);
        int status = client.executeMethod(getWorstCaseOaiMethod);
        assertEquals(200, status);

        final String response = getWorstCaseOaiMethod.getResponseBodyAsString();
        assertTrue("Didn't find oai_dc!", compile("oai_dc", DOTALL).matcher(
                response).find());

        assertTrue("Didn't find dc:identifier!", compile("dc:identifier",
                DOTALL).matcher(response).find());
    }

    @Test
    public void testWellKnownPathOaiDc() throws Exception {
        PostMethod createObjMethod =
                new PostMethod(serverAddress + "objects/lkjh");
        client.executeMethod(createObjMethod);

        PostMethod createDSMethod =
                new PostMethod(serverAddress + "objects/lkjh/datastreams/DC");

        createDSMethod.setRequestEntity(new StringRequestEntity(
                "marbles for everyone", null, null));

        client.executeMethod(createDSMethod);

        GetMethod getWorstCaseOaiMethod =
                new GetMethod(serverAddress + "objects/lkjh/oai_dc");
        getWorstCaseOaiMethod.setRequestHeader("Accept", TEXT_XML);
        int status = client.executeMethod(getWorstCaseOaiMethod);
        assertEquals(200, status);

        final String response = getWorstCaseOaiMethod.getResponseBodyAsString();
        assertTrue("Didn't find our datastream!", compile(
                "marbles for everyone", DOTALL).matcher(response).find());
    }
}
