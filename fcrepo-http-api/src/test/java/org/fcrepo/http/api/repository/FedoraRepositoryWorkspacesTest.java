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
package org.fcrepo.http.api.repository;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockRepository;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.RdfLexicon;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.Repository;

import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.NotFoundException;

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

    @Mock
    private Session mockWorkspaceSession;

    @Mock
    private Workspace mockOtherWorkspace;

    @Mock
    private Repository mockRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mockUriInfo = getUriInfoImpl();
        workspaces = new FedoraRepositoryWorkspaces();
        setField(workspaces, "session", mockSession);
        setField(workspaces, "uriInfo", mockUriInfo);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getRepository()).thenReturn(mockRepository);
    }

    @Test
    public void testGetWorkspaces() throws Exception {
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(
                new String[] {"xxx"});


        // Do the test.
        final Model result = workspaces.getWorkspaces().asModel();

        assertTrue(result.contains(createResource("http://localhost/fcrepo/"),
                                      RdfLexicon.HAS_WORKSPACE,
                                      createResource("http://localhost/fcrepo/workspace:xxx" )));

    }

    @Test
    public void testCreateWorkspace() throws Exception {
        final Repository mockRepository = mockRepository();
        when(mockSession.getRepository()).thenReturn(mockRepository);
        when(mockWorkspaceSession.getRepository()).thenReturn(mockRepository);
        when(mockRepository.login("xxx")).thenReturn(mockWorkspaceSession);
        when(mockWorkspaceSession.getWorkspace()).thenReturn(mockOtherWorkspace);
        when(mockOtherWorkspace.getName()).thenReturn("xxx");
        final Response response = workspaces.createWorkspace("xxx", mockUriInfo);
        verify(mockWorkspace).createWorkspace("xxx");
        assertEquals(201, response.getStatus());
        final String location = response.getMetadata().getFirst("Location").toString();
        assertEquals("http://localhost/fcrepo/workspace:xxx/", location);
    }

    @Test
    public void testDeleteWorkspace() throws Exception {
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(new String[] { "xxx" });
        final Response response = workspaces.deleteWorkspace("xxx");
        verify(mockWorkspace).deleteWorkspace("xxx");
        assertEquals(204, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteMissingWorkspace() throws Exception {
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(new String[] { "yyy" });
        final Response response = workspaces.deleteWorkspace("xxx");
        assertEquals(404, response.getStatus());
    }
}
