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
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.impl.DefaultIdentifierTranslator;
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
 * @author Andrew Woods
 * @since 2014-2-3
 */
@ContextConfiguration({"/spring-test/repo.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FedoraFileSystemConnectorIT {

    @Inject
    private Repository repo;

    @Inject
    private NodeService nodeService;

    @Inject
    private ObjectService objectService;

    @Inject
    private DatastreamService datastreamService;

    private final String testFile = "/federated/config/testing/repository.json";

    private final static String PROP_TEST_DIR = "fcrepo.test.dir";

    /**
     * Sets a system property and ensures artifacts from previous tests are
     * cleaned up.
     */
    @BeforeClass
    public static void beforeClass() {
        final File testDir = new File("target/test-classes");
        final WildcardFileFilter filter = new WildcardFileFilter("*.modeshape.json");
        final Collection<File> files = FileUtils.listFiles(testDir, filter, TrueFileFilter.INSTANCE);
        final Iterator<File> iterator = files.iterator();

        // Clean up files persisted in previous runs
        while (iterator.hasNext()) {
            if (!iterator.next().delete()) {
                fail("Unable to delete work files from a previous test run");
            }
        }

        // Note: This property is used in the repository.json
        setProperty(PROP_TEST_DIR, testDir.getAbsolutePath());
    }

    @AfterClass
    public static void afterClass() {
        clearProperty(PROP_TEST_DIR);
    }

    @Test
    public void testGetFederatedObject() throws RepositoryException {
        final Session session = repo.login();

        final FedoraObject object = objectService.getObject(session, testFile);
        assertNotNull(object);

        final Node node = object.getNode();
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

        final Node node = datastreamService.getDatastreamNode(session, testFile + "/jcr:content");
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

        final File file = new File(testFile.replace("/federated", "target/test-classes"));
        assertTrue(file.getAbsolutePath(), file.exists());
        assertEquals(file.length(), size.getLong());

        session.save();
        session.logout();
    }

    @Test
    public void testWriteProperty() throws RepositoryException {
        final Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, testFile);
        assertNotNull(object);

        final String sparql = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "INSERT DATA { " +
                "<info:fedora" + testFile + "> " +
                "fedora:name " +
                "'some-test-name' }";


        // Write the properties
        object.updatePropertiesDataset(new DefaultIdentifierTranslator(), sparql);

        // Verify
        final Property property = object.getNode().getProperty("fedora:name");
        assertNotNull(property);
        assertEquals("some-test-name", property.getValues()[0].toString());

        session.save();
        session.logout();
    }

    @Test
    public void testRemoveProperty() throws RepositoryException {
        final Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, testFile);
        assertNotNull(object);

        final String sparql = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "INSERT DATA { " +
                "<info:fedora" + testFile + "> " +
                "fedora:remove " +
                "'some-property-to-remove' }";

        // Write the properties
        final IdentifierTranslator graphSubjects = new DefaultIdentifierTranslator();
        object.updatePropertiesDataset(graphSubjects, sparql);

        // Verify property exists
        final Property property = object.getNode().getProperty("fedora:remove");
        assertNotNull(property);
        assertEquals("some-property-to-remove", property.getValues()[0].getString());

        final String sparqlRemove = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "DELETE {" +
                "  <info:fedora" + testFile + "> fedora:remove ?s " +
                "} WHERE { " +
                "  <info:fedora" + testFile + "> fedora:remove ?s" +
                "}";

        // Remove the properties
        final IdentifierTranslator graphSubjectsRemove = new DefaultIdentifierTranslator();
        object.updatePropertiesDataset(graphSubjectsRemove, sparqlRemove);

        // Persist the object (although the propery will be removed from memory without this.)
        session.save();

        // Verify
        boolean thrown = false;
        try {
            object.getNode().getProperty("fedora:remove");
        } catch (final PathNotFoundException e) {
            thrown = true;
        }
        assertTrue("Exception expected - property should be missing", thrown);

        session.logout();
    }

    @Test
    public void testFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        final Session session = repo.login();

        checkFixity(datastreamService.getDatastreamNode(session, testFile + "/jcr:content"));

        session.save();
        session.logout();
    }

    @Test
    public void testChangedFileFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        final Session session = repo.login();

        final Node node = datastreamService.getDatastreamNode(session, testFile + "/jcr:content");

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

    private File fileForNode(final Node node) {
        return new File(getProperty(PROP_TEST_DIR), testFile.replace("federated", ""));
    }
}
