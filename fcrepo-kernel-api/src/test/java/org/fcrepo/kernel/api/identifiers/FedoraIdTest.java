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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fcrepo.kernel.api.exception.InvalidMementoPathException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.junit.Test;

public class FedoraIdTest {

    @Test
    public void testMissingPrefix() throws Exception {
        final FedoraId fedoraID = FedoraId.create("this-is-a-test/ok");
        final String testID = FEDORA_ID_PREFIX + "/this-is-a-test/ok";
        assertResource(fedoraID, "NORMAL", testID, testID);
    }

    @Test
    public void testRepositoryRoot() throws Exception {
        final FedoraId fedoraID = FedoraId.create(FEDORA_ID_PREFIX);
        assertResource(fedoraID, "ROOT", FEDORA_ID_PREFIX, FEDORA_ID_PREFIX);
    }

    @Test
    public void testNormal() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "NORMAL", testID, testID);
    }

    @Test(expected = InvalidResourceIdentifierException.class)
    public void testEmptyPath() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object//second-object";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testNormalAcl() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "ACL", testID, FEDORA_ID_PREFIX + "/first-object");
    }

    @Test(expected = InvalidResourceIdentifierException.class)
    public void testNormalAclException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL + "/garbage";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testNormalDescription() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "METADATA", testID, FEDORA_ID_PREFIX + "/first-object");
    }

    @Test(expected = InvalidResourceIdentifierException.class)
    public void testNormalDescriptionException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/test-garbage";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testNormalTimemap() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "TIMEMAP", testID, FEDORA_ID_PREFIX + "/first-object");
    }

    @Test
    public void testNormalMemento() throws Exception {
        final String mementoString = "20001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "MEMENTO", testID, FEDORA_ID_PREFIX + "/first-object");
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }

    @Test(expected = InvalidMementoPathException.class)
    public void testNormalMementoException() throws Exception {
        final String mementoString = "00001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString;
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test(expected = InvalidMementoPathException.class)
    public void testNormalMementoException2() throws Exception {
        final String mementoString = "other-text";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString;
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testMetadataAcl() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("ACL", "METADATA"), testID, FEDORA_ID_PREFIX + "/first-object");
    }


    @Test(expected = InvalidResourceIdentifierException.class)
    public void testMetadataAclException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL + "/garbage";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testMetadataTimemap() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("METADATA", "TIMEMAP"), testID, FEDORA_ID_PREFIX +
                "/first-object");
    }

    @Test
    public void testMetadataMemento() throws Exception {
        final String mementoString = "20101109231256";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("METADATA", "MEMENTO"), testID, FEDORA_ID_PREFIX +
                "/first-object");
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }


    @Test
    public void testNormalWithHash() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "HASH", testID, FEDORA_ID_PREFIX + "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testAclWithHash() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "ACL"), testID, FEDORA_ID_PREFIX + "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }


    @Test(expected = InvalidResourceIdentifierException.class)
    public void testAclWithHashException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL + "/garbage#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testMetadataWithHash() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA"), testID, FEDORA_ID_PREFIX + "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMetadataAclWithHash() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA", "ACL"), testID, FEDORA_ID_PREFIX +
                "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test(expected = InvalidResourceIdentifierException.class)
    public void testMetadataAclWithHashException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL + "/garbage#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testTimemapWithHash() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "TIMEMAP"), testID, FEDORA_ID_PREFIX + "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMementoWithHash() throws Exception {
        final String mementoString = "20101109231256";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "MEMENTO"), testID, FEDORA_ID_PREFIX + "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }

    @Test(expected = InvalidMementoPathException.class)
    public void testMementoWithHashException() throws Exception {
        final String mementoString = "00001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test(expected = InvalidMementoPathException.class)
    public void testMementoWithHashException2() throws Exception {
        final String mementoString = "test-garbage";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testMetadataTimemapWithHash() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA", "TIMEMAP"), testID, FEDORA_ID_PREFIX +
                "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
    }

    @Test
    public void testMetadataMementoWithHash() throws Exception {
        final String mementoString = "20101109231256";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, Arrays.asList("HASH", "METADATA", "MEMENTO"), testID, FEDORA_ID_PREFIX +
                "/first-object");
        assertEquals("hashURI", fedoraID.getHashUri());
        assertEquals(mementoString, fedoraID.getMementoString());
        final Instant mementoInstant = Instant.from(MEMENTO_LABEL_FORMATTER.parse(mementoString));
        assertEquals(mementoInstant, fedoraID.getMementoInstant());
    }

    @Test(expected = InvalidMementoPathException.class)
    public void testMetadataMementoWithHashException() throws Exception {
        final String mementoString = "00001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test(expected = InvalidMementoPathException.class)
    public void testMetadataMementoWithHashException1() throws Exception {
        final String mementoString = "test-garbage";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        final FedoraId fedoraID = FedoraId.create(testID);
    }

    @Test
    public void testCreateNull() {
        final FedoraId fedoraID = FedoraId.create(null);
        assertTrue(fedoraID.isRepositoryRoot());
        assertEquals(FEDORA_ID_PREFIX, fedoraID.getFullId());
    }

    @Test
    public void testCreateEmpty() {
        final FedoraId fedoraID = FedoraId.create("");
        assertTrue(fedoraID.isRepositoryRoot());
        assertEquals(FEDORA_ID_PREFIX, fedoraID.getFullId());
    }

    @Test
    public void testStringConcatenation() {
        final FedoraId fedoraID = FedoraId.create("object1", "object2", "object3");
        assertEquals(FEDORA_ID_PREFIX + "/object1/object2/object3", fedoraID.getFullId());
    }

    @Test
    public void testResourceIdAddition() {
        final FedoraId fedoraID = FedoraId.create("core-object", FCR_VERSIONS);
        final FedoraId fedoraId1 = fedoraID.resolve("/" + FCR_ACL);
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_ACL, fedoraId1.getFullId());
    }

    @Test
    public void testFullIdAddition() {
        final FedoraId fedoraID = FedoraId.create("core-object", FCR_VERSIONS);
        final FedoraId fedoraId1 = fedoraID.resolve("20200401101900");
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_VERSIONS + "/20200401101900", fedoraId1.getFullId());
    }

    @Test
    public void testResourceIdAdditionMultiple() {
        final FedoraId fedoraID = FedoraId.create("core-object", FCR_ACL);
        final FedoraId fedoraId1 = fedoraID.resolve("/" + FCR_VERSIONS, "20200401110400");
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_VERSIONS + "/20200401110400", fedoraId1.getFullId());
    }

    @Test
    public void testFullIdAdditionMultiple() {
        final FedoraId fedoraID = FedoraId.create("core-object", FCR_METADATA);
        final FedoraId fedoraId1 = fedoraID.resolve(FCR_VERSIONS, "20200401110400");
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401110400",
                fedoraId1.getFullId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveBlank() {
        final FedoraId fedoraId = FedoraId.create("core-object");
        fedoraId.resolve();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveEmptyString() {
        final FedoraId fedoraId = FedoraId.create("core-object");
        fedoraId.resolve("");
    }


    /**
     * Utility to test a FedoraId against expectations.
     * @param fedoraID The FedoraId object to test.
     * @param type A string type of "ROOT", "ACL", "METADATA", "TIMEMAP", "MEMENTO" and "HASH"
     * @param fullID The expected full ID.
     * @param shortID The expected short ID.
     */
    private void assertResource(final FedoraId fedoraID, final String type, final String fullID, final String shortID) {
        assertResource(fedoraID, Collections.singletonList(type), fullID, shortID);
    }

    /**
     * Utility to test a FedoraId against expectations.
     * @param fedoraID The FedoraId object to test.
     * @param type A list of string type of "ROOT", "ACL", "METADATA", "TIMEMAP", "MEMENTO" and "HASH"
     * @param fullID The expected full ID.
     * @param shortID The expected short ID.
     */
    private void assertResource(final FedoraId fedoraID, final List<String> type, final String fullID,
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
