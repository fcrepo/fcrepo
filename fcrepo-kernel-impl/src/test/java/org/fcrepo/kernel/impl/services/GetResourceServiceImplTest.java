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
package org.fcrepo.kernel.impl.services;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.UUID;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GetResourceServiceImplTest {

    @Mock
    private PersistentStorageSessionManager psManager;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ResourceFactory rsFactory;

    @Mock
    private Transaction transaction;

    @Mock
    private ResourceHeaders headers;

    @InjectMocks
    private GetResourceServiceImpl service;

    @Before
    public void setUp() {
        when(psManager.getReadOnlySession()).thenReturn(psSession);
        when(psManager.getSession(ArgumentMatchers.any())).thenReturn(psSession);
        when(transaction.getId()).thenReturn(UUID.randomUUID().toString());
        setField(service, "psManager", psManager);
        setField(service, "rsFactory", rsFactory);
    }

    @Test
    public void doesResourceExists() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        final ResourceHeaders headers = new ResourceHeadersImpl();
        when(psSession.getHeaders(identifier, null)).thenReturn(headers);
        final boolean answer = service.doesResourceExist(transaction, identifier, null);
        assertTrue(answer);
    }

    @Test
    public void doesResourceDoesntExist() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        when(psSession.getHeaders(identifier, null)).thenThrow(PersistentItemNotFoundException.class);
        final boolean answer = service.doesResourceExist(transaction, identifier, null);
        assertFalse(answer);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void doesResourceException() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        when(psSession.getHeaders(identifier, null)).thenThrow(PersistentSessionClosedException.class);
        service.doesResourceExist(transaction, identifier, null);
    }

    @Test
    public void getResourceDoesntExist() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        when(psSession.getHeaders(identifier, null)).thenThrow(PersistentItemNotFoundException.class);
        final FedoraResource resource = service.getResource(transaction, identifier, null);
        assertNull(resource);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void getResourceException() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        when(psSession.getHeaders(identifier, null)).thenThrow(PersistentSessionClosedException.class);
        service.getResource(transaction, identifier, null);
    }

    @Test
    public void getResourceContainer() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        when(headers.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        when(psSession.getHeaders(identifier, null)).thenReturn(headers);
        final FedoraResource resource = service.getResource(transaction, identifier, null);
        assertTrue(resource instanceof Container);
    }

    @Test
    public void getResourceBinary() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        when(headers.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(psSession.getHeaders(identifier, null)).thenReturn(headers);
        final FedoraResource resource = service.getResource(transaction, identifier, null);
        assertTrue(resource instanceof Binary);
    }

}
