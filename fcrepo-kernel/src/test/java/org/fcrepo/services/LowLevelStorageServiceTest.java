package org.fcrepo.services;

import static org.fcrepo.services.DatastreamService.createDatastreamNode;
import static org.fcrepo.services.DatastreamService.getDatastream;
import static org.fcrepo.services.LowLevelStorageService.getBinaryBlobs;
import static org.fcrepo.services.ObjectService.createObjectNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class LowLevelStorageServiceTest {

    @Inject
    Repository repo;

    @Test
    public void testChecksumBlobs() throws Exception {

        Session session = repo.login();
        createObjectNode(session, "testObject");
        createDatastreamNode(session,
                "/objects/testObject/testRepositoryContent",
                "application/octet-stream", new ByteArrayInputStream(
                "0123456789".getBytes()));


        session.save();

        final Datastream ds = getDatastream("testObject", "testRepositoryContent");

        final Collection<FixityResult> fixityResults = LowLevelStorageService.getFixity(
        		ds.getNode(), MessageDigest.getInstance("SHA-1"),
        		ds.getContentDigest(), ds.getContentSize());

        assertNotEquals(0, fixityResults.size());

        for (FixityResult fixityResult : fixityResults) {
            assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016", fixityResult.computedChecksum.toString());
        }
    }

    @Test
    public void testGetBinaryBlobs() throws Exception {
        Session session = repo.login();
        createObjectNode(session, "testObject");
        createDatastreamNode(session,
                "/objects/testObject/testRepositoryContent",
                "application/octet-stream", new ByteArrayInputStream(
                "0123456789".getBytes()));


        session.save();

        final Datastream ds = getDatastream("testObject", "testRepositoryContent");

        Iterator<InputStream> inputStreamList = getBinaryBlobs(ds.getNode()).values().iterator();

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
