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
package org.fcrepo.kernel.api.identifiers;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FedoraIDTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidID() {
        final FedoraID fedoraID = new FedoraID("this-is-a-test/ok");
    }

    @Test
    public void testRepositoryRoot() {
        final FedoraID fedoraID = new FedoraID(FEDORA_ID_PREFIX);
        assertResource(fedoraID, "ROOT", FEDORA_ID_PREFIX, FEDORA_ID_PREFIX);
    }

    @Test
    public void testNormal() {
        final String testID = FEDORA_ID_PREFIX + "first-object";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, "NORMAL", testID, testID);
    }

    @Test
    public void testNormalAcl() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_ACL;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, "ACL", testID, FEDORA_ID_PREFIX + "first-object");
    }

    @Test
    public void testNormalDescription() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, "METADATA", testID, FEDORA_ID_PREFIX + "first-object");
    }

    @Test
    public void testNormalTimemap() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_VERSIONS;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, "TIMEMAP", testID, FEDORA_ID_PREFIX + "first-object");
    }

    @Test
    public void testNormalMemento() {
        final String mementoString = "20001221010304";
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_VERSIONS + "/" + mementoString;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, "MEMENTO", testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }

    @Test
    public void testMetadataAcl() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "/" + FCR_ACL;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("ACL", "METADATA"), testID, FEDORA_ID_PREFIX + "first-object");
    }

    @Test
    public void testMetadataTimemap() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "/" + FCR_VERSIONS;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("METADATA", "TIMEMAP"), testID, FEDORA_ID_PREFIX + "first-object");
    }

    @Test
    public void testMetadataMemento() {
        final String mementoString = "20101109231256";
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString;
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("METADATA", "MEMENTO"), testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }


    @Test
    public void testNormalWithHash() {
        final String testID = FEDORA_ID_PREFIX + "first-object#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, "HASH", testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testAclWithHash() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_ACL + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "ACL"), testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMetadataWithHash() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA"), testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMetadataAclWithHash() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "/" + FCR_ACL + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA", "ACL"), testID, FEDORA_ID_PREFIX +
                "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testTimemapWithHash() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_VERSIONS + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "TIMEMAP"), testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMementoWithHash() {
        final String mementoString = "20101109231256";
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "MEMENTO"), testID, FEDORA_ID_PREFIX + "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }

    @Test
    public void testMetadataTimemapWithHash() {
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA", "TIMEMAP"), testID, FEDORA_ID_PREFIX +
                "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMetadataMementoWithHash() {
        final String mementoString = "20101109231256";
        final String testID = FEDORA_ID_PREFIX + "first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        final FedoraID fedoraID = new FedoraID(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA", "MEMENTO"), testID, FEDORA_ID_PREFIX +
                "first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }


    /**
     * Utility to test a FedoraID against expectations.
     * @param fedoraID The FedoraID object to test.
     * @param type A string type of "ROOT", "ACL", "METADATA", "TIMEMAP", "MEMENTO" and "HASH"
     * @param fullID The expected full ID.
     * @param shortID The expected short ID.
     */
    private void assertResource(final FedoraID fedoraID, final String type, final String fullID, final String shortID) {
        assertResource(fedoraID, Collections.singletonList(type), fullID, shortID);
    }

    /**
     * Utility to test a FedoraID against expectations.
     * @param fedoraID The FedoraID object to test.
     * @param type A list of string type of "ROOT", "ACL", "METADATA", "TIMEMAP", "MEMENTO" and "HASH"
     * @param fullID The expected full ID.
     * @param shortID The expected short ID.
     */
    private void assertResource(final FedoraID fedoraID, final List<String> type, final String fullID,
                                final String shortID) {
        if (type.contains("ROOT")) {
            assertTrue(fedoraID.isRepositoryRoot());
        } else {
            assertFalse(fedoraID.isRepositoryRoot());
        }
        if (type.contains("ACL")) {
            assertTrue(fedoraID.isAcl());
        } else {
            assertFalse(fedoraID.isAcl());
        }
        if (type.contains("METADATA")) {
            assertTrue(fedoraID.isDescription());
        } else {
            assertFalse(fedoraID.isDescription());
        }
        if (type.contains("HASH")) {
            assertTrue(fedoraID.isHashUri());
        } else {
            assertFalse(fedoraID.isHashUri());
        }
        if (type.contains("MEMENTO")) {
            assertTrue(fedoraID.isMemento());
        } else {
            assertFalse(fedoraID.isMemento());
        }
        if (type.contains("TIMEMAP")) {
            assertTrue(fedoraID.isTimemap());
        } else {
            assertFalse(fedoraID.isTimemap());
        }
        assertEquals(fullID, fedoraID.getFullId());
        assertEquals(shortID, fedoraID.getResourceId());
    }
}
