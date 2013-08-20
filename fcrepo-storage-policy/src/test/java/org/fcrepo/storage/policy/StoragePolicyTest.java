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

package org.fcrepo.storage.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.poi.hwpf.model.NoteType;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.policy.Policy;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.JcrTools;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class StoragePolicyTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private SessionFactory mockSessions;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode;

    @Mock
    private Node mockCodeNode;

    @Mock
    private Property mockProperty;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private StoragePolicyDecisionPoint mockPolicyDecisionPoint;

    @Mock
    private JcrTools mockJcrTools;

    @Before
    public void setup() throws RepositoryException {
        initMocks(this);
        when(mockSessions.getSession()).thenReturn(mockSession);
        when(
            mockNodeService.findOrCreateNode(mockSession,
                "/fedora:system/fedora:storage_policy", null)).thenReturn(
            mockCodeNode);
        Property property = mock(Property.class);
        when(property.getString()).thenReturn("image/tiff");
        when(mockCodeNode.getProperty("image/tiff")).thenReturn(property);
    }

    @Test
    public void nodeCreated() throws Exception {
        mockSession = mockSessions.getSession();
        mockNodeService.findOrCreateNode(mockSession,
            "/fedora:system/fedora:storage_policy", null);
        assertEquals(mockCodeNode.getProperty("image/tiff").getString(),
            "image/tiff");
    }
   
    @Test
    public void getPolicyType() throws PolicyTypeException {
        StoragePolicy obj = new StoragePolicy();
        Policy type = obj.newPolicyInstance("mix:mimeType", "image/tiff", null);
        assertEquals(type.getClass(), MimeTypePolicy.class);
    }

    @Test
    public void ensureUniquePolicy() {
        /* List<Policy> can have MimeType("image/tiff", "tiff-store") and MimeType("image/tiff", "cloud-tiffs").
        List<Policy> can NOT have two MimeType("image/tiff", "tiff-store"); */

        PolicyDecisionPoint obj = new PolicyDecisionPoint();
        Policy p1 = new MimeTypePolicy("image/tiff", "tiff-store");
        Policy p2 = new MimeTypePolicy("image/tiff", "tiff-store");
        Policy p3 = new MimeTypePolicy("image/tiff", "tiff-store");
        if (!obj.contains(p3)) {
            obj.addPolicy(p1);
        }
        if (!obj.contains(p3)) {
            obj.addPolicy(p2);
        }
        assertEquals(obj.contains(p1), true);
        assertEquals(obj.contains(p2), true);
        obj.removePolicy(p1);
        assertEquals(obj.contains(p1), false);
        assertEquals(obj.contains(p2), false);
    }

    @Test
    public void testPost() throws Exception {
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockSession.getRootNode()).thenReturn(mockRootNode);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
        when(mockNodeTypeManager.getNodeType("mix:mimeType")).thenReturn(mockNodeType);
        when(mockNodeType.getName()).thenReturn("mix:mimeType");

        when(mockJcrTools.findOrCreateNode(any(Session.class),
                                           anyString(),
                                           anyString())).thenReturn(
                mockCodeNode);

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(
                "http://localhost/rest/policies/jcr:storagepolicy"));

        StoragePolicy storagePolicy = new StoragePolicy();
        storagePolicy.setJcrTools(mockJcrTools);
        storagePolicy.setUriInfo(mockUriInfo);
        storagePolicy.request = mockRequest;
        storagePolicy.session = mockSession;
        storagePolicy.storagePolicyDecisionPoint = mockPolicyDecisionPoint;

        Response response = storagePolicy.post(StoragePolicy.POLICY_RESOURCE,
                                               "mix:mimeType image/tiff cloud");
        assertNotNull(response);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testGetAllPolicies() throws Exception {
        StoragePolicy storagePolicy = new StoragePolicy();
        Response response = storagePolicy.get("policies");
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGetPolicy() throws Exception {
        Value mockValue = mock(Value.class);
        when(mockValue.getString()).thenReturn("image/tiff cloud");
        when(mockProperty.getValues()).thenReturn(new Value[]{mockValue});
        when(mockCodeNode.getProperty("mix:mimeType")).thenReturn(mockProperty);
        when(mockJcrTools.findOrCreateNode(any(Session.class),
                                           anyString(),
                                           anyString())).thenReturn(
                mockCodeNode);

        StoragePolicy storagePolicy = new StoragePolicy();
        storagePolicy.setJcrTools(mockJcrTools);
        storagePolicy.session = mockSession;


        Response response = storagePolicy.get("mix:mimeType");
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }
    
    

}