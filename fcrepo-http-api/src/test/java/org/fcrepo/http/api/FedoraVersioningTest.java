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
package org.fcrepo.http.api;

import com.hp.hpl.jena.query.Dataset;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.impl.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;

import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author cabeer
 */
public class FedoraVersioningTest {

    private FedoraVersioning testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    VersionService mockVersions;

    @Mock
    SessionFactory mockSessionFactory;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    private Session mockSession;

    @Mock
    private FedoraResourceImpl mockResource;

    private RdfStream mockRdfStream = new RdfStream();

    @Mock
    private Request mockRequest;

    @Mock
    private Variant mockVariant;

    @Mock
    private Dataset mockDataset;

    private String path = "/some/path";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraVersioning(path);
        mockSession = mockSession(testObj);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "versionService", mockVersions);
        setField(testObj, "sessionFactory", mockSessionFactory);
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn("/test/path");
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
    }


    @Test
    public void testGetVersionList() throws RepositoryException {
        final String pid = "FedoraVersioningTest";
        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(
                mockVariant);
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        when(mockResource.getTriples(any(HttpIdentifierTranslator.class), eq(VersionsRdfContext.class)))
                .thenReturn(mockRdfStream);
        when(mockResource.hasType("mix:versionable")).thenReturn(true);
        when(mockVariant.getMediaType()).thenReturn(
                new MediaType("text", "turtle"));
        final RdfStream response = testObj.getVersionList();
        assertEquals("Got wrong RdfStream!", mockRdfStream, response);
    }

}