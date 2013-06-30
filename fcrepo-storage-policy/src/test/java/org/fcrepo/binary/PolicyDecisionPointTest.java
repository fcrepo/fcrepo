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
package org.fcrepo.binary;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;


public class PolicyDecisionPointTest {

    static PolicyDecisionPoint pt;

    static private String dummyHint;

    static private String tiffHint;

    @BeforeClass
    public static void setupPdp() {
        pt = new PolicyDecisionPoint();

        dummyHint = "dummy-store-id";
        Policy policy = new MimeTypePolicy("image/x-dummy-type", dummyHint);

        pt.addPolicy(policy);

        tiffHint = "tiff-store-id";
        Policy tiffPolicy = new MimeTypePolicy("image/tiff", tiffHint);

        pt.addPolicy(tiffPolicy);
    }

    @Test
    public void testDummyNode() throws Exception {

        Session mockSession = mock(Session.class);
        Node mockRootNode = mock(Node.class);
        Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-dummy-type");
        Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertThat(receivedHint, is(dummyHint));
    }

    @Test
    public void testTiffNode() throws Exception {

        Session mockSession = mock(Session.class);
        Node mockRootNode = mock(Node.class);
        Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/tiff");
        Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertThat(receivedHint, is(tiffHint));
    }

    @Test
    public void testOtherNode() throws Exception {

        Session mockSession = mock(Session.class);
        Node mockRootNode = mock(Node.class);
        Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-other");
        Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertNull(receivedHint);
    }

}
