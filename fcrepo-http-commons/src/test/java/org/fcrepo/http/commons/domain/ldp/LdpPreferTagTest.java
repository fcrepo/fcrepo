/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain.ldp;

import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.fcrepo.http.commons.domain.PreferTag;

import org.junit.jupiter.api.Test;

/**
 * @author cabeer
 */
public class LdpPreferTagTest {

    private LdpPreferTag testObj;

    @Test
    public void testDefaultDecisions() {
        final PreferTag prefer = PreferTag.emptyTag();
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertTrue(testObj.displayContainment());
        assertTrue(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testMinimalContainer() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertFalse(testObj.displayContainment());
        assertFalse(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testPreferMembership() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + " "
                                                                    + PREFER_MEMBERSHIP + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertFalse(testObj.displayContainment());
        assertTrue(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testPreferContainment() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + " "
                                                                    + PREFER_CONTAINMENT + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertTrue(testObj.displayContainment());
        assertFalse(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testPreferContainmentAndMembership() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MEMBERSHIP + " "
                                                                    + PREFER_CONTAINMENT + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertTrue(testObj.displayContainment());
        assertTrue(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testPreferOmitContainmentAndMembership() {
        final PreferTag prefer
                = new PreferTag("return=representation; omit=\"" + PREFER_MEMBERSHIP + " "
                                                                 + PREFER_CONTAINMENT + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertFalse(testObj.displayContainment());
        assertFalse(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testPreferReference() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + INBOUND_REFERENCES + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertTrue(testObj.displayReferences());
        assertTrue(testObj.displayContainment());
        assertTrue(testObj.displayMembership());
        assertFalse(testObj.displayEmbed());
    }

    @Test
    public void testEmbedContained() {
        testObj = new LdpPreferTag(new PreferTag("return=representation; include=\"" + EMBED_CONTAINED + "\""));

        assertTrue(testObj.displayUserRdf());
        assertTrue(testObj.displayServerManaged());
        assertFalse(testObj.displayReferences());
        assertTrue(testObj.displayContainment());
        assertTrue(testObj.displayMembership());
        assertTrue(testObj.displayEmbed());
    }
}
