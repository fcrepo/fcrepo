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
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.ResourceContent;

import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;


/**
 * ReindexService tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReindexServiceTest {

    @Mock
    private OcflObjectSessionFactory ocflObjectSessionFactory;

    @Mock
    private FedoraToOcflObjectIndex ocflObjectIndex;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private SearchIndex searchIndex;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private OcflObjectSession objectSession;

    private ReindexService reindexService;

    @Before
    public void setUp() {
        reindexService = new ReindexService(ocflObjectSessionFactory, ocflObjectIndex, containmentIndex, searchIndex,
                referenceService, membershipService, 5);
        when(ocflObjectSessionFactory.newSession(anyString())).thenReturn(objectSession);
    }

    @Test
    public void testRebuildOnce() throws Exception {
        final var transactionId = getRandomId();
        final var parentId = FedoraId.create(getRandomId());
        final var childId = parentId.resolve(getRandomId());
        final var headers1 = ResourceHeaders.builder().withId(parentId.getFullId())
                .withArchivalGroup(true).withInteractionModel(BASIC_CONTAINER.toString()).build();
        final var headers2 = ResourceHeaders.builder().withId(childId.getFullId())
                .withInteractionModel(BASIC_CONTAINER.toString()).withParent(parentId.getFullId()).build();

        final String childRdf = "<" + childId + "> <http://purl.org/dc/elements/1.1/title> \"Title\" .";
        final Optional<InputStream> parentStream = Optional.empty();
        final InputStream childStream = IOUtils.toInputStream(childRdf, UTF_8);
        final ResourceContent parentContent = new ResourceContent(parentStream, headers1);
        final ResourceContent childContent = new ResourceContent(childStream, headers2);

        when(objectSession.streamResourceHeaders()).thenReturn(Stream.of(headers1, headers2));
        when(objectSession.readContent(childId.getFullId())).thenReturn(childContent);
        when(objectSession.readContent(parentId.getFullId())).thenReturn(parentContent);

        reindexService.indexOcflObject(transactionId, parentId.getFullId());

        verify(containmentIndex).addContainedBy(transactionId, FedoraId.getRepositoryRootId(), parentId);
        verify(containmentIndex).addContainedBy(transactionId, parentId, childId);
        verify(referenceService).updateReferences(eq(transactionId), eq(childId), isNull(), any(RdfStream.class));
        verify(ocflObjectIndex).addMapping(transactionId, parentId, parentId, parentId.getFullId());
        verify(ocflObjectIndex).addMapping(transactionId, childId, parentId, parentId.getFullId());
        verify(searchIndex, times(2))
                .addUpdateIndex(eq(transactionId), any(org.fcrepo.kernel.api.models.ResourceHeaders.class));

        final var result = Mockito.mock(SearchResult.class);
        when(result.getItems()).thenReturn(Collections.emptyList());
        when(searchIndex.doSearch(any(SearchParameters.class))).thenReturn(result);
        reindexService.commit(transactionId);
        verify(containmentIndex).commitTransaction(transactionId);
        verify(ocflObjectIndex).commit(transactionId);
        verify(referenceService).commitTransaction(transactionId);
        verify(searchIndex).doSearch(any(SearchParameters.class));
    }

    private String getRandomId() {
        return UUID.randomUUID().toString();
    }
}
