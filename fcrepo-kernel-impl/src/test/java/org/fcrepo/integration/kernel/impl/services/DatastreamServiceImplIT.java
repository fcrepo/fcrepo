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
package org.fcrepo.integration.kernel.impl.services;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.PREMIS_FILE_NAME;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.tika.io.IOUtils;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.FixityResult;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>DatastreamServiceImplIT class.</p>
 *
 * @author ksclarke
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    @Test
    public void testCreateDatastreamNode() throws Exception {
        Session session = repository.login();
        datastreamService.createDatastream(session, "/testDatastreamNode",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));
        session.save();
        session.logout();
        session = repository.login();

        assertTrue(session.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("asdf", session.getNode("/testDatastreamNode").getNode(
                JCR_CONTENT).getProperty(JCR_DATA).getString());
        session.logout();
    }

    @Test
    public void testCreateDatastreamNodeWithfilename() throws Exception {
        Session session = repository.login();
        datastreamService.createDatastream(session, "/testDatastreamNode",
                                           "application/octet-stream",
                                           "xyz.jpg",
                                           new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();
        session = repository.login();

        assertTrue(session.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("xyz.jpg", session.getNode("/testDatastreamNode").getNode(JCR_CONTENT)
                .getProperty(PREMIS_FILE_NAME).getString());
        session.logout();
    }

    @Test
    public void testGetDatastreamContentInputStream() throws Exception {
        Session session = repository.login();
        final InputStream is = new ByteArrayInputStream("asdf".getBytes());
        objectService.createObject(session, "/testDatastreamServiceObject");
        datastreamService.createDatastream(session,
                "/testDatastreamServiceObject/" + "testDatastreamNode",
                "application/octet-stream", null, is);

        session.save();
        session.logout();
        session = repository.login();
        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testDatastreamServiceObject/" + "testDatastreamNode");
        assertEquals("asdf", IOUtils.toString(ds.getContent(), "UTF-8"));
        session.logout();
    }

    @Test
    public void testChecksumBlobs() throws Exception {

        final Session session = repository.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService.createDatastream(session,
                "/testLLObject/testRepositoryContent",
                "application/octet-stream", null, new ByteArrayInputStream(
                        "01234567890123456789012345678901234567890123456789".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session, "/testLLObject/"
                    + "testRepositoryContent");

        final Collection<FixityResult> fixityResults =
            datastreamService.getFixity(ds.getNode().getNode(JCR_CONTENT), ds
                    .getContentDigest(), ds.getContentSize());

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            Assert.assertEquals("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf",
                    fixityResult.getComputedChecksum().toString());
        }
    }

    @Test
    public void testChecksumBlobsForInMemoryValues() throws Exception {

        final Session session = repository.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService.createDatastream(session,
                                                  "/testLLObject/testMemoryContent",
                                                  "application/octet-stream",
                                                  null,
                                                  new ByteArrayInputStream("0123456789".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session, "/testLLObject/"
                                                         + "testMemoryContent");

        final Collection<FixityResult> fixityResults =
            datastreamService.getFixity(ds.getNode().getNode(JCR_CONTENT), ds.getContentDigest(), ds.getContentSize());

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            Assert.assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016",
                    fixityResult.getComputedChecksum().toString());
        }
    }

    @Test
    public void testChecksumBlobsForValuesWithoutChecksums() throws Exception {

        final Session session = repository.login();
        final javax.jcr.ValueFactory factory = session.getValueFactory();
        final FedoraObject object = objectService.createObject(session, "/testLLObject");

        final Node testRandomContentNode = object.getNode().addNode("testRandomContent", NT_FILE);
        testRandomContentNode.addMixin(FEDORA_DATASTREAM);
        final Node testRandomContent = testRandomContentNode.addNode(JCR_CONTENT, NT_RESOURCE);
        testRandomContent.setProperty(JCR_DATA,
                                      factory.createBinary(new ByteArrayInputStream("0123456789".getBytes())));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session, "/testLLObject/testRandomContent");

        final Collection<FixityResult> fixityResults =
            datastreamService.getFixity(ds.getNode().getNode(JCR_CONTENT), ds.getContentDigest(), ds.getContentSize());

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            assertFalse(fixityResult.isSuccess());
            assertTrue(fixityResult.getStatus().contains(
                    FixityResult.FixityState.MISSING_STORED_FIXITY));
            assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016",
                    fixityResult.getComputedChecksum().toString());
        }
    }

    @Test(expected = FedoraInvalidNamespaceException.class)
    public void testCreateDatastreamNodeWithInvalidNS() throws Exception {
        Session session = repository.login();
        datastreamService.createDatastream(session, "/bad_ns:testDatastreamNode",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));
        session.save();
        session.logout();
        session = repository.login();

        assertTrue(session.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("asdf", session.getNode("/testDatastreamNode").getNode(
                JCR_CONTENT).getProperty(JCR_DATA).getString());
        session.logout();
    }
}
