
package org.fcrepo;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.*;

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
public class ObjectTest extends AbstractTest {

    @Inject
    Repository repo;

    @Test
    public void testLabel() throws RepositoryException, IOException {
        Session session = repo.login();
        Node dsNode = createObjectNode(session, "testObject");
        new FedoraObject(dsNode).setLabel("Best object ever!");
        session.save();
        session.logout();

        session = repo.login();
        final FedoraObject obj = getObject("testObject");
        assertEquals("Wrong label!", "Best object ever!", obj.getLabel());
    }

    @Test
    public void testDoubleLabel() throws RepositoryException, IOException {
        Session session = repo.login();
        Node dsNode = createObjectNode(session, "testObject");
        new FedoraObject(dsNode).setLabel("Worst object ever!");
        session.save();
        session.logout();

        session = repo.login();
        dsNode = createObjectNode(session, "testObject");
        new FedoraObject(dsNode).setLabel("Best object ever!");
        session.save();
        session.logout();

        session = repo.login();
        final FedoraObject obj = getObject("testObject");
        assertEquals("Wrong label!", "Best object ever!", obj.getLabel());
    }

}
