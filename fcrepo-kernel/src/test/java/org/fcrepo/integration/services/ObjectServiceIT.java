package org.fcrepo.integration.services;


import org.apache.tika.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.integration.AbstractIT;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;


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

        datastreamService.createDatastreamNode(session, "testObjectServiceNode",
                "application/octet-stream", new ByteArrayInputStream("asdf"
                .getBytes()));
        session.save();
        session.logout();

        session = repository.login();

        double afterSize = objectService.getAllObjectsDatastreamSize();

        assertEquals(4.0, afterSize - originalSize);

        session.logout();
    }

}
