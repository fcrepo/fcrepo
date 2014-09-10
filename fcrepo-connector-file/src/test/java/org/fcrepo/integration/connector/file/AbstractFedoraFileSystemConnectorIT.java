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
package org.fcrepo.integration.connector.file;

import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.fcrepo.jcr.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.kernel.utils.ContentDigest.asURI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.modeshape.common.util.SecureHash.getHash;
import static org.modeshape.common.util.SecureHash.Algorithm.SHA_1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.FixityResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * An abstract suite of tests that should work against any configuration
 * of a FedoraFileSystemFederation.  Tests that only work on certain
 * configurations (ie, require read/write capabilities) should be implemented
 * in subclasses.
 *
 * @author Andrew Woods
 * @since 2014-2-3
 */
@ContextConfiguration({"/spring-test/repo.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractFedoraFileSystemConnectorIT {

    @Inject
    protected Repository repo;

    @Inject
    protected NodeService nodeService;

    @Inject
    protected ObjectService objectService;

    @Inject
    protected DatastreamService datastreamService;

    /**
     * Gets the path (relative to the filesystem federation) of a directory
     * that's expected to be present.
     */
    protected abstract String testDirPath();

    /**
     * Gets the path (relative to the filesystem federation) of a file
     * that's expected to be present.
     */
    protected abstract String testFilePath();

    /**
     * The name (relative path) of the federation to be tested.  This
     * must coincide with the "projections" provided in repository.json.
     */
    protected abstract String federationName();

    /**
     * The filesystem path for the root of the filesystem federation being
     * tested.  This must coincide with the "directoryPath" provided in
     * repository.json (or the system property that's populating the relevant
     * configuration".
     */
    protected abstract String getFederationRoot();

    private final static String PROP_TEST_DIR1 = "fcrepo.test.dir1";
    private final static String PROP_TEST_DIR2 = "fcrepo.test.dir2";
    private final static String PROP_EXT_TEST_DIR = "fcrepo.test.properties.dir";

    protected String getReadWriteFederationRoot() {
        return getProperty(PROP_TEST_DIR1);
    }

    protected String getReadOnlyFederationRoot() {
        return getProperty(PROP_TEST_DIR2);
    }

    /**
     * Sets a system property and ensures artifacts from previous tests are
     * cleaned up.
     */
    @BeforeClass
    public static void setSystemPropertiesAndCleanUp() {

        // Instead of creating dummy files over which to federate,
        // we configure the FedoraFileSystemFederation instances to
        // point to paths within the "target" directory.
        final File testDir1 = new File("target/test-classes/config/testing");
        cleanUpJsonFilesFiles(testDir1);
        setProperty(PROP_TEST_DIR1, testDir1.getAbsolutePath());

        final File testDir2 = new File("target/test-classes/spring-test");
        cleanUpJsonFilesFiles(testDir2);
        setProperty(PROP_TEST_DIR2, testDir2.getAbsolutePath());

        final File testPropertiesDir = new File("target/test-classes-properties");
        if (testPropertiesDir.exists()) {
            cleanUpJsonFilesFiles(testPropertiesDir);
        } else {
            testPropertiesDir.mkdir();
        }
        setProperty(PROP_EXT_TEST_DIR, testPropertiesDir.getAbsolutePath());
    }

    @AfterClass
    public static void unsetSystemPropertiesAndCleanUp() {
        clearProperty(PROP_TEST_DIR1);
        clearProperty(PROP_TEST_DIR2);
        clearProperty(PROP_EXT_TEST_DIR);
    }

    protected static void cleanUpJsonFilesFiles(final File directory) {
        final WildcardFileFilter filter = new WildcardFileFilter("*.modeshape.json");
        final Collection<File> files = FileUtils.listFiles(directory, filter, TrueFileFilter.INSTANCE);
        final Iterator<File> iterator = files.iterator();

        // Clean up files persisted in previous runs
        while (iterator.hasNext()) {
            if (!iterator.next().delete()) {
                fail("Unable to delete work files from a previous test run");
            }
        }
    }

    @Test
    public void testGetFederatedObject() throws RepositoryException {
        final Session session = repo.login();

        final FedoraObject object = objectService.getObject(session, testDirPath());
        assertNotNull(object);

        final Node node = object.getNode();
        final NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(2, mixins.length);

        boolean found = false;
        for (final NodeType nodeType : mixins) {
            if (nodeType.getName().equals(FEDORA_OBJECT)) {
                found = true;
            }
        }
        assertTrue("Mixin not found: " + FEDORA_OBJECT, found);

        session.save();
        session.logout();
    }

    @Test
    public void testGetFederatedDatastream() throws RepositoryException {
        final Session session = repo.login();

        final Datastream datastream = datastreamService.getDatastream(session, testFilePath());
        assertNotNull(datastream);

        final Node node = datastream.getNode();
        final NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(2, mixins.length);

        boolean found = false;
        for (final NodeType nodeType : mixins) {
            if (nodeType.getName().equals(FEDORA_DATASTREAM)) {
                found = true;
            }
        }
        assertTrue("Mixin not found: " + FEDORA_DATASTREAM, found);

        session.save();
        session.logout();
    }

    @Test
    public void testGetFederatedContent() throws RepositoryException {
        final Session session = repo.login();

        final Node node = nodeService.getObject(session, testFilePath() + "/jcr:content").getNode();
        assertNotNull(node);

        final NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(2, mixins.length);

        boolean found = false;
        for (final NodeType nodeType : mixins) {
            if (nodeType.getName().equals(FEDORA_BINARY)) {
                found = true;
            }
        }
        assertTrue("Mixin not found: " + FEDORA_BINARY, found);

        final Property size = node.getProperty(CONTENT_SIZE);

        final File file = fileForNode(node);
        assertTrue(file.getAbsolutePath(), file.exists());
        assertEquals(file.length(), size.getLong());

        session.save();
        session.logout();
    }

    @Test
    public void testFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        final Session session = repo.login();

        checkFixity(nodeService.getObject(session, testFilePath() + "/jcr:content").getNode());

        session.save();
        session.logout();
    }

    @Test
    public void testChangedFileFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        final Session session = repo.login();

        final Node node = nodeService.getObject(session, testFilePath() + "/jcr:content").getNode();

        final String originalFixity = checkFixity(node);

        final File file = fileForNode(node);
        appendToFile(file, " ");

        final String newFixity = checkFixity(node);

        assertNotEquals("Checksum is expected to have changed!", originalFixity, newFixity);

        session.save();
        session.logout();
    }

    private void appendToFile(final File f, final String data) throws IOException {
        final FileOutputStream fos = new FileOutputStream(f, true);
        try {
            fos.write(data.getBytes("UTF-8"));
        } finally {
            fos.close();
        }
    }

    private String checkFixity(final Node node) throws IOException, NoSuchAlgorithmException, RepositoryException {
        assertNotNull(node);

        final File file = fileForNode(node);
        final byte[] hash = getHash(SHA_1, file);

        final URI calculatedChecksum = asURI(SHA_1.toString(), hash);

        final Collection<FixityResult> results = datastreamService.getFixity(node, calculatedChecksum, file.length());
        assertNotNull(results);

        assertFalse("Found no results!", results.isEmpty());

        final Iterator<FixityResult> resultIterator = results.iterator();
        while (resultIterator.hasNext()) {
            final FixityResult result = resultIterator.next();
            assertTrue(result.isSuccess());
        }
        return calculatedChecksum.toString();
    }

    protected File fileForNode(final Node node) {
        return new File(getFederationRoot(), testFilePath().replace(federationName(), ""));
    }

    /**
     * The following is painfully tied to some implementation details
     * but it's critical that we test that the json files are actually written
     * somewhere, so it's the best I can do without further opening up the
     * internals of JsonSidecarExtraPropertiesStore.
     */
    protected File propertyFileForNode(final Node node) {
        try {
            System.out.println("NODE PATH: " + node.getPath());
        } catch (RepositoryException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return new File(getProperty(PROP_EXT_TEST_DIR),
                testFilePath().replace(federationName(), "") + ".modeshape.json");
    }
}
