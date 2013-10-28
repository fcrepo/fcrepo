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
package org.fcrepo.http.api.repository;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.io.IOException;
import java.io.InputStream;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 *
 * @author cbeer
 */
public class FedoraRepositoryNodeTypesTest {

    private FedoraRepositoryNodeTypes testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private RdfStream mockRdfStream;

    private Session mockSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraRepositoryNodeTypes();
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
    }

    @Test
    public void itShouldRetrieveNodeTypesRdfStream() throws RepositoryException {
        final Model mockModel = ModelFactory.createDefaultModel();

        when(mockRdfStream.asModel()).thenReturn(mockModel);
        when(mockNodes.getNodeTypes(mockSession)).thenReturn(mockRdfStream);

        final Dataset nodeTypes = testObj.getNodeTypes();

        assertEquals(mockModel, nodeTypes.getDefaultModel());
        verify(mockSession).logout();
    }

    @Test
    public void itShouldPersistIncomingCndFile() throws RepositoryException, IOException {
        testObj.updateCnd(mockInputStream);

        verify(mockNodes).registerNodeTypes(mockSession, mockInputStream);
    }
}
