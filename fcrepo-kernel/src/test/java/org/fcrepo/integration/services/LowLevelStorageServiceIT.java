/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.integration.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.JcrConstants;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date Mar 20, 2013
 */
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


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetBinaryBlobs() throws Exception {
        final Session session = repo.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testLLObject/testRepositoryContent",
                                  "image/tiff",
                                  new ByteArrayInputStream("0123456789987654321012345678900987654321".getBytes())
                                  );

        session.save();

        final Datastream ds =
            datastreamService
            .getDatastream(session, "/testLLObject/testRepositoryContent");

        final Iterator<LowLevelCacheEntry> inputStreamList =
            lowLevelService
            .getLowLevelCacheEntries(ds.getNode()
                                     .getNode(JcrConstants.JCR_CONTENT))
            .iterator();

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
