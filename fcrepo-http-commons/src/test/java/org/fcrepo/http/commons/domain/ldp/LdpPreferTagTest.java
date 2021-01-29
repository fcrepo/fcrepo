/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.commons.domain.ldp;

import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.fcrepo.http.commons.domain.PreferTag;

import org.junit.Test;

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
