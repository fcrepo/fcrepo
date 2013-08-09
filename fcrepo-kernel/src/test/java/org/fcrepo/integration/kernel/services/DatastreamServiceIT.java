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

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.tika.io.IOUtils;
import org.fcrepo.integration.kernel.AbstractIT;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.FixityResult;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.springframework.test.context.ContextConfiguration;


@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamServiceIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    @Test
    public void testCreateDatastreamNode() throws Exception {
        Session session = repository.login();
        datastreamService
            .createDatastreamNode(session,
                                  "/testDatastreamNode",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();
        session = repository.login();

        assertTrue(session.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("asdf", session.getNode("/testDatastreamNode")
                     .getNode(JCR_CONTENT).getProperty(JCR_DATA).getString());
        session.logout();
    }

    @Test
    public void testGetDatastreamContentInputStream() throws Exception {
        Session session = repository.login();
        final InputStream is = new ByteArrayInputStream("asdf".getBytes());
        objectService.createObject(session, "/testDatastreamServiceObject");
        datastreamService.createDatastreamNode(session,
                                               "/testDatastreamServiceObject/" +
                                               "testDatastreamNode",
                                               "application/octet-stream", is);

        session.save();
        session.logout();
        session = repository.login();
        final Datastream ds =
            datastreamService.getDatastream(session,
                                            "/testDatastreamServiceObject/" +
                                            "testDatastreamNode");
        assertEquals("asdf", IOUtils.toString(ds.getContent(), "UTF-8"));
        session.logout();
    }

    @Test
    public void testChecksumBlobs() throws Exception {

        final Session session = repository.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testLLObject/testRepositoryContent",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("0123456789"
                                                           .getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                                            "/testLLObject/" +
                                            "testRepositoryContent");

        final Collection<FixityResult> fixityResults =
            datastreamService.getFixity(ds.getNode()
                                        .getNode(JcrConstants.JCR_CONTENT),
                                        ds.getContentDigest(),
                                        ds.getContentSize());

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            Assert.assertEquals("urn:" +
                                "sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016",
                                fixityResult.computedChecksum.toString());
        }
    }
}
