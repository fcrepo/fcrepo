/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.integration.kernel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
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
    public void testGetBinaryBlobs() throws Exception {
        final Session session = repo.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService.createDatastream(session,
                "/testLLObject/testRepositoryContent", "image/tiff",
                null, new ByteArrayInputStream(
                        "0123456789987654321012345678900987654321".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testLLObject/testRepositoryContent");

        final Iterator<LowLevelCacheEntry> inputStreamList =
            lowLevelService.getLowLevelCacheEntries(
                    ds.getNode().getNode(JCR_CONTENT)).iterator();

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
