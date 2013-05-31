/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.binary;

import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Apr 25, 2013
 */
public class PolicyDecisionPointTest {
    static PolicyDecisionPoint pt;
    static private String dummyHint;
    static private String tiffHint;

    /**
     * @todo Add Documentation.
     */
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

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testDummyNode() throws Exception {

        Session mockSession = mock(Session.class);
        Node mockRootNode = mock(Node.class);
        Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-dummy-type");
        Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).
            thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).
            thenReturn(mockProperty);

        String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertThat(receivedHint, is(dummyHint));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testTiffNode() throws Exception {

        Session mockSession = mock(Session.class);
        Node mockRootNode = mock(Node.class);
        Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/tiff");
        Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).
            thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).
            thenReturn(mockProperty);

        String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertThat(receivedHint, is(tiffHint));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testOtherNode() throws Exception {

        Session mockSession = mock(Session.class);
        Node mockRootNode = mock(Node.class);
        Node mockDsNode = mock(Node.class);

        when(mockDsNode.getSession()).thenReturn(mockSession);
        Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn("image/x-other");
        Node mockContentNode = mock(Node.class);
        when(mockDsNode.getNode(JcrConstants.JCR_CONTENT)).
            thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_MIME_TYPE)).
            thenReturn(mockProperty);

        String receivedHint = pt.evaluatePolicies(mockDsNode);
        assertNull(receivedHint);
    }

}
