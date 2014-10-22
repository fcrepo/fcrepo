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

import static org.fcrepo.jcr.FedoraJcrTypes.PREMIS_FILE_NAME;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.tika.io.IOUtils;
import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>DatastreamServiceImplIT class.</p>
 *
 * @author ksclarke
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class BinaryServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    BinaryService binaryService;

    @Test
    public void testCreateDatastreamNode() throws Exception {
        Session session = repository.login();

        binaryService.findOrCreateBinary(session, "/testDatastreamNode").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

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
        binaryService.findOrCreateBinary(session, "/testDatastreamNode").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                "xyz.jpg",
                null
        );

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
        objectService.findOrCreateObject(session, "/testDatastreamServiceObject");

        binaryService.findOrCreateBinary(session, "/testDatastreamServiceObject/" + "testDatastreamNode")
                .setContent(
                        is,
                        "application/octet-stream",
                        null,
                        null,
                        null
                );

        session.save();
        session.logout();
        session = repository.login();
        final FedoraBinary binary =
                binaryService.findOrCreateBinary(session,
                    "/testDatastreamServiceObject/" + "testDatastreamNode");
        assertEquals("asdf", IOUtils.toString(binary.getContent(), "UTF-8"));
        session.logout();
    }


}
