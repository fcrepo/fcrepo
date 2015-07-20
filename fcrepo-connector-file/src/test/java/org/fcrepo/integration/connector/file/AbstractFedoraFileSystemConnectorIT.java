/**
 * Copyright 2015 DuraSpace, Inc.
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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static com.google.common.collect.Lists.transform;
import static org.fcrepo.kernel.api.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.modeshape.common.util.SecureHash.getHash;
import static org.modeshape.common.util.SecureHash.Algorithm.SHA_1;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import com.hp.hpl.jena.rdf.model.Model;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
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
    protected ContainerService containerService;

    @Inject
    protected BinaryService binaryService;

    /**
     * Gets the path (relative to the filesystem federation) of a directory
     * that's expected to be present.
     *
     * @return string that contains the path to the dir
     */
    protected abstract String testDirPath();

    /**
     * Gets the path (relative to the filesystem federation) of a file
     * that's expected to be present.
     *
     * @return string that contains the path to the file
     */
    protected abstract String testFilePath();

    /**
     * The name (relative path) of the federation to be tested.  This
     * must coincide with the "projections" provided in repository.json.
     *
     * @return string that contains the path to the federation
     */
    protected abstract String federationName();

    /**
     * The filesystem path for the root of the filesystem federation being
     * tested.  This must coincide with the "directoryPath" provided in
     * repository.json (or the system property that's populating the relevant
     * configuration".
     *
     * @return string that contains the path to root
     */
    protected abstract String getFederationRoot();

    private final static String PROP_TEST_DIR1 = "fcrepo.test.connector.file.directory1";
    private final static String PROP_TEST_DIR2 = "fcrepo.test.connector.file.directory2";
    private final static String PROP_EXT_TEST_DIR = "fcrepo.test.connector.file.properties.directory";

    protected String getReadWriteFederationRoot() {
        return getProperty(PROP_TEST_DIR1);
    }

    protected String getReadOnlyFederationRoot() {
        return getProperty(PROP_TEST_DIR2);
    }

    private static final Logger logger =
            getLogger(AbstractFedoraFileSystemConnectorIT.class);

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
            final File f = iterator.next();
            final String path = f.getAbsolutePath();
            try {
                Files.deleteIfExists(Paths.get(path));
            } catch (final IOException e) {
                logger.error("Error in clean up", e);
                fail("Unable to delete work files from a previous test run. File=" + path);
            }
        }
    }

    @Test
    public void testGetFederatedObject() throws RepositoryException {
        final Session session = repo.login();

        final Container object = containerService.findOrCreate(session, testDirPath());
        assertNotNull(object);

        final Node node = object.getNode();
        final NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(2, mixins.length);

        final boolean found = transform(asList(mixins), NodeType::getName).contains(FEDORA_CONTAINER);
        assertTrue("Mixin not found: " + FEDORA_CONTAINER, found);

        session.save();
        session.logout();
    }

    @Test
    public void testGetFederatedDatastream() throws RepositoryException {
        final Session session = repo.login();

        final NonRdfSourceDescription nonRdfSourceDescription
                = binaryService.findOrCreate(session, testFilePath()).getDescription();
        assertNotNull(nonRdfSourceDescription);

        final Node node = nonRdfSourceDescription.getNode();
        final NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(2, mixins.length);

        final boolean found = transform(asList(mixins), NodeType::getName)
                .contains(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        assertTrue("Mixin not found: " + FEDORA_NON_RDF_SOURCE_DESCRIPTION, found);

        session.save();
        session.logout();
    }

    @Test
    public void testGetFederatedContent() throws RepositoryException {
        final Session session = repo.login();

        final Node node = nodeService.find(session, testFilePath() + "/jcr:content").getNode();
        assertNotNull(node);

        final NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(2, mixins.length);

        final boolean found = transform(asList(mixins), NodeType::getName).contains(FEDORA_BINARY);
        assertTrue("Mixin not found: " + FEDORA_BINARY, found);

        final File file = fileForNode();

        assertTrue(file.getAbsolutePath(), file.exists());
        assertEquals(file.length(), node.getProperty(CONTENT_SIZE).getLong());

        session.save();
        session.logout();
    }

    @Test
    public void testFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        final Session session = repo.login();

        checkFixity(binaryService.findOrCreate(session, testFilePath()));

        session.save();
        session.logout();
    }

    @Test
    public void testChangedFileFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        final Session session = repo.login();

        final FedoraBinary binary = binaryService.findOrCreate(session, testFilePath());

        final String originalFixity = checkFixity(binary);

        final File file = fileForNode();
        write(file.toPath(), " ".getBytes("UTF-8"));

        final String newFixity = checkFixity(binary);

        assertNotEquals("Checksum is expected to have changed!", originalFixity, newFixity);

        session.save();
        session.logout();
    }

    private String checkFixity(final FedoraBinary binary)
            throws IOException, NoSuchAlgorithmException, RepositoryException {
        assertNotNull(binary);

        final File file = fileForNode();
        final byte[] hash = getHash(SHA_1, file);

        final URI calculatedChecksum = asURI(SHA_1.toString(), hash);

        final DefaultIdentifierTranslator graphSubjects = new DefaultIdentifierTranslator(repo.login());
        final Model results = binary.getFixity(graphSubjects).asModel();
        assertNotNull(results);

        assertFalse("Found no results!", results.isEmpty());


        assertTrue("Expected to find checksum",
                results.contains(null,
                        HAS_MESSAGE_DIGEST,
                        createResource(calculatedChecksum.toString())));

        return calculatedChecksum.toString();
    }

    protected File fileForNode() {
        return new File(getFederationRoot(), testFilePath().replace(federationName(), ""));
    }

    /**
     * The following is painfully tied to some implementation details
     * but it's critical that we test that the json files are actually written
     * somewhere, so it's the best I can do without further opening up the
     * internals of JsonSidecarExtraPropertiesStore.
     *
     * @param node The node to access for the file reference
     * @return A reference to the nodes property file
     */
    protected File propertyFileForNode(final Node node) {
        try {
            System.out.println("NODE PATH: " + node.getPath());
        } catch (final RepositoryException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return new File(getProperty(PROP_EXT_TEST_DIR),
                testFilePath().replace(federationName(), "") + ".modeshape.json");
    }
}
