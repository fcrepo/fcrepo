/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date Mar 20, 2013
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    DatastreamService datastreamService;

    @Inject
    ObjectService objectService;


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testCreatedDate() throws RepositoryException, IOException,
                                         InvalidChecksumException {
        Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testDatastreamObject/testDatastreamNode1",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();
        session = repo.login();
        final Datastream ds =
            datastreamService
            .getDatastream(session,
                           "/testDatastreamObject/testDatastreamNode1");
        assertNotNull("Couldn't find created date on datastream!", ds
                      .getCreatedDate());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testDatastreamContent()
        throws IOException, RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testDatastreamObject/testDatastreamNode1",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("asdf".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService
            .getDatastream(session,
                           "/testDatastreamObject/testDatastreamNode1");
        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testDatastreamContentDigestAndLength()
        throws IOException, RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testDatastreamObject/testDatastreamNode2",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("asdf".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService
            .getDatastream(session,
                           "/testDatastreamObject/testDatastreamNode2");
        assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                     .getContentDigest().toString());
        assertEquals(4L, ds.getContentSize());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testModifyDatastreamContentDigestAndLength()
        throws IOException, RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testDatastreamObject/testDatastreamNode3",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("asdf".getBytes()));

        session.save();

        final Datastream ds =
            datastreamService
            .getDatastream(session,
                           "/testDatastreamObject/testDatastreamNode3");


        ds.setContent(new ByteArrayInputStream("0123456789".getBytes()));

        assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016", ds
                     .getContentDigest().toString());
        assertEquals(10L, ds.getContentSize());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("0123456789", contentString);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testDatastreamContentWithChecksum()
        throws IOException, RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        objectService.createObject(session, "/testDatastreamObject");
        datastreamService
            .createDatastreamNode(session,
                                  "/testDatastreamObject/testDatastreamNode4",
                                  "application/octet-stream",
                                  new ByteArrayInputStream("asdf".getBytes()),
                                  "SHA-1",
                                  "3da541559918a808c2402bba5012f6c60b27661c");

        session.save();

        final Datastream ds =
            datastreamService
            .getDatastream(session,
                           "/testDatastreamObject/testDatastreamNode4");
        assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                     .getContentDigest().toString());

        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);
    }

}
