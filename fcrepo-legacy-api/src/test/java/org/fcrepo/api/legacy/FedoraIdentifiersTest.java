
package org.fcrepo.api.legacy;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class FedoraIdentifiersTest extends AbstractResourceTest {

    @Test
    public void testGetNextPidResponds() throws Exception {
        HttpPost method = new HttpPost(serverAddress + "nextPID");
        method.addHeader("Accepts", "text/xml");
        logger.debug("Executed testGetNextPidResponds()");
        assertEquals(HttpServletResponse.SC_OK, getStatus(method));
    }

    @Test
    public void testGetNextHasAPid() throws IOException {
        HttpPost method = new HttpPost(serverAddress + "nextPID?numPids=1");
        method.addHeader("Accepts", "text/xml");
        HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasAPid()");
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Only to find:\n" + content);
        assertTrue("Didn't find a single dang PID!", compile("<pid>.*?</pid>",
                DOTALL).matcher(content).find());
    }

    @Test
    public void testGetNextHasTwoPids() throws IOException {
        HttpPost method = new HttpPost(serverAddress + "nextPID?numPids=2");
        method.addHeader("Accepts", "text/xml");
        HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasTwoPids()");
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Only to find:\n" + response);
        assertTrue("Didn't find a two dang PIDs!", compile(
                "<pid>.*?</pid>.*?<pid>.*?</pid>", DOTALL).matcher(content)
                .find());
    }
}
