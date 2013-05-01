
package org.fcrepo.integration.services;

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
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class LowLevelStorageServiceIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    @Inject
    LowLevelStorageService lowLevelService;

    @Test
    public void testChecksumBlobs() throws Exception {

        final Session session = repo.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService.createDatastreamNode(session,
                "/testLLObject/testRepositoryContent",
                "application/octet-stream", new ByteArrayInputStream(
                        "0123456789".getBytes()));

        session.save();

        final Datastream ds = datastreamService.getDatastream(session, "/testLLObject/testRepositoryContent");

        final Collection<FixityResult> fixityResults =
                lowLevelService.getFixity(ds.getNode(), MessageDigest
                        .getInstance("SHA-1"), ds.getContentDigest(), ds
                        .getContentSize());

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016",
                    fixityResult.computedChecksum.toString());
        }
    }

    @Test
    public void testGetBinaryBlobs() throws Exception {
        final Session session = repo.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService.createDatastreamNode(session,
                "/testLLObject/testRepositoryContent",
                "image/tiff", new ByteArrayInputStream(
                        "0123456789987654321012345678900987654321".getBytes()));

        session.save();

        final Datastream ds =
                datastreamService.getDatastream(session, "/testLLObject/testRepositoryContent");

        final Iterator<LowLevelCacheEntry> inputStreamList =
                lowLevelService.getLowLevelCacheEntries(ds.getNode()).iterator();

        int i = 0;
        while (inputStreamList.hasNext()) {
            final InputStream is = inputStreamList.next().getInputStream();

            final String myString = IOUtils.toString(is, "UTF-8");

            assertEquals("0123456789987654321012345678900987654321", myString);

            i++;
        }

        assertNotEquals(0, i);

    }

}
