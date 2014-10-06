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

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;

import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.VersionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;

/**
 * <p>FedoraVersionsTest class.</p>
 *
 * @author awoods
 */
public class FedoraVersionsTest {

    private FedoraVersions testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    VersionService mockVersions;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    private Session mockSession;

    @Mock
    private FedoraResourceImpl mockResource;

    @Mock
    private Request mockRequest;

    @Mock
    private Variant mockVariant;

    @Mock
    private Dataset mockDataset;

    private String path = "/some/path";
    private String versionLabel = "someLabel";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = spy(new FedoraVersions(path, versionLabel, ""));
        mockSession = mockSession(testObj);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "versionService", mockVersions);
        when(mockResource.getPath()).thenReturn(path);
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);

        setField(testObj, "identifierTranslator",
                new UriAwareIdentifierConverter(mockSession, UriBuilder.fromUri("http://localhost/fcrepo/{path: .*}")));
    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {
        doReturn(mockResource).when(testObj).unversionedResource();

        final Response response = testObj.addVersion();
        verify(mockResource).addVersionLabel(anyString());
        verify(mockVersions).createVersion(any(Workspace.class),
                Matchers.<Collection<String>>any());
        assertNotNull(response);
    }

    @Test
    public void testRevertToVersion() throws RepositoryException {
        doReturn(mockResource).when(testObj).unversionedResource();
        final Response response = testObj.revertToVersion();
        verify(mockVersions).revertToVersion(testObj.session.getWorkspace(), path, versionLabel);
        assertNotNull(response);
    }

    @Test (expected = PathNotFoundException.class)
    public void testRevertToVersionFailure() throws RepositoryException {
        doThrow(PathNotFoundException.class).when(testObj).unversionedResource();
        testObj.revertToVersion();
    }

    @Test
    public void testRemoveVersion() throws RepositoryException {
        doReturn(mockResource).when(testObj).unversionedResource();
        final Response response = testObj.removeVersion();
        verify(mockVersions).removeVersion(testObj.session.getWorkspace(), path, versionLabel);
        assertNotNull(response);
    }

    @Test (expected = PathNotFoundException.class)
    public void testRemoveVersionFailure() throws RepositoryException {
        doThrow(PathNotFoundException.class).when(testObj).unversionedResource();
        testObj.removeVersion();
    }

}
