
package org.fcrepo.integration.services;

import static org.jgroups.util.Util.assertEquals;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.integration.AbstractIT;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class ObjectServiceIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    @Test
    public void testGetAllObjectsDatastreamSize() throws Exception {
        Session session = repository.login();

        double originalSize = objectService.getAllObjectsDatastreamSize();

        datastreamService.createDatastreamNode(session,
                "testObjectServiceNode", "application/octet-stream",
                new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();

        session = repository.login();

        double afterSize = objectService.getAllObjectsDatastreamSize();

        assertEquals(4.0, afterSize - originalSize);

        session.logout();
    }

}
