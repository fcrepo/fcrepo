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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class ContainerImplTest {
    @Mock
    private PersistentStorageSessionManager sessionManager;
    @Mock
    private ResourceFactory resourceFactory;

    private final static String TX_ID = "transacted";

    private FedoraId fedoraId;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        fedoraId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void getChildren_WithChildren() {
        final var child1 = mock(Container.class);
        final var child2 = mock(Binary.class);
        final var childrenStream = Stream.of(child1, child2);

        when(resourceFactory.getChildren(TX_ID, fedoraId)).thenReturn(childrenStream);

        final Container container = new ContainerImpl(fedoraId, TX_ID, sessionManager, resourceFactory);

        final var resultStream = container.getChildren();
        final var childrenList = resultStream.collect(Collectors.toList());
        assertEquals(2, childrenList.size());

        assertTrue(childrenList.stream().anyMatch(c -> c instanceof Container));
        assertTrue(childrenList.stream().anyMatch(c -> c instanceof Binary));
    }
}
