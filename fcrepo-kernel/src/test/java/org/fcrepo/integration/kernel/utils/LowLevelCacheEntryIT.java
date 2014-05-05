/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.kernel.utils;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.kernel.utils.CacheEntry;
import org.fcrepo.kernel.utils.impl.LocalBinaryStoreEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/repo.xml"})
public class LowLevelCacheEntryIT {

    @Inject
    Repository repo;

    @Test
    public void testGetExternalIdentifier() throws Exception {
        final Session session = repo.login();
            final CacheEntry cs = createCacheEntry(session, "some-property");
            assertEquals("info:org.modeshape.jcr.value.binary.TransientBinaryStore", cs
                .getExternalIdentifier().split("@")[0]);
            session.logout();

    }

    @Test
    public void testEquals() throws Exception {

        final Session session = repo.login();
        final CacheEntry cs1 = createCacheEntry(session, "some-property");
        final CacheEntry cs2 = createCacheEntry(session, "some-property");

        assertEquals(cs1, cs2);
        session.logout();
    }

    @Test
    public void testHashCode() throws Exception {

        final Session session = repo.login();
        final CacheEntry cs1 = createCacheEntry(session, "some-property");
        final CacheEntry cs2 = createCacheEntry(session, "some-property");

        assertEquals(cs1.hashCode(), cs2.hashCode());
        session.logout();
    }

    private CacheEntry createCacheEntry(final Session session, final String propertyName) throws Exception {
        final Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream("xyz".getBytes()));
        final Property property = session.getRootNode().setProperty(propertyName, binary);

        return new LocalBinaryStoreEntry(binaryStore(), property);
    }

    private BinaryStore binaryStore() throws Exception {
        return ((JcrRepository) repo).getConfiguration().getBinaryStorage()
                .getBinaryStore();
    }

}
