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

package org.fcrepo.integration.connector.file;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.junit.AfterClass;
import org.junit.Assert;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.services.NodeService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.util.SecureHash;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;


/**
 * @author Andrew Woods
 *         Date: 2/3/14
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

    @BeforeClass
    public static void beforeClass() {
        // Note: This property is used in the repository.json
        System.setProperty(PROP_TEST_DIR, new File("target/test-classes").getAbsolutePath());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(PROP_TEST_DIR);
    }

    @Test
    public void testGetFederatedObject() throws RepositoryException {
        Session session = repo.login();

        final FedoraObject object = objectService.getObject(session, testFile);
        Assert.assertNotNull(object);

        final Node node = object.getNode();
        final NodeType[] mixins = node.getMixinNodeTypes();
        Assert.assertEquals(2, mixins.length);

        boolean found = false;
        for (NodeType nodeType : mixins) {
            if (nodeType.getName().equals(FEDORA_DATASTREAM)) {
                found = true;
            }
        }
        Assert.assertTrue("Mixin not found: " + FEDORA_DATASTREAM, found);

        session.save();
        session.logout();
    }

    @Test
    public void testGetFederatedContent() throws RepositoryException {
        Session session = repo.login();

        final Node node = datastreamService.getDatastreamNode(session, testFile + "/jcr:content");
        Assert.assertNotNull(node);

        final NodeType[] mixins = node.getMixinNodeTypes();
        Assert.assertEquals(2, mixins.length);

        boolean found = false;
        for (NodeType nodeType : mixins) {
            if (nodeType.getName().equals(FEDORA_BINARY)) {
                found = true;
            }
        }
        Assert.assertTrue("Mixin not found: " + FEDORA_BINARY, found);

        final Property size = node.getProperty(FedoraJcrTypes.CONTENT_SIZE);

        final File file = new File(testFile.replace("/federated", "target/test-classes"));
        Assert.assertTrue(file.getAbsolutePath(), file.exists());
        Assert.assertEquals(file.length(), size.getLong());

        session.save();
        session.logout();
    }

    @Test
    public void testWriteProperty() throws RepositoryException {
        Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, testFile);
        Assert.assertNotNull(object);

        final String sparql = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "INSERT DATA { " +
                "<info:fedora" + testFile + "> " +
                "fedora:name " +
                "'some-test-name' }";

        // Write the properties
        final GraphSubjects graphSubjects = new DefaultGraphSubjects(session);
        object.updatePropertiesDataset(graphSubjects, sparql);

        // Verify
        final Property property = object.getNode().getProperty("fedora:name");
        Assert.assertNotNull(property);
        Assert.assertEquals("some-test-name", property.getValues()[0].toString());

        session.save();
        session.logout();
    }

    @Test
    public void testFixity() throws RepositoryException, IOException, NoSuchAlgorithmException {
        Session session = repo.login();

        final Node node = datastreamService.getDatastreamNode(session, testFile + "/jcr:content");
        Assert.assertNotNull(node);

        final File file = new File(System.getProperty(PROP_TEST_DIR), testFile.replace("federated", ""));
        final byte[] hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1, file);

        final URI calculatedChecksum = ContentDigest.asURI(SecureHash.Algorithm.SHA_1.toString(), hash);

        final Collection<FixityResult> results = datastreamService.getFixity(node, calculatedChecksum, file.length());
        Assert.assertNotNull(results);

        Assert.assertTrue(!results.isEmpty());

        final Iterator<FixityResult> resultIterator = results.iterator();
        while (resultIterator.hasNext()) {
            final FixityResult result = resultIterator.next();
            Assert.assertTrue(result.isSuccess());
        }

        session.save();
        session.logout();
    }

}
