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
package org.fcrepo.transform.http;

import org.fcrepo.http.commons.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.query.qom.QueryObjectModelFactory;
import org.modeshape.jcr.api.query.qom.SelectQuery;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.QueryObjectModel;
import javax.ws.rs.core.UriInfo;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FedoraSparqlTest {

    FedoraSparql testObj;

    private Session mockSession;
    private UriInfo uriInfo;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private QueryManager mockQueryManager;

    @Mock
    private QueryObjectModelFactory mockQueryFactory;

    @Mock
    private Column mockColumn;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private QueryObjectModel mockQuery;

    @Mock
    private QueryResult mockResults;

    @Before
    public void setUp() throws NoSuchFieldException, RepositoryException {
        initMocks(this);

        testObj = new FedoraSparql();

        this.uriInfo = getUriInfoImpl();
        TestHelpers.setField(testObj, "uriInfo", uriInfo);

        mockSession = mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);

        when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
        when(mockQueryManager.getQOMFactory()).thenReturn(mockQueryFactory);
        when(mockQueryFactory.column(anyString(), anyString(), anyString())).thenReturn(mockColumn);

        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.getNodeType(anyString())).thenReturn(mockNodeType);

        when(mockNodeType.getPropertyDefinitions()).thenReturn(new PropertyDefinition[] {});

        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);

        when(mockQueryFactory.createQuery(any(SelectQuery.class))).thenReturn(mockQuery);

        when(mockQuery.execute()).thenReturn(mockResults);
    }

    @Test
    @Ignore
    public void testRunSparqlQuery() throws Exception {
      //  final JQLResultSet resultSet = (JQLResultSet)testObj.runSparqlQuery(IOUtils.toInputStream("SELECT ?pid WHERE { ?pid <info:x> \"a\" }"), uriInfo);

      //  assertEquals(mockResults, resultSet.getQueryResult());
    }
}
