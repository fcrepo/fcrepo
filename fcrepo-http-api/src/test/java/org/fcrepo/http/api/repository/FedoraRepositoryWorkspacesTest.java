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

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Andrew Woods Date: 8/7/13
 */
public class FedoraRepositoryWorkspacesTest {

    private FedoraRepositoryWorkspaces workspaces;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        workspaces = new FedoraRepositoryWorkspaces();
        setField(workspaces, "session", mockSession);
        setField(workspaces, "uriInfo", mockUriInfo);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        mockUriInfo = getUriInfoImpl();
    }

    @Test
    public void testGetWorkspaces() throws Exception {
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(
                new String[] {"xxx"});

        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                mockNamespaceRegistry);

        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[]{"yyy"});
        when(mockNamespaceRegistry.getURI("yyy")).thenReturn("http://example.com");

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(mockUriBuilder);
        when(mockUriBuilder.path(any(String.class))).thenReturn(mockUriBuilder);

        final URI uri = new URI("http://base/workspaces");
        when(mockUriBuilder.build()).thenReturn(uri);

        // Do the test.
        final Dataset dataset = workspaces.getWorkspaces(mockUriInfo);

        final Resource resource =
            dataset.getDefaultModel().getResource(uri.toString());

        final String resourceName = resource.toString();

        org.junit.Assert.assertNotNull(resourceName);
        assertEquals(uri.toString(), resourceName);
    }

    @Test
    public void testCreateWorkspace() throws Exception {
        final Response response = workspaces.createWorkspace("xxx", mockUriInfo);
        verify(mockWorkspace).createWorkspace("xxx");
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testDeleteWorkspace() throws Exception {
        final Response response = workspaces.deleteWorkspace("xxx");
        verify(mockWorkspace).deleteWorkspace("xxx");
        assertEquals(204, response.getStatus());
    }
}
