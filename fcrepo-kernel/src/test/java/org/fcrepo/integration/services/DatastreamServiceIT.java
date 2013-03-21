
package org.fcrepo.integration.services;

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
import org.fcrepo.Datastream;
import org.fcrepo.integration.AbstractIT;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamServiceIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;
    
    @Test
    public void testCreateDatastreamNode() throws Exception {
        Session session = repository.login();
        datastreamService.createDatastreamNode(session, "testDatastreamNode",
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
        objectService.createObjectNode(session, "testDatastreamServiceObject");
        datastreamService.createDatastreamNode(session, "/objects/testDatastreamServiceObject/testDatastreamNode",
                "application/octet-stream", is);

        session.save();
        session.logout();
        session = repository.login();
        final Datastream ds = datastreamService.getDatastream("testDatastreamServiceObject", "testDatastreamNode");
        assertEquals("asdf", IOUtils.toString(ds.getContent(), "UTF-8"));
        session.logout();
    }
}
