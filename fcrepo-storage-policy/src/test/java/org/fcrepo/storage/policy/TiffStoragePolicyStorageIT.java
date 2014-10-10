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
import static org.fcrepo.kernel.RdfLexicon.HAS_CONTENT_LOCATION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.rdf.model.Model;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.impl.services.DatastreamServiceImpl;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.impl.services.ObjectServiceImpl;
import org.fcrepo.kernel.impl.services.functions.GetBinaryKey;
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
        objectService = new ObjectServiceImpl();
    }

    @Test
    public void testPolicyDrivenStorage() throws Exception {
        ByteArrayInputStream data;
        final Session session = repo.login();

        final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(session);

        objectService.findOrCreateObject(session, "/testCompositeObject");

        data = new ByteArrayInputStream(
                ("987654321987654321098765432109876543210987654321098765432109876543210987654" +
                        "3210987654321009876543210").getBytes());

        final FedoraBinary binary = datastreamService.getBinary(session,
                "/testCompositeObject/content");

        binary.setContent(data, "application/octet-stream", null, null, pdp);

        data = new ByteArrayInputStream(
                ("87acec17cd9dcd20a716cc2cf67417b71c8a701687acec17cd9dcd20a716cc2cf67417b71c8a70" +
                        "1687acec17cd9dcd20a716cc2cf67417b71c8a701687acec17cd9dcd20a716cc2cf674" +
                        "17b71c8a701687acec17cd9dcd20a716cc2cf67417b71c8a701687acec17cd9dcd20a7" +
                        "16cc2cf67417b71c8a701687acec17cd9dcd20a716cc2cf67417b71c8a7016")
                        .getBytes());
        final FedoraBinary datastream1 = datastreamService.getBinary(session,
                "/testCompositeObject/tiffContent");

        datastream1.setContent(data, "image/tiff", null, null, pdp);

        session.save();

        final Node node = session.getNode("/testCompositeObject/content").getNode(JcrConstants.JCR_CONTENT);

        final BinaryKey key =
            getBinaryKey.apply(node.getProperty(JcrConstants.JCR_DATA));

        logger.info("content key: {}", key);

        final Node tiffNode =
            session.getNode("/testCompositeObject/tiffContent").getNode(JcrConstants.JCR_CONTENT);

        final BinaryKey tiffKey =
            getBinaryKey.apply(tiffNode.getProperty(JcrConstants.JCR_DATA));

        logger.info("tiff key: {}", tiffKey);

        final FedoraBinary normalBinary = datastreamService.asBinary(node);

        Model fixity = normalBinary.getFixity(subjects).asModel();

        assertNotEquals(0, fixity.size());

        String contentLocation = fixity.getProperty(null, HAS_CONTENT_LOCATION).getObject().toString();

        assertThat(contentLocation, containsString(key.toString()));

        final FedoraBinary tiffBinary = datastreamService.asBinary(tiffNode);

        fixity = tiffBinary.getFixity(subjects).asModel();

        assertNotEquals(0, fixity.size());

        contentLocation = fixity.getProperty(null, HAS_CONTENT_LOCATION).getObject().toString();

        assertThat(contentLocation, containsString(tiffKey.toString()));

    }
}
