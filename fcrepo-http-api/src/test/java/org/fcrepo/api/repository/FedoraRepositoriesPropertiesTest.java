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

package org.fcrepo.api.repository;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraRepositoriesPropertiesTest {

    private FedoraRepositoriesProperties testObj;

    private NodeService mockNodes;

    private Session mockSession;

    @Before
    public void setUp() throws Exception {
        mockNodes = mock(NodeService.class);
        testObj = new FedoraRepositoriesProperties();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setSession(mockSession);
        TestHelpers.setField(testObj, "nodeService", mockNodes);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
    }

    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final FedoraObject mockObject = mock(FedoraObject.class);

        when(mockObject.getDatasetProblems()).thenReturn(null);
        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, "/")).thenReturn(mockObject);

        testObj.updateSparql(mockStream);

        verify(mockObject).updatePropertiesDataset(any(GraphSubjects.class),
                eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }
}
