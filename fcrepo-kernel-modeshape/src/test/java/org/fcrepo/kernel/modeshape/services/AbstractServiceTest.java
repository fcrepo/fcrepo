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
package org.fcrepo.kernel.modeshape.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static org.fcrepo.kernel.modeshape.services.AbstractService.encodePath;
import static org.fcrepo.kernel.modeshape.services.AbstractService.decodePath;
import static org.fcrepo.kernel.modeshape.services.AbstractService.registeredPrefixes;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.fcrepo.kernel.modeshape.FedoraSessionImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * AbstractService tests.
 *
 * @author whikloj
 * @since 2019-04-09
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AbstractServiceTest {

    @Mock
    private Session mockSession;

    @Mock
    private FedoraSessionImpl mockFedoraSession;

    @Mock
    private Workspace mockWork;

    @Mock
    private NamespaceRegistry mockRegistry;

    private final String[] prefixes = { "fedora", "fcr", "test" };

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockFedoraSession.getJcrSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWork);
        when(mockWork.getNamespaceRegistry()).thenReturn(mockRegistry);
        when(mockRegistry.getPrefixes()).thenReturn(prefixes);
        when(mockRegistry.getURI("fedora")).thenReturn("info/fedora#");
        when(mockRegistry.getURI("fcr")).thenReturn("info/fcr#");
        when(mockRegistry.getURI("test")).thenReturn("info/test#");
        // Needed due to static nature and previous tests.
        registeredPrefixes = null;
    }

    @Test
    public void testEncodingPaths() throws Exception {
        assertEquals("", encodePath("", mockFedoraSession));
        assertEquals("/", encodePath("/", mockFedoraSession));
        assertEquals("/ham/and/cheese/", encodePath("/ham/and/cheese/", mockFedoraSession));
        assertEquals("/1234/5678/--9", encodePath("/1234/5678/--9", mockFedoraSession));
        assertEquals("/1234/ham/new_ns%3Aspecial/bacon",
            encodePath("/1234/ham/new_ns:special/bacon", mockFedoraSession));
        assertEquals("/1234/ham/test:special/bacon", encodePath("/1234/ham/test:special/bacon", mockFedoraSession));
        assertEquals("1234-5678", encodePath("1234-5678", mockFedoraSession));
        assertEquals("very/large/container/fcr:acl", encodePath("very/large/container/fcr:acl", mockFedoraSession));
        assertEquals("something/else/fcr:versions", encodePath("something/else/fcr:versions", mockFedoraSession));
        assertEquals("what/is/the/1234/fedora:timemap",
            encodePath("what/is/the/1234/fedora:timemap", mockFedoraSession));

        assertEquals("block%3Aparty", encodePath("block:party", mockFedoraSession));
        assertEquals("/block%3Aparty", encodePath("/block:party", mockFedoraSession));
        assertEquals("block%3Aparty/", encodePath("block:party/", mockFedoraSession));
        assertEquals("/block%3Aparty/", encodePath("/block:party/", mockFedoraSession));

        assertEquals("big/block%3Aparty", encodePath("big/block:party", mockFedoraSession));
        assertEquals("/big/block%3Aparty", encodePath("/big/block:party", mockFedoraSession));
        assertEquals("big/block%3Aparty/", encodePath("big/block:party/", mockFedoraSession));
        assertEquals("/big/block%3Aparty/", encodePath("/big/block:party/", mockFedoraSession));

        assertEquals("what%3Aa/big/block%3Aparty", encodePath("what:a/big/block:party", mockFedoraSession));
        assertEquals("/what%3Aa/big/block%3Aparty", encodePath("/what:a/big/block:party", mockFedoraSession));
        assertEquals("what%3Aa/big/block%3Aparty/", encodePath("what:a/big/block:party/", mockFedoraSession));
        assertEquals("/what%3Aa/big/block%3Aparty/", encodePath("/what:a/big/block:party/", mockFedoraSession));

        assertEquals("what%3Aa/big/block%3Aparty/fcr:versions",
            encodePath("what:a/big/block:party/fcr:versions", mockFedoraSession));
        assertEquals("/what%3Aa/big/block%3Aparty/fcr:versions",
            encodePath("/what:a/big/block:party/fcr:versions", mockFedoraSession));
        assertEquals("what%3Aa/big/block%3Aparty/fcr:versions/",
            encodePath("what:a/big/block:party/fcr:versions/", mockFedoraSession));
        assertEquals("/what%3Aa/big/block%3Aparty/fcr:versions/",
            encodePath("/what:a/big/block:party/fcr:versions/", mockFedoraSession));

        assertEquals("/what/fedora:doing/to/the/fcr:jam/in/the%3Ahouse",
            encodePath("/what/fedora:doing/to/the/fcr:jam/in/the:house", mockFedoraSession));

    }

    @Test
    public void testDecodingPaths() throws Exception {
        assertEquals("", decodePath("", mockFedoraSession));
        assertEquals("/", decodePath("/", mockFedoraSession));
        assertEquals("/ham/and/cheese/", decodePath("/ham/and/cheese/", mockFedoraSession));
        assertEquals("/1234/5678/--9", decodePath("/1234/5678/--9", mockFedoraSession));
        assertEquals("/1234/ham/new_ns:special/bacon",
            decodePath("/1234/ham/new_ns%3Aspecial/bacon", mockFedoraSession));
        assertEquals("/1234/ham/test:special/bacon", decodePath("/1234/ham/test:special/bacon", mockFedoraSession));
        assertEquals("1234-5678", decodePath("1234-5678", mockFedoraSession));
        assertEquals("very/large/container/fcr:acl", decodePath("very/large/container/fcr:acl", mockFedoraSession));
        assertEquals("something/else/fcr:versions", decodePath("something/else/fcr:versions", mockFedoraSession));
        assertEquals("what/is/the/1234/fedora:timemap",
            decodePath("what/is/the/1234/fedora:timemap", mockFedoraSession));

        assertEquals("block:party", decodePath("block%3Aparty", mockFedoraSession));
        assertEquals("/block:party", decodePath("/block%3Aparty", mockFedoraSession));
        assertEquals("block:party/", decodePath("block%3Aparty/", mockFedoraSession));
        assertEquals("/block:party/", decodePath("/block%3Aparty/", mockFedoraSession));

        assertEquals("big/block:party", decodePath("big/block%3Aparty", mockFedoraSession));
        assertEquals("/big/block:party", decodePath("/big/block%3Aparty", mockFedoraSession));
        assertEquals("big/block:party/", decodePath("big/block%3Aparty/", mockFedoraSession));
        assertEquals("/big/block:party/", decodePath("/big/block%3Aparty/", mockFedoraSession));

        assertEquals("what:a/big/block:party", decodePath("what%3Aa/big/block%3Aparty", mockFedoraSession));
        assertEquals("/what:a/big/block:party", decodePath("/what%3Aa/big/block%3Aparty", mockFedoraSession));
        assertEquals("what:a/big/block:party/", decodePath("what%3Aa/big/block%3Aparty/", mockFedoraSession));
        assertEquals("/what:a/big/block:party/", decodePath("/what%3Aa/big/block%3Aparty/", mockFedoraSession));

        assertEquals("what:a/big/block:party/fcr:versions",
            decodePath("what%3Aa/big/block%3Aparty/fcr:versions", mockFedoraSession));
        assertEquals("/what:a/big/block:party/fcr:versions",
            decodePath("/what%3Aa/big/block%3Aparty/fcr:versions", mockFedoraSession));
        assertEquals("what:a/big/block:party/fcr:versions/",
            decodePath("what%3Aa/big/block%3Aparty/fcr:versions/", mockFedoraSession));
        assertEquals("/what:a/big/block:party/fcr:versions/",
            decodePath("/what%3Aa/big/block%3Aparty/fcr:versions/", mockFedoraSession));

        assertEquals("/what/fedora:doing/to/the/fcr:jam/in/the:house",
            decodePath("/what/fedora:doing/to/the/fcr:jam/in/the%3Ahouse", mockFedoraSession));

    }
}
