/**
 * Copyright 2014 DuraSpace, Inc.
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

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.ok;

import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.transform.http.responses.ResultSetStreamingOutput;
import org.fcrepo.transform.sparql.JQLResultSet;
import org.fcrepo.transform.sparql.SparqlServiceDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.query.qom.QueryObjectModelFactory;
import org.modeshape.jcr.api.query.qom.SelectQuery;

import javax.jcr.Node;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_SPARQL_RDF_VARIANTS;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * <p>FedoraSparqlTest class.</p>
 * @author lsitu
 * @author cbeer
 */
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

    @Mock
    private IdentifierConverter<Resource, Node> MockIdentifierTranslator;

    @Mock
    private Variant mockVariant;

    @Mock
    private Request mockRequest;

    private static String testSparql = "SELECT ?s WHERE { ?x <" + RdfLexicon.DC_TITLE.getURI() + "> ?z }";

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        testObj = new FedoraSparql();

        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);

        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
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
    public void testSparqlServiceDescription() {
        final Response response = testObj.sparqlServiceDescription(mockRequest, uriInfo);
        assertTrue(response.getStatus() == OK.getStatusCode());
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream output) {
                final SparqlServiceDescription sd = new SparqlServiceDescription(mockSession, uriInfo);
                final Lang rdfLang = RDFLanguages.contentTypeToLang(response.getMediaType().toString());
                final Writer outWriter = new OutputStreamWriter(output);
                sd.createServiceDescription().asModel().write(outWriter, rdfLang.getName());
            }
        };
        final String expected = ok(stream).header("Content-Type",
                RDFLanguages.contentTypeToLang(response.getMediaType().toString()))
                .build().toString();
        assertEquals(expected, response.toString());
    }

    @Test
    public void testSparqlQueryForm() throws IOException, RepositoryException {
        final String[] nsPrefix = {};
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(nsPrefix);
        final Response response = testObj.sparqlQueryForm();
        assertTrue(response.getStatus() == OK.getStatusCode());
    }

    @Test
    public void testRunSparqlQuery() throws RepositoryException, IOException {
        when(mockRequest.selectVariant(POSSIBLE_SPARQL_RDF_VARIANTS)).thenReturn(mockVariant);
        when(mockVariant.getMediaType()).thenReturn(MediaType.valueOf(contentTypeTextTSV));
        final InputStream input = new ByteArrayInputStream(testSparql.getBytes());
        final Response response = testObj.runSparqlQuery(input, mockRequest, uriInfo);
        assertTrue(response.getStatus() == OK.getStatusCode());
        assertEquals(ok(new ResultSetStreamingOutput(new JQLResultSet(mockSession,
                new UriAwareIdentifierConverter(mockSession, uriInfo.getBaseUriBuilder()),
                mockResults), mockVariant.getMediaType())).build().toString(),
                response.toString());
    }

    @Test
    public void testRunSparqlQueryWithHTMLForm() throws RepositoryException {
        when(mockRequest.selectVariant(POSSIBLE_SPARQL_RDF_VARIANTS)).thenReturn(mockVariant);
        when(mockVariant.getMediaType()).thenReturn(MediaType.valueOf(contentTypeTextTSV));
        final Response response = testObj.runSparqlQuery(testSparql, mockRequest, uriInfo);
        assertTrue(response.getStatus() == OK.getStatusCode());
        assertEquals(ok(new ResultSetStreamingOutput(new JQLResultSet(mockSession,
                new UriAwareIdentifierConverter(mockSession, uriInfo.getBaseUriBuilder()),
                mockResults), mockVariant.getMediaType())).build().toString(),
                response.toString());
    }
}
