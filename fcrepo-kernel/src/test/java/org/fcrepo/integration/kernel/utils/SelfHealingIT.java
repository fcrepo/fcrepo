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

package org.fcrepo.integration.kernel.utils;

import static java.util.Collections.singletonMap;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.RepositoryFactory.URL;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.poi.util.IOUtils;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.fcrepo.kernel.utils.impl.CacheStoreEntry;
import org.fcrepo.kernel.utils.infinispan.StoreChunkOutputStream;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class SelfHealingIT {

    protected Logger logger;

    private static Repository repo;

    private DatastreamService datastreamService;

    private ObjectService objectService;

    private LowLevelStorageService lowLevelService;

    @Autowired(required = false)
    StoragePolicyDecisionPoint storagePolicyDecisionPoint;

    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

    @Before
    public void setRepository() throws RepositoryException {

        final URL config =
            this.getClass().getClassLoader().getResource(
                    "test_selfhealing_repository.json");

        final Map<?, ?> params = singletonMap(URL, config.toString());
        repo = new JcrRepositoryFactory().getRepository(params);

        datastreamService = new DatastreamService();
        datastreamService.setRepository(repo);
        datastreamService
                .setStoragePolicyDecisionPoint(storagePolicyDecisionPoint);
        objectService = new ObjectService();
        objectService.setRepository(repo);
        lowLevelService = new LowLevelStorageService();
        lowLevelService.setRepository(repo);
        datastreamService.setLlStoreService(lowLevelService);
    }

    private void tamperWithNode(final Node node) throws Exception {

        logger.info("Tampering with node " + node.toString());
        final Set<LowLevelCacheEntry> binaryBlobs =
            lowLevelService.getLowLevelCacheEntries(node.getNode(JCR_CONTENT));

        final Iterator<LowLevelCacheEntry> it = binaryBlobs.iterator();

        final LowLevelCacheEntry entryToTamper = it.next();
        CacheStore store = ((CacheStoreEntry) entryToTamper).getLowLevelStore();
        if (store instanceof ChainingCacheStore) {
            store =
                ((ChainingCacheStore) store).getStores().keySet().iterator()
                        .next();
            final OutputStream outputStream =
                new StoreChunkOutputStream(store, entryToTamper.getKey()
                        .toString()
                        + "-data");
            IOUtils.copy(new ByteArrayInputStream("qwerty".getBytes()),
                    outputStream);
            outputStream.close();
        } else {
            entryToTamper.storeValue(new ByteArrayInputStream("qwerty"
                    .getBytes()));
        }
        Thread.sleep(1000);

    }

    private Collection<FixityResult>
            getNodeFixity(final Datastream ds) throws NoSuchAlgorithmException,
                                              RepositoryException {

        return datastreamService.getFixity(ds.getNode().getNode(JCR_CONTENT),
                ds.getContentDigest(), ds.getContentSize());

    }

    @Test
    public void testEddiesMagicSelfHealingRepository() throws Exception {
        final Session session = repo.login();

        final String contentA =
            "qn8y34jweuytgopfv3oevo29r7ajrp6r7q21jrxkkciggheh7rqqjbolsq09";
        final String contentB =
            "2e6sxpys67dslongzydxosx6ndze5vbgb6fnj1rr53buk405i1380a868xsb";

        final URI shaA =
            ContentDigest.asURI("SHA-1", encodeHexString(MessageDigest
                    .getInstance("SHA-1").digest(contentA.getBytes())));
        final URI shaB =
            ContentDigest.asURI("SHA-1", encodeHexString(MessageDigest
                    .getInstance("SHA-1").digest(contentB.getBytes())));
        objectService.createObject(session, "/testSelfHealingObject");

        datastreamService.createDatastreamNode(session,
                "/testSelfHealingObject/testDatastreamNode4",
                "application/octet-stream", null, new ByteArrayInputStream(contentA
                        .getBytes()), shaA);
        datastreamService.createDatastreamNode(session,
                "/testSelfHealingObject/testDatastreamNode5",
                "application/octet-stream", null, new ByteArrayInputStream(contentB
                        .getBytes()), shaB);

        session.save();

        Collection<FixityResult> nodeFixity;
        Collection<FixityResult> nodeFixity2;

        Thread.sleep(1000);

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testSelfHealingObject/testDatastreamNode4");

        final Datastream ds2 =
            datastreamService.getDatastream(session,
                    "/testSelfHealingObject/testDatastreamNode5");

        logger.info("checking that our setup succeeded");
        nodeFixity = getNodeFixity(ds);

        nodeFixity2 = getNodeFixity(ds2);

        assertNotEquals(0, nodeFixity.size());

        logger.info("ds1");
        boolean fixityOk = true;

        for (final FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.equals(shaA);
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);

        logger.info("ds2");

        fixityOk = true;

        for (final FixityResult fixityResult : nodeFixity2) {
            fixityOk &= fixityResult.computedChecksum.equals(shaB);
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);

        tamperWithNode(ds.getNode());

        nodeFixity = getNodeFixity(ds);

        fixityOk = true;
        for (final FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.equals(shaA);
        }

        assertFalse("Expected the fixity check to fail.", fixityOk);

        datastreamService.runFixityAndFixProblems(ds);

        nodeFixity = getNodeFixity(ds);

        fixityOk = true;
        for (final FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.equals(shaA);
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);

    }
}
