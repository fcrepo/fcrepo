/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class FedoraRepositoryNamespacesTest {

    FedoraRepositoryNamespaces testObj;

    NodeService mockNodeService;

    private Session mockSession;

    @Before
    public void setUp() throws RepositoryException, URISyntaxException,
            NoSuchFieldException {
        mockNodeService = mock(NodeService.class);

        testObj = new FedoraRepositoryNamespaces();
        TestHelpers.setField(testObj, "nodeService", mockNodeService);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        mockSession = TestHelpers.mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testGetNamespaces() throws RepositoryException, IOException {

        final Dataset mockDataset = mock(Dataset.class);

        when(mockNodeService.getNamespaceRegistryGraph(mockSession))
                .thenReturn(mockDataset);
        assertEquals(mockDataset, testObj.getNamespaces());
    }

    @Test
    public void testUpdateNamespaces() throws RepositoryException, IOException {

        final Model model = ModelFactory.createDefaultModel();
        final Dataset mockDataset = DatasetFactory.create(model);

        when(mockNodeService.getNamespaceRegistryGraph(mockSession))
                .thenReturn(mockDataset);

        testObj.updateNamespaces(new ByteArrayInputStream(
                "INSERT { <http://example.com/this> <http://example.com/is> \"abc\"} WHERE { }"
                        .getBytes()));

        assertEquals(1, model.size());
    }
}
