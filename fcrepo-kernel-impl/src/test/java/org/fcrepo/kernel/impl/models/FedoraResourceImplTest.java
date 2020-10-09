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

package org.fcrepo.kernel.impl.models;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.net.URI.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEGATE_TYPE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author pwinckles
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FedoraResourceImplTest {

    @Mock
    private TimeMap timeMap;

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceHeaders headers;

    private static final String ID = "info:fedora/test";

    private static final FedoraId FEDORA_ID = FedoraId.create(ID);

    @Test
    public void findMementoWhenOnlyOneAndBeforeSearch() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172117");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/0", match.getId());
    }

    @Test
    public void findClosestMementoWhenMultiple() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172117", "20200309172118", "20200309172119");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/1", match.getId());
    }

    @Test
    public void findClosestMementoWhenMultipleNoneExact() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172116", "20200309172117", "20200309172119");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/1", match.getId());
    }

    @Test
    public void findClosestMementoMultipleSameSecond() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172117", "20200309172117", "20200309172117");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/2", match.getId());
    }

    @Test
    public void findMementoWhenNonBeforeSearch() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos("20200309172119", "20200309172120", "20200309172121");
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertEquals(FEDORA_ID_PREFIX + "/0", match.getId());
    }

    @Test
    public void findNoMementoWhenThereAreNone() {
        final var resource = resourceWithMockedTimeMap();
        expectMementos();
        final var match = resource.findMementoByDatetime(instant("20200309172118"));
        assertNull("Should not find a memento", match);
    }

    @Test
    public void testTypesRdfSource() throws Exception {
        final var subject = createResource(ID);
        final String exampleType = "http://example.org/customType";
        final Model userModel = createDefaultModel();
        userModel.add(subject, type, createResource(exampleType));
        final var userStream = fromModel(subject.asNode(), userModel);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);
        when(psSession.getTriples(eq(FEDORA_ID), any())).thenReturn(userStream);

        final List<URI> expectedTypes = List.of(
                create(exampleType),
                create(BASIC_CONTAINER.toString()),
                create(RESOURCE.toString()),
                create(FEDORA_RESOURCE.toString()),
                create(VERSIONED_RESOURCE.getURI()),
                create(VERSIONING_TIMEGATE_TYPE)
        );

        final var resource = new FedoraResourceImpl(FEDORA_ID, null, sessionManager, resourceFactory);
        resource.setInteractionModel(BASIC_CONTAINER.toString());
        resource.setIsArchivalGroup(false);
        final var resourceTypes = resource.getTypes();

        // Initial lengths are the same
        assertEquals(expectedTypes.size(), resourceTypes.size());
        // Only keep the types in the expected list.
        resourceTypes.retainAll(expectedTypes);
        // Lengths are still the same.
        assertEquals(expectedTypes.size(), resourceTypes.size());
    }

    @Test
    public void testTypesNonRdfSource() throws Exception {
        final var descriptionFedoraId = FEDORA_ID.asDescription();
        final var subject = createResource(ID);
        final String exampleType = "http://example.org/customType";
        final Model userModel = createDefaultModel();
        userModel.add(subject, type, createResource(exampleType));
        final var userStream = fromModel(subject.asNode(), userModel);

        final var description = new NonRdfSourceDescriptionImpl(descriptionFedoraId, null, sessionManager,
                resourceFactory);

        when(resourceFactory.getResource((String)any(), eq(descriptionFedoraId))).thenReturn(description);
        when(sessionManager.getReadOnlySession()).thenReturn(psSession);
        when(psSession.getTriples(eq(descriptionFedoraId), any())).thenReturn(userStream);

        final List<URI> expectedTypes = List.of(
                create(exampleType),
                create(NON_RDF_SOURCE.toString()),
                create(RESOURCE.toString()),
                create(FEDORA_RESOURCE.toString()),
                create(FEDORA_BINARY.toString()),
                create(VERSIONED_RESOURCE.getURI()),
                create(VERSIONING_TIMEGATE_TYPE)
        );

        final var resource = new BinaryImpl(FEDORA_ID, null, sessionManager, resourceFactory);
        resource.setInteractionModel(NON_RDF_SOURCE.toString());
        resource.setIsArchivalGroup(false);
        final var resourceTypes = resource.getTypes();

        // Initial lengths are the same
        assertEquals(expectedTypes.size(), resourceTypes.size());
        // Only keep the types in the expected list.
        resourceTypes.retainAll(expectedTypes);
        // Lengths are still the same.
        assertEquals(expectedTypes.size(), resourceTypes.size());
    }

    @Test
    public void testGetChildren() {
        final var resource = new FedoraResourceImpl(FEDORA_ID, null, sessionManager, resourceFactory);
        assertEquals(0, resource.getChildren().count());
    }

    private void expectMementos(final String... instants) {
        final var mementos = new ArrayList<FedoraResource>(instants.length);
        for (int i = 0; i < instants.length; i++) {
            mementos.add(memento(String.valueOf(i), instant(instants[i])));
        }
        when(timeMap.getChildren()).thenReturn(mementos.stream());
    }

    private FedoraResource resourceWithMockedTimeMap() {
        final var resource = spy(new FedoraResourceImpl(FEDORA_ID, null, sessionManager, resourceFactory));
        doReturn(timeMap).when(resource).getTimeMap();
        return resource;
    }

    private FedoraResource memento(final String id, final Instant instant) {
        final String mementoTime = VersionService.MEMENTO_LABEL_FORMATTER.format(instant);
        final FedoraId fedoraID = FedoraId.create(id, FCR_VERSIONS, mementoTime);
        final var memento = new FedoraResourceImpl(fedoraID, null, sessionManager, resourceFactory);
        memento.setIsMemento(true);
        memento.setMementoDatetime(instant);
        return memento;
    }

    private Instant instant(final String timestamp) {
        return Instant.from(VersionService.MEMENTO_LABEL_FORMATTER.parse(timestamp));
    }

}
