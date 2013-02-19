
package org.fcrepo.legacy.foxml;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.CharStreams.newReaderSupplier;
import static java.lang.Thread.sleep;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

public class FedoraOXMLTest extends AbstractResourceTest {

    final private String pid = "test";

    @Test
    public void importFOXML() throws ClientProtocolException, IOException,
            InterruptedException {
        logger.debug("Beginning " + this.getClass().getName());
        final String foxml =
                CharStreams.toString(newReaderSupplier(foxmlSup, UTF_8));
        HttpPut foxmlMessage = new HttpPut(serverAddress + "foxml/" + pid);
        foxmlMessage.setEntity(new StringEntity(foxml));
        foxmlMessage.addHeader("Content-Type", TEXT_XML);
        client.execute(foxmlMessage);
        sleep(2000);
        assertEquals("Couldn't find the object from the FOXML!", OK
                .getStatusCode(), getStatus(new HttpGet(serverAddress +
                "objects/" + pid)));
    }

    final InputSupplier<InputStream> foxmlSup =
            new InputSupplier<InputStream>() {

                public InputStream getInput() throws IOException {
                    return this.getClass().getResourceAsStream(
                            "/foxml-example.xml");
                }
            };
}
