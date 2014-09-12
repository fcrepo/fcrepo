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
package org.fcrepo.storage.policy;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.impl.services.DatastreamServiceImpl;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.impl.services.ObjectServiceImpl;
import org.fcrepo.kernel.impl.services.functions.GetBinaryKey;
import org.fcrepo.kernel.utils.FixityResult;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.BinaryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>TiffStoragePolicyStorageIT class.</p>
 *
 * @author awoods
 */
public class TiffStoragePolicyStorageIT {

    protected Logger logger;

    static private Repository repo;

    private DatastreamService datastreamService;

    private ObjectService objectService;

    private StoragePolicyDecisionPointImpl pdp;

    GetBinaryKey getBinaryKey = new GetBinaryKey();

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Before
    public void setRepository() throws RepositoryException {

        final URL config =
                this.getClass().getClassLoader().getResource(
                    "repository.json");

        final Map<String, String> params = singletonMap(JcrRepositoryFactory.URL,
                                              config.toString());
        repo = new JcrRepositoryFactory().getRepository(params);

        pdp = new StoragePolicyDecisionPointImpl();
        pdp.add(new MimeTypeStoragePolicy("image/tiff", "tiff-store"));

        datastreamService = new DatastreamServiceImpl();
        datastreamService.setRepository(repo);
        ((DatastreamServiceImpl) datastreamService).setStoragePolicyDecisionPoint(pdp);
        objectService = new ObjectServiceImpl();
        objectService.setRepository(repo);
    }

    @Test
    //@Ignore
    public void testPolicyDrivenStorage() throws Exception {
        ByteArrayInputStream data;
        final Session session = repo.login();

        objectService.createObject(session, "/testCompositeObject");

        data = new ByteArrayInputStream(
                ("987654321987654321098765432109876543210987654321098765432109876543210987654" +
                        "3210987654321009876543210").getBytes());
        datastreamService.createDatastream(session,
                                           "/testCompositeObject/content",
                                           "application/octet-stream",
                                           null,
                                           data);
        data = new ByteArrayInputStream(
                ("87acec17cd9dcd20a716cc2cf67417b71c8a701687acec17cd9dcd20a716cc2cf67417b71c8a70" +
                        "1687acec17cd9dcd20a716cc2cf67417b71c8a701687acec17cd9dcd20a716cc2cf674" +
                        "17b71c8a701687acec17cd9dcd20a716cc2cf67417b71c8a701687acec17cd9dcd20a7" +
                        "16cc2cf67417b71c8a701687acec17cd9dcd20a716cc2cf67417b71c8a7016")
                        .getBytes());
        datastreamService
            .createDatastream(session,
                                  "/testCompositeObject/tiffContent",
                                  "image/tiff",
                                  null,
                                  data);

        session.save();

        final Node node = session.getNode("/testCompositeObject/content");

        final BinaryKey key =
            getBinaryKey.apply(node.getNode(JcrConstants.JCR_CONTENT)
                               .getProperty(JcrConstants.JCR_DATA));

        logger.info("content key: {}", key);

        final Node tiffNode =
            session.getNode("/testCompositeObject/tiffContent");

        final BinaryKey tiffKey =
            getBinaryKey.apply(tiffNode.getNode(JcrConstants.JCR_CONTENT)
                               .getProperty(JcrConstants.JCR_DATA));

        logger.info("tiff key: {}", tiffKey);

        Collection<FixityResult> fixity = datastreamService.getFixity(node.getNode(JcrConstants.JCR_CONTENT), null, 0L);

        assertNotEquals(0, fixity.size());

        FixityResult e = fixity.iterator().next();

        assertThat(e.getStoreIdentifier(), containsString(key.toString()));


        fixity = datastreamService.getFixity(tiffNode.getNode(JcrConstants.JCR_CONTENT), null, 0L);

        assertNotEquals(0, fixity.size());

        e = fixity.iterator().next();

        assertThat(e.getStoreIdentifier(), containsString(tiffKey.toString()));
    }
}
