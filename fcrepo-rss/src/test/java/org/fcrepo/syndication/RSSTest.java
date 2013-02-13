
package org.fcrepo.syndication;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml", "/spring-test/rest.xml",
        "/spring-test/eventing.xml"})
public class RSSTest extends AbstractResourceTest {

    final private Logger logger = LoggerFactory.getLogger(RSSTest.class);

    @Test
    public void testRSS() throws Exception {

        assertEquals(201, getStatus(new HttpPost(serverAddress +
                "/objects/RSSTESTPID")));

        HttpGet getRSSMethod = new HttpGet(serverAddress + "/rss");
        HttpResponse response = client.execute(getRSSMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved RSS feed:\n" + content);
        assertTrue("Didn't find the test PID in RSS!", compile("RSSTESTPID",
                DOTALL).matcher(content).find());
    }
}