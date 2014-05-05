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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.services.policy.StoragePolicy;
import org.junit.BeforeClass;
import org.junit.Test;

public class StoragePolicyDecisionPointTest {

    static StoragePolicyDecisionPoint pt;

    static private String dummyHint;

    static private String tiffHint;

    @BeforeClass
    public static void setupPdp() {
        pt = new StoragePolicyDecisionPointImpl();

        dummyHint = "dummy-store-id";
        final StoragePolicy policy =
                new MimeTypeStoragePolicy("image/x-dummy-type", dummyHint);

        pt.addPolicy(policy);

        tiffHint = "tiff-store-id";
        final StoragePolicy tiffPolicy = new MimeTypeStoragePolicy("image/tiff", tiffHint);

        pt.addPolicy(tiffPolicy);
    }

    @Test
    public void testDummyNode() throws Exception {

        final Session mockSession = mock(Session.class);
        final Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-dummy-type");
        final Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        final String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertThat("Received hint didn't match dummy hint!", receivedHint,
                is(dummyHint));
    }

    @Test
    public void testTiffNode() throws Exception {

        final Session mockSession = mock(Session.class);
        final Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/tiff");
        final Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        final String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertThat(receivedHint, is(tiffHint));
    }

    @Test
    public void testOtherNode() throws Exception {

        final Session mockSession = mock(Session.class);
        final Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-other");
        final Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).thenReturn(
                mockProperty);

        final String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertNull(receivedHint);
    }

}
