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
package org.fcrepo.integration.kernel.impl;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.jgroups.util.Util;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>DatastreamImplIT class.</p>
 *
 * @author ksclarke
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    DatastreamService datastreamService;

    @Inject
    ObjectService objectService;

    @Test
    public void testCreatedDate() throws RepositoryException, InvalidChecksumException {
        final String objPid = getTestObjIdentifier();
        final String dsid = getTestDsIdentifier();

        Session session = repo.login();
        objectService.createObject(session, "/" + objPid);
        datastreamService.createDatastream(session,
                "/" + objPid + "/" + dsid,
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));
        session.save();
        session.logout();
        session = repo.login();
        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/" + objPid + "/" + dsid);
        assertNotNull("Couldn't find created date on datastream!", ds
                .getCreatedDate());
    }

    @Test
    public void testDatastreamContent() throws IOException,
                                       RepositoryException,
                                       InvalidChecksumException {
        final String objPid = getTestObjIdentifier();
        final String dsid = getTestDsIdentifier();

        final Session session = repo.login();
        objectService.createObject(session, "/" + objPid);
        datastreamService.createDatastream(session,
                "/" + objPid + "/" + dsid,
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/" + objPid + "/" + dsid);
        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);

    }

    @Test
    public void testDatastreamContentDigestAndLength() throws IOException,
                                                      RepositoryException,
                                                      InvalidChecksumException {
        final String objPid = getTestObjIdentifier();
        final String dsid = getTestDsIdentifier();

        final Session session = repo.login();
        objectService.createObject(session, "/" + objPid);
        datastreamService.createDatastream(session,
                "/" + objPid + "/" + dsid,
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/" + objPid + "/" + dsid);
        assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                .getContentDigest().toString());
        assertEquals(4L, ds.getContentSize());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);
    }

    @Test
    public void
            testModifyDatastreamContentDigestAndLength() throws IOException,
                                                        RepositoryException,
                                                        InvalidChecksumException {
        final String objPid = getTestObjIdentifier();
        final String dsid = getTestDsIdentifier();

        final Session session = repo.login();
        objectService.createObject(session, "/" + objPid);
        datastreamService.createDatastream(session,
                "/" + objPid + "/" + dsid,
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));

        session.save();

        final Datastream ds = datastreamService.getDatastream(session, "/" + objPid + "/" + dsid);

        ds.setContent(new ByteArrayInputStream("0123456789".getBytes()), null, null, null, null);

        assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016", ds
                .getContentDigest().toString());
        assertEquals(10L, ds.getContentSize());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("0123456789", contentString);
    }

    @Test
    public void testDatastreamContentWithChecksum() throws IOException,
                                                   RepositoryException,
                                                   InvalidChecksumException {
        final String objPid = getTestObjIdentifier();
        final String dsid = getTestDsIdentifier();

        final Session session = repo.login();
        objectService.createObject(session, "/" + objPid);
        datastreamService.createDatastream(session,
                "/" + objPid + "/" + dsid,
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()), ContentDigest.asURI("SHA-1",
                        "3da541559918a808c2402bba5012f6c60b27661c"));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/" + objPid + "/" + dsid);
        assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                .getContentDigest().toString());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);
    }

    @Test
    public void testDatastreamFileName() throws RepositoryException, InvalidChecksumException {
        final String objPid = getTestObjIdentifier();
        final String dsid = getTestDsIdentifier();

        final Session session = repo.login();
        objectService.createObject(session, "/" + objPid);
        datastreamService.createDatastream(session,
                "/" + objPid + "/" + dsid,
                "application/octet-stream",
                "xyz.jpg",
                new ByteArrayInputStream("asdf".getBytes()));

        session.save();

        final Datastream ds =
                datastreamService.getDatastream(session,
                        "/" + objPid + "/" + dsid);
        final String filename = ds.getFilename();

        assertEquals("xyz.jpg", filename);

    }

    @Test
    public void testChecksumBlobs() throws Exception {

        final Session session = repo.login();
        objectService.createObject(session, "/testLLObject");
        datastreamService.createDatastream(session,
                "/testLLObject/testRepositoryContent",
                "application/octet-stream", null, new ByteArrayInputStream(
                        "01234567890123456789012345678901234567890123456789".getBytes()));

        session.save();

        final Datastream ds =
                datastreamService.getDatastream(session, "/testLLObject/"
                        + "testRepositoryContent");


        final String algorithm = ContentDigest.getAlgorithm(ds.getContentDigest());
        final Collection<FixityResult> fixityResults = ds.getFixity(repo, algorithm);

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            Assert.assertEquals("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf",
                    fixityResult.getComputedChecksum().toString());
        }
    }

    @Test
    public void testChecksumBlobsForInMemoryValues() throws Exception {

        final Session session = repo.login();
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


        final String algorithm = ContentDigest.getAlgorithm(ds.getContentDigest());
        final Collection<FixityResult> fixityResults = ds.getFixity(repo, algorithm);
        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            Assert.assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016",
                    fixityResult.getComputedChecksum().toString());
        }
    }

    @Test
    public void testChecksumBlobsForValuesWithoutChecksums() throws Exception {

        final Session session = repo.login();
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

        final String algorithm = ContentDigest.getAlgorithm(ds.getContentDigest());
        final Collection<FixityResult> fixityResults = ds.getFixity(repo, algorithm);

        assertNotEquals(0, fixityResults.size());

        for (final FixityResult fixityResult : fixityResults) {
            assertFalse(fixityResult.matches(ds.getContentSize(), ds.getContentDigest()));
            Util.assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016",
                    fixityResult.getComputedChecksum().toString());
        }
    }
}
