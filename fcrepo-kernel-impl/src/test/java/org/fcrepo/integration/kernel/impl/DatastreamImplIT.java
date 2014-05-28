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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.ContentDigest;
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
        Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService.createDatastream(session,
                "/testDatastreamObject/testDatastreamNode1",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));
        session.save();
        session.logout();
        session = repo.login();
        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testDatastreamObject/testDatastreamNode1");
        assertNotNull("Couldn't find created date on datastream!", ds
                .getCreatedDate());
    }

    @Test
    public void testDatastreamContent() throws IOException,
                                       RepositoryException,
                                       InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService.createDatastream(session,
                "/testDatastreamObject/testDatastreamNode1",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testDatastreamObject/testDatastreamNode1");
        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);

    }

    @Test
    public void testDatastreamContentDigestAndLength() throws IOException,
                                                      RepositoryException,
                                                      InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService.createDatastream(session,
                "/testDatastreamObject/testDatastreamNode2",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testDatastreamObject/testDatastreamNode2");
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
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService.createDatastream(session,
                "/testDatastreamObject/testDatastreamNode3",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testDatastreamObject/testDatastreamNode3");

        ds.setContent(new ByteArrayInputStream("0123456789".getBytes()));

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
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService.createDatastream(session,
                "/testDatastreamObject/testDatastreamNode4",
                "application/octet-stream", null, new ByteArrayInputStream("asdf"
                        .getBytes()), ContentDigest.asURI("SHA-1",
                        "3da541559918a808c2402bba5012f6c60b27661c"));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                    "/testDatastreamObject/testDatastreamNode4");
        assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                .getContentDigest().toString());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);
    }

    @Test
    public void testDatastreamFileName() throws RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService.createDatastream(session,
                                                  "/testDatastreamObject/testDatastreamNode5",
                                                  "application/octet-stream",
                                                  "xyz.jpg",
                                                  new ByteArrayInputStream("asdf".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService.getDatastream(session,
                                               "/testDatastreamObject/testDatastreamNode5");
        final String filename = ds.getFilename();

        assertEquals("xyz.jpg", filename);

    }
}
