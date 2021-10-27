/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author pwinckles
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TimeMapImplTest {

    @Mock
    private PersistentStorageSessionManager sessionManager;

    @Mock
    private PersistentStorageSession session;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private Transaction transaction;

    private final String defaultId = FEDORA_ID_PREFIX + "/resource";

    private TimeMapImpl timeMap;

    @Before
    public void setup() {
        when(transaction.getId()).thenReturn(UUID.randomUUID().toString());
        when(transaction.isShortLived()).thenReturn(true);
        timeMap = createTimeMap(defaultId);

        when(sessionManager.getReadOnlySession()).thenReturn(session);
    }

    @Test
    public void copyParentPropsWhenCreatingTimeMap() {
        final var parent = createParent(FEDORA_ID_PREFIX + "id");
        final var timeMap = new TimeMapImpl(parent, transaction, sessionManager, resourceFactory);

        assertEquals(parent.getId(), timeMap.getId());
        assertEquals(parent.getCreatedBy(), timeMap.getCreatedBy());
        assertEquals(parent.getCreatedDate(), timeMap.getCreatedDate());
        assertEquals(parent.getLastModifiedBy(), timeMap.getLastModifiedBy());
        assertEquals(parent.getLastModifiedDate(), timeMap.getLastModifiedDate());
        assertEquals(parent.getEtagValue(), timeMap.getEtagValue());
        assertEquals(parent.getStateToken(), timeMap.getStateToken());
        assertSame(parent, timeMap.getOriginalResource());
        assertSame(timeMap, timeMap.getTimeMap());
    }

    @Test
    public void shouldHaveTimeMapTypes() {
        assertTrue(timeMap.getTypes().containsAll(
                List.of(
                URI.create(RdfLexicon.VERSIONING_TIMEMAP.getURI())
                ))
        );
    }

    @Test
    public void returnChildMementosWhenExist() throws PersistentStorageException, PathNotFoundException {
        final var version1 = instant("20200225131900");
        final var version2 = instant("20200226131900");

        mockListVersions(defaultId, version1, version2);

        final var children = timeMap.getChildren();

        assertTrue(children.map(FedoraResource::getMementoDatetime)
                .collect(Collectors.toList()).containsAll(List.of(version1, version2)));
    }

    @Test
    public void returnNoMementosWhenNoneExist() throws PersistentStorageException, PathNotFoundException {
        mockListVersions(defaultId);

        final var children = timeMap.getChildren();

        assertEquals(0, children.count());
    }

    @Test
    public void returnTriples() throws PersistentStorageException, PathNotFoundException {
        final var version1 = instant("20200225131900");
        final var version2 = instant("20200226131900");

        final var mementos = mockListVersions(defaultId, version1, version2);

        final var triples = timeMap.getTriples();

        final var timeMapUri = node(timeMap);

        assertThat(triples.collect(Collectors.toList()), contains(
                Triple.create(timeMapUri, RdfLexicon.CONTAINS.asNode(), node(mementos.get(1))),
                Triple.create(timeMapUri, RdfLexicon.CONTAINS.asNode(), node(mementos.get(0))),
                Triple.create(timeMapUri, RdfLexicon.MEMENTO_ORIGINAL_RESOURCE.asNode(),
                        NodeFactory.createURI(defaultId))));
    }

    private List<FedoraResource> mockListVersions(final String id, final Instant... versions)
            throws PersistentStorageException, PathNotFoundException {
        if (versions.length == 0) {
            when(session.listVersions(FedoraId.create(id))).thenReturn(Collections.emptyList());
        } else {
            when(session.listVersions(FedoraId.create(id))).thenReturn(List.of(versions));
        }

        final var mementos = new ArrayList<FedoraResource>();

        for (final var version : versions) {
            final var memento = createMemento(id, version);
            mementos.add(memento);
            final FedoraId mementoID = FedoraId.create(id, FCR_VERSIONS, instantStr(version));
            when(memento.getFedoraId()).thenReturn(mementoID);
            when(resourceFactory.getResource(transaction, mementoID)).thenReturn(memento);
        }
        return mementos;
    }

    private Node node(final FedoraResource memento) {
        return NodeFactory.createURI(memento.getFedoraId().getFullId());
    }

    private Instant instant(final String instantStr) {
        return Instant.from(VersionService.MEMENTO_LABEL_FORMATTER.parse(instantStr));
    }

    private String instantStr(final Instant instant) {
        return VersionService.MEMENTO_LABEL_FORMATTER.format(instant);
    }

    private FedoraResource createMemento(final String id, final Instant version) {
        final var mock = mock(FedoraResource.class);
        when(mock.getId()).thenReturn(id);
        when(mock.isMemento()).thenReturn(true);
        when(mock.getMementoDatetime()).thenReturn(version);
        return mock;
    }

    private TimeMapImpl createTimeMap(final String id) {
        return new TimeMapImpl(createParent(id), transaction, sessionManager, resourceFactory);
    }

    private FedoraResource createParent(final String id) {
        final var fedoraId = FedoraId.create(id);
        final var parent = new ContainerImpl(fedoraId, transaction, sessionManager, resourceFactory, null);

        parent.setCreatedBy("createdBy");
        parent.setCreatedDate(Instant.now());
        parent.setLastModifiedBy("modifiedBy");
        parent.setLastModifiedDate(Instant.now());
        parent.setEtag("etag");
        parent.setStateToken("stateToken");

        return parent;
    }

}
