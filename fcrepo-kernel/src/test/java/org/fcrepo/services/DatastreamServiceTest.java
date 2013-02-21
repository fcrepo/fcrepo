
package org.fcrepo.services;

import static org.fcrepo.services.DatastreamService.createDatastreamNode;
import static org.fcrepo.services.DatastreamService.getDatastream;
import static org.fcrepo.services.ObjectService.createObjectNode;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.tika.io.IOUtils;
import org.fcrepo.AbstractTest;
import org.fcrepo.Datastream;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamServiceTest extends AbstractTest {

    @Inject
    private Repository repository;

    @Test
    public void testCreateDatastreamNode() throws Exception {
        Session session = repository.login();
        createDatastreamNode(session, "testDatastreamNode",
                "application/octet-stream", new ByteArrayInputStream("asdf"
                        .getBytes()));
        session.save();
        session.logout();
        session = repository.login();

        assertTrue(session.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("asdf", session.getNode("/testDatastreamNode").getNode(
                JCR_CONTENT).getProperty(JCR_DATA).getString());
        session.logout();
    }

    @Test
    public void testGetDatastreamContentInputStream() throws Exception {
        Session session = repository.login();
        InputStream is = new ByteArrayInputStream("asdf".getBytes());
        createObjectNode(session, "testObject");
        createDatastreamNode(session, "/objects/testObject/testDatastreamNode",
                "application/octet-stream", is);

        session.save();
        session.logout();
        session = repository.login();
        final Datastream ds = getDatastream("testObject", "testDatastreamNode");
        assertEquals("asdf", IOUtils.toString(ds.getContent(), "UTF-8"));
        session.logout();
    }
}
