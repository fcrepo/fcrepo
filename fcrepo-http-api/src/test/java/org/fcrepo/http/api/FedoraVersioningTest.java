/**
 * Copyright 2015 DuraSpace, Inc.
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

import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;

import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.VersionsRdfContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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

    private final RdfStream mockRdfStream = new RdfStream();

    @Mock
    private Request mockRequest;

    @Mock
    private Variant mockVariant;

    private final String path = "/some/path";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = spy(new FedoraVersioning(path));
        mockSession = mockSession(testObj);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "versionService", mockVersions);
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn(path);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getNode(path)).thenReturn(mockNode);
        doReturn(mockResource).when(testObj).resource();

        setField(testObj, "idTranslator",
                new HttpResourceConverter(mockSession, UriBuilder.fromUri("http://localhost/fcrepo/{path: .*}")));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testGetVersionList() {
        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(
                mockVariant);
        when(mockNodes.find(any(Session.class), anyString())).thenReturn(
                mockResource);
        when(mockResource.getTriples(any(IdentifierConverter.class), eq(VersionsRdfContext.class)))
                .thenReturn(mockRdfStream);
        when(mockResource.isVersioned()).thenReturn(true);
        when(mockVariant.getMediaType()).thenReturn(
                new MediaType("text", "turtle"));
        final RdfStream response = testObj.getVersionList();
        assertEquals("Got wrong RdfStream!", mockRdfStream, response);
    }

}
