
package org.fcrepo.integration.api.legacy;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class FedoraIdentifiersIT extends AbstractResourceIT {

    @Test
    public void testGetNextPidResponds() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "nextPID");
        method.addHeader("Accepts", "text/xml");
        logger.debug("Executed testGetNextPidResponds()");
        assertEquals(SC_OK, getStatus(method));
    }

    @Test
    public void testGetNextPidHasAPid() throws IOException, XpathException,
            SAXException {
        final HttpPost method =
                new HttpPost(serverAddress + "nextPID?numPids=1");
        method.addHeader("Accepts", "text/xml");
        final HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasAPid()");
        final String content = EntityUtils.toString(response.getEntity());
        assertXpathExists("/pids/pid", content);
        logger.debug("Found a PID.");
    }

    @Test
    public void testGetNextPidHasTwoPids() throws IOException, XpathException,
            SAXException {
        final HttpPost method =
                new HttpPost(serverAddress + "nextPID?numPids=2");
        method.addHeader("Accepts", "text/xml");
        final HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasTwoPids()");
        final String content = EntityUtils.toString(response.getEntity());
        assertXpathExists("/pids/pid[2]", content);
        logger.debug("Found two PIDs.");
    }
}
