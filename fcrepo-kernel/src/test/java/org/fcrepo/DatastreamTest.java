
package org.fcrepo;

import static org.fcrepo.services.DatastreamService.createDatastreamNode;
import static org.fcrepo.services.DatastreamService.getDatastream;
import static org.fcrepo.services.ObjectService.createObjectNode;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamTest extends AbstractTest {

    @Inject
    Repository repo;

    @Test
    public void testLabel() throws RepositoryException, IOException {
        Session session = repo.login();
        createObjectNode(session, "testObject");
        Node dsNode =
                createDatastreamNode(session,
                        "/objects/testObject/testDatastreamNode",
                        "application/octet-stream", new ByteArrayInputStream(
                                "asdf".getBytes()));
        new Datastream(dsNode).setLabel("Best datastream ever!");
        session.save();
        session.logout();

        session = repo.login();
        final Datastream ds = getDatastream("testObject", "testDatastreamNode");
        assertEquals("Wrong label!", "Best datastream ever!", ds.getLabel());
    }
}
