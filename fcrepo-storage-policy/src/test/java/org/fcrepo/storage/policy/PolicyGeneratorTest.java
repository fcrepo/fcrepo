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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.policy.Policy;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

public class PolicyGeneratorTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private SessionFactory mockSessions;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode;

    @Mock
    private Node mockCodeNode;

    @Mock
    NodeService mockNodeService;

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
        PolicyGenerator obj = new PolicyGenerator();
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
    
    

}