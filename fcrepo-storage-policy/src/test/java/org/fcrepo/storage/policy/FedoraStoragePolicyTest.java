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
package org.fcrepo.storage.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.services.policy.StoragePolicy;
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

/**
 * <p>FedoraStoragePolicyTest class.</p>
 *
 * @author awoods
 */
public class FedoraStoragePolicyTest {

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
        when(mockSessions.getInternalSession()).thenReturn(mockSession);
        when(
                mockJcrTools.findOrCreateNode(mockSession, "/fedora:system/fedora:storage_policy", null))
                .thenReturn(mockCodeNode);
        final Property property = mock(Property.class);
        when(property.getString()).thenReturn("image/tiff");
        when(mockCodeNode.getProperty("image/tiff")).thenReturn(property);
    }

    @Test
    public void nodeCreated() throws Exception {
        mockSession = mockSessions.getInternalSession();
        mockJcrTools.findOrCreateNode(mockSession, "/fedora:system/fedora:storage_policy", null);
        assertEquals(mockCodeNode.getProperty("image/tiff").getString(), "image/tiff");
    }

    @Test
    public void getPolicyType() throws StoragePolicyTypeException {
        final FedoraStoragePolicy obj = new FedoraStoragePolicy();
        final StoragePolicy type = obj.newPolicyInstance("mix:mimeType", "image/tiff", null);
        assertEquals(type.getClass(), MimeTypeStoragePolicy.class);
    }

    @Test
    public void ensureUniquePolicy() {
        /* List<StoragePolicy> can have MimeType("image/tiff", "tiff-store") and MimeType("image/tiff", "cloud-tiffs").
        List<StoragePolicy> can NOT have two MimeType("image/tiff", "tiff-store"); */

        final StoragePolicyDecisionPointImpl obj = new StoragePolicyDecisionPointImpl();
        final StoragePolicy p1 = new MimeTypeStoragePolicy("image/tiff", "tiff-store");
        final StoragePolicy p2 = new MimeTypeStoragePolicy("image/tiff", "tiff-store");
        final StoragePolicy p3 = new MimeTypeStoragePolicy("image/tiff", "tiff-store");
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

        final FedoraStoragePolicy storagePolicy = new FedoraStoragePolicy();
        storagePolicy.setJcrTools(mockJcrTools);
        storagePolicy.setUriInfo(mockUriInfo);
        storagePolicy.request = mockRequest;
        storagePolicy.session = mockSession;
        storagePolicy.storagePolicyDecisionPoint = mockPolicyDecisionPoint;

        final Response response = storagePolicy.post(FedoraStoragePolicy.POLICY_RESOURCE,
                                               "mix:mimeType image/tiff cloud");
        assertNotNull(response);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testGetAllPolicies() throws Exception {
        final FedoraStoragePolicy storagePolicy = new FedoraStoragePolicy();
        final Response response = storagePolicy.get("policies");
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGetPolicy() throws Exception {
        final Value mockValue = mock(Value.class);
        when(mockValue.getString()).thenReturn("image/tiff cloud");
        when(mockProperty.getValues()).thenReturn(new Value[]{mockValue});
        when(mockCodeNode.getProperty("mix:mimeType")).thenReturn(mockProperty);
        when(mockJcrTools.findOrCreateNode(any(Session.class),
                                           anyString(),
                                           anyString())).thenReturn(
                mockCodeNode);

        final FedoraStoragePolicy storagePolicy = new FedoraStoragePolicy();
        storagePolicy.setJcrTools(mockJcrTools);
        storagePolicy.session = mockSession;


        final Response response = storagePolicy.get("mix:mimeType");
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }


}
