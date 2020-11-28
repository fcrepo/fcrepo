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

import org.fcrepo.http.commons.domain.PreferTag;
import org.junit.Test;

import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 */
public class LdpPreferTagTest {

    private LdpPreferTag testObj;


    @Test
    public void testMinimalHandling() {
        testObj = new LdpPreferTag(new PreferTag("handling=lenient; received=\"minimal\""));

        assertFalse(testObj.prefersServerManaged());
        assertFalse(testObj.prefersContainment());
        assertFalse(testObj.prefersMembership());
        assertFalse(testObj.prefersEmbed());
        assertFalse(testObj.prefersReferences());

    }

    @Test
    public void testMinimalContainer() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersServerManaged());
        assertFalse(testObj.prefersReferences());
        assertFalse(testObj.prefersContainment());
        assertFalse(testObj.prefersMembership());
        assertFalse(testObj.prefersEmbed());
    }

    @Test
    public void testPreferMembership() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + " "
                                                                    + PREFER_MEMBERSHIP + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersMembership());
    }

    @Test
    public void testPreferContainment() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + " "
                                                                    + PREFER_CONTAINMENT + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersContainment());
    }

    @Test
    public void testPreferContainmentAndMembership() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + PREFER_MEMBERSHIP + " "
                                                                    + PREFER_CONTAINMENT + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersMembership());
        assertTrue(testObj.prefersContainment());
    }

    @Test
    public void testPreferOmitContainmentAndMembership() {
        final PreferTag prefer
                = new PreferTag("return=representation; omit=\"" + PREFER_MEMBERSHIP + " "
                                                                 + PREFER_CONTAINMENT + "\"");
        testObj = new LdpPreferTag(prefer);

        assertFalse(testObj.prefersMembership());
        assertFalse(testObj.prefersContainment());
    }

    @Test
    public void testPreferReference() {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + INBOUND_REFERENCES + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersReferences());
    }

    @Test
    public void testEmbedDefault() {
        testObj = new LdpPreferTag(PreferTag.emptyTag());
        assertFalse(testObj.prefersEmbed());
    }

    @Test
    public void testEmbedContained() {
        testObj = new LdpPreferTag(new PreferTag("return=representation; include=\"" + EMBED_CONTAINED + "\""));
        assertTrue(testObj.prefersEmbed());
    }
}
