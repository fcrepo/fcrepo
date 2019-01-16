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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.fcrepo.kernel.api.RdfLexicon.LDPCV_TIME_MAP;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;

import javax.inject.Inject;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.TimeMapService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author bbpennel
 */
@ContextConfiguration({ "/spring-test/fcrepo-config.xml" })
public class TimeMapServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repo;

    @Inject
    private ContainerService containerService;

    @Inject
    private TimeMapService timeMapService;

    @Inject
    private BinaryService binaryService;

    private FedoraSession session;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        session = repo.login();
    }

    @After
    public void tearDown() {
        session.expire();
    }

    @Test
    public void testCreateTimeMap() {
        final String path = "/" + getRandomPid();
        final Container object = containerService.findOrCreate(session, path);
        final FedoraTimeMap timeMap = timeMapService.findOrCreate(session, path);
        session.commit();

        verifyTimeMap(object, timeMap, Container.class);
    }

    @Test
    public void testCreateTimeMapFromFullPath() {
        final String path = "/" + getRandomPid();
        final Container object = containerService.findOrCreate(session, path);
        final String timeMapPath = path + "/" + LDPCV_TIME_MAP;
        final FedoraTimeMap timeMap = timeMapService.findOrCreate(session, timeMapPath);
        session.commit();

        verifyTimeMap(object, timeMap, Container.class);
    }

    @Test
    public void testFindTimeMap() {
        final String path = "/" + getRandomPid();
        final Container object = containerService.findOrCreate(session, path);
        timeMapService.findOrCreate(session, path);
        session.commit();

        final FedoraTimeMap timeMap = timeMapService.find(session, path);

        verifyTimeMap(object, timeMap, Container.class);
    }

    @Test
    public void testTimeMapExists() {
        final String path = "/" + getRandomPid();
        containerService.findOrCreate(session, path);
        session.commit();

        assertFalse(timeMapService.exists(session, path));

        timeMapService.findOrCreate(session, path);
        session.commit();

        assertTrue(timeMapService.exists(session, path));
    }

    @Test
    public void testFindOrCreateForBinary() throws Exception {
        final String path = "/" + getRandomPid();
        final FedoraBinary object = binaryService.findOrCreate(session, path);
        try (InputStream contentStream = new ByteArrayInputStream("content".getBytes())) {
            object.setContent(contentStream, "text/plain", null, null, null);
        }

        final FedoraTimeMap binaryTimeMap = timeMapService.findOrCreate(session, path);
        verifyTimeMap(object, binaryTimeMap, FedoraBinary.class);

        final FedoraTimeMap descTimeMap = timeMapService.findOrCreate(session, path + "/" + FEDORA_DESCRIPTION);
        verifyTimeMap(object.getDescription(), descTimeMap, NonRdfSourceDescription.class);
    }

    private void verifyTimeMap(final FedoraResource resource, final FedoraTimeMap timeMap,
            final Class<?> resClass) {
        final FedoraResource originalResource = timeMap.getOriginalResource();
        assertTrue(resClass.isInstance(originalResource));
        assertEquals("Original resource must reference original container",
                resource.getPath(), originalResource.getPath());
    }
}
