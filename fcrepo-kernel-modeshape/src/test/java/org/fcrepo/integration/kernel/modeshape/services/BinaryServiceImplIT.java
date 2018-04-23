/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.modeshape.services;

import static org.fcrepo.kernel.api.FedoraTypes.FILENAME;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.Session;

import org.apache.tika.io.IOUtils;
import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
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
    private FedoraRepository repository;

    @Inject
    ContainerService containerService;

    @Inject
    BinaryService binaryService;

    @Test
    public void testCreateDatastreamNode() throws Exception {
        FedoraSession session = repository.login();
        Session jcrSession = getJcrSession(session);

        binaryService.findOrCreate(session, "/testDatastreamNode").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.commit();
        session.expire();

        session = repository.login();
        jcrSession = getJcrSession(session);

        assertTrue(jcrSession.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("asdf", jcrSession.getNode("/testDatastreamNode")
                .getProperty(JCR_DATA).getString());
        session.expire();
    }

    @Test
    public void testCreateDatastreamNodeWithfilename() throws Exception {
        FedoraSession session = repository.login();
        Session jcrSession = getJcrSession(session);

        binaryService.findOrCreate(session, "/testDatastreamNode").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                "xyz.jpg",
                null
        );

        session.commit();
        session.expire();

        session = repository.login();
        jcrSession = getJcrSession(session);

        assertTrue(jcrSession.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("xyz.jpg", jcrSession.getNode("/testDatastreamNode/fedora:description")
                .getProperty(FILENAME).getString());
        session.expire();
    }

    @Test
    public void testGetDatastreamContentInputStream() throws Exception {
        FedoraSession session = repository.login();
        final InputStream is = new ByteArrayInputStream("asdf".getBytes());
        containerService.findOrCreate(session, "/testDatastreamServiceObject");

        binaryService.findOrCreate(session, "/testDatastreamServiceObject/" + "testDatastreamNode")
                .setContent(
                        is,
                        "application/octet-stream",
                        null,
                        null,
                        null
                );

        session.commit();
        session.expire();
        session = repository.login();
        final FedoraBinary binary =
                binaryService.findOrCreate(session,
                    "/testDatastreamServiceObject/" + "testDatastreamNode");
        assertEquals("asdf", IOUtils.toString(binary.getContent(), "UTF-8"));
        session.expire();
    }


}
