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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.inject.Inject;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.DESCRIBED_BY;
import static org.fcrepo.kernel.modeshape.services.VersionServiceImpl.VERSION_TRIPLES;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/fcrepo-config.xml"})
public class VersionServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    NodeService nodeService;

    @Inject
    private ContainerService containerService;

    @Inject
    private BinaryService binaryService;

    @Inject
    private VersionService versionService;

    private DefaultIdentifierTranslator subjects;

    private FedoraSession session;

    private static final Instant mementoDate1 = Instant.now();

    private static final Instant mementoDate2 = LocalDateTime.of(2000, 5, 10, 18, 30)
            .atZone(ZoneOffset.UTC).toInstant();

    @Before
    public void setUp() {
        session = repository.login();
        subjects = new DefaultIdentifierTranslator(getJcrSession(session));
    }

    @After
    public void tearDown() {
        session.expire();
    }

    @Test
    public void testCreateVersion() {
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, resource, subjects, mementoDate1);
        session.commit();
        assertEquals(1L, countVersions(session, resource));
    }

    @Test
    public void testCreateMementoWithChildReference() {
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        final String childPath = "/" + pid + "/x";
        final String childLocation = "info:fedora" + childPath;
        final FedoraResource childResource = containerService.findOrCreate(session, childPath);
        session.commit();

        // create a memento
        versionService.createVersion(session, resource, subjects, mementoDate2);
        session.commit();

        // verify the child node containment triple
        final FedoraResource mementoBeforeDeletion = resource.getTimeMap().getChild("20000510183000");
        assertTrue(mementoBeforeDeletion.getTriples(subjects, VERSION_TRIPLES)
                .anyMatch(x -> x.getPredicate().getURI().equals(CONTAINS.getURI())
                            && x.getObject().getURI().equals(childLocation)));

        // delete the child node
        childResource.delete();
        session.commit();

        assertFalse(resource.getTriples(subjects, VERSION_TRIPLES)
                .anyMatch(x -> x.getPredicate().getURI().equals(CONTAINS.getURI())
                            && x.getObject().getURI().equals(childLocation)));

        // verify the child node containment triple after child deletion
        final FedoraResource mementoAfterDeletion = resource.getTimeMap().getChild("20000510183000");
        assertTrue(mementoAfterDeletion.getTriples(subjects, VERSION_TRIPLES)
                          .anyMatch(x -> x.getPredicate().getURI().equals(CONTAINS.getURI())
                                      && x.getObject().getURI().equals(childLocation)));
    }

    @Test
    public void testRemoveVersion() {
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, resource, subjects, mementoDate1);
        session.commit();
        assertEquals(1L, countVersions(session, resource));

        // create another version
        versionService.createVersion(session, resource, subjects, mementoDate2);
        session.commit();
        assertEquals(2L, countVersions(session, resource));
    }

    @Test
    public void testCreateDescriptionVersion() {
        final String pid = getRandomPid();
        final FedoraResource resource = binaryService.findOrCreate(session, "/" + pid);
        session.commit();

        final FedoraResource descResc = resource.getDescription();

        // create a memento
        final FedoraResource memento = versionService.createVersion(session, descResc, subjects, mementoDate2);
        session.commit();

        final String descPath = "info:fedora/" + pid + "/fedora:description";

        assertTrue(memento.getTriples(subjects, VERSION_TRIPLES)
                .anyMatch(x -> x.getPredicate().getURI().equals(DESCRIBED_BY.getURI()) && x.getObject().getURI()
                        .equals(descPath)));

        assertEquals(1L, countVersions(session, descResc));
    }

    private static long countVersions(final FedoraSession session, final FedoraResource resource ) {
        final FedoraResource timeMap = resource.getTimeMap();
        return timeMap.getChildren().count();
    }
}
