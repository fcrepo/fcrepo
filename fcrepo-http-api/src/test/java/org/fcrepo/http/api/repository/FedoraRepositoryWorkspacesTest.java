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
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.api.repository.FedoraRepositoryWorkspaces;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Andrew Woods
 *         Date: 8/7/13
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
    }

    @Test
    public void testGetWorkspaces() throws Exception {
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(new String[]{"xxx"});

        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                mockNamespaceRegistry);
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[]{"yyy"});

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(mockUriBuilder);
        when(mockUriBuilder.path(any(String.class))).thenReturn(mockUriBuilder);

        URI uri = new URI("http://base/workspaces");
        when(mockUriBuilder.build()).thenReturn(uri);

        // Do the test.
        Dataset dataset = workspaces.getWorkspaces();

        Resource resource = dataset.getDefaultModel()
                .getResource(uri.toString());
        String resourceName = resource.toString();

        org.junit.Assert.assertNotNull(resourceName);
        org.junit.Assert.assertEquals(uri.toString(), resourceName);
    }

}
