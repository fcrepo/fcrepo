/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.http.commons.domain.ldp;

import org.fcrepo.http.commons.domain.PreferTag;
import org.junit.Test;

import java.text.ParseException;

import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 */
public class LdpPreferTagTest {

    private LdpPreferTag testObj;


    @Test
    public void testMinimalHandling() throws ParseException {
        testObj = new LdpPreferTag(new PreferTag("handling=lenient; received=\"minimal\""));

        assertFalse(testObj.prefersServerManaged());
        assertFalse(testObj.prefersContainment());
        assertFalse(testObj.prefersMembership());
        assertFalse(testObj.prefersEmbed());
        assertFalse(testObj.prefersReferences());

    }

    @Test
    public void testMinimalContainer() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + LDP_NAMESPACE + "PreferMinimalContainer\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersServerManaged());
        assertFalse(testObj.prefersReferences());
        assertFalse(testObj.prefersContainment());
        assertFalse(testObj.prefersMembership());
        assertFalse(testObj.prefersEmbed());
    }

    @Test
    public void testPreferMembership() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + LDP_NAMESPACE + "PreferMinimalContainer "
                                                                    + LDP_NAMESPACE + "PreferMembership\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersMembership());
    }

    @Test
    public void testPreferContainment() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + LDP_NAMESPACE + "PreferMinimalContainer "
                                                                    + LDP_NAMESPACE + "PreferContainment\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersContainment());
    }

    @Test
    public void testPreferContainmentAndMembership() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + LDP_NAMESPACE + "PreferMembership "
                                                                    + LDP_NAMESPACE + "PreferContainment\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersMembership());
        assertTrue(testObj.prefersContainment());
    }

    @Test
    public void testPreferOmitContainmentAndMembership() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; omit=\"" + LDP_NAMESPACE + "PreferMembership "
                                                                 + LDP_NAMESPACE + "PreferContainment\"");
        testObj = new LdpPreferTag(prefer);

        assertFalse(testObj.prefersMembership());
        assertFalse(testObj.prefersContainment());
    }

    @Test
    public void testPreferEmbed() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + EMBED_CONTAINS + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersEmbed());
    }

    @Test
    public void testPreferReference() throws ParseException {
        final PreferTag prefer
                = new PreferTag("return=representation; include=\"" + INBOUND_REFERENCES + "\"");
        testObj = new LdpPreferTag(prefer);

        assertTrue(testObj.prefersReferences());
    }

}
