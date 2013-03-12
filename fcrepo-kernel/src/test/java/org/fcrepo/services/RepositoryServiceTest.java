package org.fcrepo.services;

import org.apache.commons.io.IOUtils;
import org.fcrepo.AbstractTest;
import org.fcrepo.Datastream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.fcrepo.services.DatastreamService.createDatastreamNode;
import static org.fcrepo.services.DatastreamService.getDatastream;
import static org.fcrepo.services.ObjectService.createObjectNode;
import static org.fcrepo.services.RepositoryService.getContentBlobs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class RepositoryServiceTest {

    @Inject
    Repository repo;

    @Test
    public void testGetBlobs() throws Exception {
        Session session = repo.login();
        createObjectNode(session, "testObject");
        createDatastreamNode(session,
                "/objects/testObject/testRepositoryContent",
                "application/octet-stream", new ByteArrayInputStream(
                "0123456789".getBytes()));


        session.save();

        final Datastream ds = getDatastream("testObject", "testRepositoryContent");

        Iterator<InputStream> inputStreamList = getContentBlobs(ds.getNode()).values().iterator();

        int i = 0;
        while(inputStreamList.hasNext()) {
            InputStream is = inputStreamList.next();

            String myString = IOUtils.toString(is, "UTF-8");

            assertEquals("0123456789", myString);

            i++;
        }

        assertNotEquals(0, i);

    }
}
