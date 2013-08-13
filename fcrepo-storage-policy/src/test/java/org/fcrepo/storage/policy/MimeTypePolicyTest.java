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

import org.fcrepo.kernel.services.policy.Policy;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

public class MimeTypePolicyTest {

    @Test
    public void shouldEvaluatePolicyAndReturnHint() throws Exception {
        final String hint = "store-id";
        final Policy policy = new MimeTypePolicy("image/x-dummy", hint);

        final Session mockSession = mock(Session.class);
        final Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-dummy");
        final Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        final String receivedHint = policy.evaluatePolicy(mockDsNode);

        assertThat(receivedHint, is(hint));
    }

    @Test
    public void shouldEvaluatePolicyAndReturnNoHint() throws Exception {
        final String hint = "store-id";
        final Policy policy = new MimeTypePolicy("image/x-dummy", hint);

        final Session mockSession = mock(Session.class);
        final Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("application/x-other");
        final Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        final String receivedHint = policy.evaluatePolicy(mockDsNode);

        assertNull(receivedHint);
    }

    @Test
    public void shouldEvaluatePolicyAndReturnNoHintOnException()
            throws Exception {
        final String hint = "store-id";
        final Policy policy = new MimeTypePolicy("image/x-dummy", hint);

        final Node mockDsNode = mock(Node.class);

        when(mockDsNode.getNode(JCR_CONTENT)).thenThrow(
                new RepositoryException());

        final String receivedHint = policy.evaluatePolicy(mockDsNode);

        assertNull("Received hint was not null!", receivedHint);
    }
    
    // Test using equals. As impl. of <Policy> involves, this may change
    @Test
    public void testEquals() {
        MimeTypePolicy obj1 = new MimeTypePolicy("image/tiff", "tiff-store");
        MimeTypePolicy obj2 = new MimeTypePolicy("image/tiff", "tiff-store");
        assertEquals(obj1,obj2);
    }   
    
}
