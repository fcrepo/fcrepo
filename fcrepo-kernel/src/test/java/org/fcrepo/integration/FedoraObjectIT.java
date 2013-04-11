
package org.fcrepo.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraObjectIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;

    @Test
    public void testCreatedObject() throws RepositoryException, IOException {
        Session session = repo.login();
        objectService.createObject(session, "testObject");
        session.save();
        session.logout();
        session = repo.login();
        final FedoraObject obj = objectService.getObject("testObject");
        assertNotNull("Couldn't find object!", obj);
    }

    @Test
    public void testOwnerId() throws RepositoryException, IOException {
        Session session = repo.login();
        objectService.createObject(session, "testObject").setOwnerId("ajs6f");
        session.save();
        session.logout();
        session = repo.login();
        final String ownerId =
                objectService.getObject("testObject").getOwnerId();
        assertEquals("Couldn't find object owner ID!", "ajs6f", ownerId);
    }
}
