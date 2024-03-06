/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.identifiers;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.exception.InvalidMementoPathException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;

import org.junit.jupiter.api.Test;

/**
 * @author pwinckles
 */
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

    @Test
    public void testEmptyPath() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object//second-object";
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
    }

    @Test
    public void testNormalAcl() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "ACL", testID, FEDORA_ID_PREFIX + "/first-object");
    }

    @Test
    public void testNormalAclException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL + "/garbage";
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
    }

    @Test
    public void testNormalDescription() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA;
        final FedoraId fedoraID = FedoraId.create(testID);
        assertResource(fedoraID, "METADATA", testID, FEDORA_ID_PREFIX + "/first-object");
    }

    @Test
    public void testNormalDescriptionException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/test-garbage";
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
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

    @Test
    public void testNormalMementoException() throws Exception {
        final String mementoString = "00001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString;
        assertThrows(InvalidMementoPathException.class, () ->FedoraId.create(testID));
    }

    @Test
    public void testNormalMementoException2() throws Exception {
        final String mementoString = "other-text";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString;
        assertThrows(InvalidMementoPathException.class, () ->FedoraId.create(testID));
    }

    @Test
    public void testMetadataAcl() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL;
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
    }


    @Test
    public void testMetadataAclException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL + "/garbage";
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
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


    @Test
    public void testAclWithHashException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_ACL + "/garbage#hashURI";
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
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
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
    }

    @Test
    public void testMetadataAclWithHashException() throws Exception {
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_ACL + "/garbage#hashURI";
        assertThrows(InvalidResourceIdentifierException.class, () -> FedoraId.create(testID));
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

    @Test
    public void testMementoWithHashException() throws Exception {
        final String mementoString = "00001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString + "#hashURI";
        assertThrows(InvalidMementoPathException.class, () -> FedoraId.create(testID));
    }

    @Test
    public void testMementoWithHashException2() throws Exception {
        final String mementoString = "test-garbage";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_VERSIONS + "/" + mementoString + "#hashURI";
        assertThrows(InvalidMementoPathException.class, () -> FedoraId.create(testID));
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

    @Test
    public void testMetadataMementoWithHashException() throws Exception {
        final String mementoString = "00001221010304";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        assertThrows(InvalidMementoPathException.class, () -> FedoraId.create(testID));
    }

    @Test
    public void testMetadataMementoWithHashException1() throws Exception {
        final String mementoString = "test-garbage";
        final String testID = FEDORA_ID_PREFIX + "/first-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/" +
                mementoString + "#hashURI";
        assertThrows(InvalidMementoPathException.class, () -> FedoraId.create(testID));
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
        final FedoraId fedoraId1 = fedoraID.asAcl();
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_ACL, fedoraId1.getFullId());
    }

    @Test
    public void testFullIdAddition() {
        final FedoraId fedoraID = FedoraId.create("core-object");
        final FedoraId fedoraId1 = fedoraID.asMemento("20200401101900");
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_VERSIONS + "/20200401101900", fedoraId1.getFullId());
    }

    @Test
    public void testResourceIdAdditionMultiple() {
        final FedoraId fedoraID = FedoraId.create("core-object", FCR_ACL);
        final FedoraId fedoraId1 = fedoraID.asMemento("20200401110400");
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_VERSIONS + "/20200401110400", fedoraId1.getFullId());
    }

    @Test
    public void testFullIdAdditionMultiple() {
        final FedoraId fedoraID = FedoraId.create("core-object", FCR_METADATA);
        final FedoraId fedoraId1 = fedoraID.asMemento("20200401110400");
        assertEquals(FEDORA_ID_PREFIX + "/core-object/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401110400",
                fedoraId1.getFullId());
    }

    @Test
    public void testResolveBlank() {
        final FedoraId fedoraId = FedoraId.create("core-object");
        assertThrows(IllegalArgumentException.class, () -> fedoraId.resolve(null));
    }

    @Test
    public void testResolveEmptyString() {
        final FedoraId fedoraId = FedoraId.create("core-object");
        assertThrows(IllegalArgumentException.class, () -> fedoraId.resolve(""));
    }

    @Test
    public void testDoubleAcl() {
        assertThrows(InvalidResourceIdentifierException.class,
                () -> FedoraId.create("core-object/" + FCR_ACL + "/" + FCR_ACL));
    }

    @Test
    public void testAsMemento() {
        assertAsMemento("original/" + FCR_METADATA + "/" + FCR_VERSIONS,
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101900");
        assertAsMemento("original/" + FCR_METADATA + "#blah",
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101900#blah");
        assertAsMemento("original/" + FCR_VERSIONS,
                "original/" + FCR_VERSIONS + "/20200401101900");
        assertAsMemento("original/" + FCR_TOMBSTONE,
                "original/" + FCR_VERSIONS + "/20200401101900");
        assertAsMemento("original/" + FCR_ACL,
                "original/" + FCR_VERSIONS + "/20200401101900");
        assertAsMemento("original/" + FCR_VERSIONS + "/20200401101901",
                "original/" + FCR_VERSIONS + "/20200401101901");
        assertAsMemento("original/child",
                "original/child/" + FCR_VERSIONS + "/20200401101900");
        assertAsMemento("original/child#sad",
                "original/child/" + FCR_VERSIONS + "/20200401101900#sad");
    }

    @Test
    public void testAsTimemap() {
        assertAsTimemap("original/" + FCR_METADATA + "/" + FCR_VERSIONS,
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS);
        assertAsTimemap("original/" + FCR_METADATA + "#blah",
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS);
        assertAsTimemap("original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101901",
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS);
        assertAsTimemap("original/" + FCR_VERSIONS,
                "original/" + FCR_VERSIONS);
        assertAsTimemap("original/" + FCR_TOMBSTONE,
                "original/" + FCR_VERSIONS);
        assertAsTimemap("original/" + FCR_ACL,
                "original/" + FCR_VERSIONS);
        assertAsTimemap("original/" + FCR_VERSIONS + "/20200401101900",
                "original/" + FCR_VERSIONS);
        assertAsTimemap("original/child",
                "original/child/" + FCR_VERSIONS);
        assertAsTimemap("original/child#sad",
                "original/child/" + FCR_VERSIONS);
    }

    @Test
    public void testAsDescription() {
        assertAsDescription("original/" + FCR_METADATA + "/" + FCR_VERSIONS,
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS);
        assertAsDescription("original/" + FCR_METADATA + "#blah",
                "original/" + FCR_METADATA + "#blah");
        assertAsDescription("original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101901",
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101901");
        assertAsDescription("original/" + FCR_VERSIONS,
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS);
        assertAsDescription("original/" + FCR_TOMBSTONE,
                "original/" + FCR_METADATA);
        assertAsDescription("original/" + FCR_ACL,
                "original/" + FCR_METADATA);
        assertAsDescription("original/" + FCR_VERSIONS + "/20200401101900",
                "original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101900");
        assertAsDescription("original/child",
                "original/child/" + FCR_METADATA);
        assertAsDescription("original/child#sad",
                "original/child/" + FCR_METADATA + "#sad");
    }

    @Test
    public void testAsTombstone() {
        final var tombstone = "original/" + FCR_TOMBSTONE;
        assertAsTombstone("original/" + FCR_METADATA + "/" + FCR_VERSIONS, tombstone);
        assertAsTombstone("original/" + FCR_METADATA + "#blah", tombstone);
        assertAsTombstone("original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101901", tombstone);
        assertAsTombstone("original/" + FCR_VERSIONS, tombstone);
        assertAsTombstone("original/" + FCR_TOMBSTONE, tombstone);
        assertAsTombstone("original/" + FCR_ACL, tombstone);
        assertAsTombstone("original/" + FCR_VERSIONS + "/20200401101900", tombstone);
        assertAsTombstone("original/child", "original/child/" + FCR_TOMBSTONE);
        assertAsTombstone("original/child#sad", "original/child/" + FCR_TOMBSTONE);
    }

    @Test
    public void testAsAcl() {
        final var acl = "original/" + FCR_ACL;
        assertAsAcl("original/" + FCR_METADATA + "/" + FCR_VERSIONS, acl);
        assertAsAcl("original/" + FCR_METADATA + "#blah", acl);
        assertAsAcl("original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101901", acl);
        assertAsAcl("original/" + FCR_VERSIONS, acl);
        assertAsAcl("original/" + FCR_TOMBSTONE, acl);
        assertAsAcl("original/" + FCR_ACL, acl);
        assertAsAcl("original/" + FCR_VERSIONS + "/20200401101900", acl);
        assertAsAcl("original/child", "original/child/" + FCR_ACL);
        assertAsAcl("original/child#sad", "original/child/" + FCR_ACL);
    }

    @Test
    public void testEncodedId() {
        // Slashes are not encoded.
        final String normal = "/object/child";
        final var normalId = FedoraId.create(normal);
        assertEquals(FEDORA_ID_PREFIX + normal, normalId.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + normal, normalId.getFullId());
        // Colons are not encoded.
        final String withColon = "/object/test:prefix";
        final var withColonId = FedoraId.create(withColon);
        assertEquals(FEDORA_ID_PREFIX + withColon, withColonId.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + withColon, withColonId.getFullId());
        // Hashes are not encoded.
        final String withHash = "/object/with#hash";
        final var withHashId = FedoraId.create(withHash);
        assertEquals(FEDORA_ID_PREFIX + withHash, withHashId.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + withHash, withHashId.getFullId());
        // Query string characters (? & =) are not encoded.
        final String withQueryString = "/object/with?query=parameters&for=uris";
        final var withQueryStringId = FedoraId.create(withQueryString);
        assertEquals(FEDORA_ID_PREFIX + withQueryString, withQueryStringId.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + withQueryString, withQueryStringId.getFullId());
        // Plus and minus (hyphens) signs are not encoded
        final String plus_sign = "/object/one+two/three-four";
        final var plus_signId = FedoraId.create(plus_sign);
        assertEquals(FEDORA_ID_PREFIX + plus_sign, plus_signId.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + plus_sign, plus_signId.getFullId());
        // Remaining URI sub-deliminators are not encoded
        final String sub_delims = "/object/with!/some$weird/possibly'unstable/group(ing)/all*/comma,comma/and;this";
        final var sub_delimsId = FedoraId.create(sub_delims);
        assertEquals(FEDORA_ID_PREFIX + sub_delims, sub_delimsId.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + sub_delims, sub_delimsId.getFullId());

        // Spaces ARE encoded.
        final String with_space = "/object/has a space";
        final var with_space_id = FedoraId.create(with_space);
        assertEquals(FEDORA_ID_PREFIX + "/object/has%20a%20space", with_space_id.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + with_space, with_space_id.getFullId());

        // Encoded spaces are double encoded
        final String with_encoded_space = "/object/has%20a%20space";
        final var with_encoded_space_id = FedoraId.create(with_encoded_space);
        assertEquals(FEDORA_ID_PREFIX + "/object/has%2520a%2520space", with_encoded_space_id.getEncodedFullId());
        assertEquals(FEDORA_ID_PREFIX + with_encoded_space, with_encoded_space_id.getFullId());
    }

    @Test
    public void verifyStorageNamingRestrictions() {
        assertIdStringConstraint(".fcrepo");
        assertIdStringConstraint("fcr-root");
        assertIdStringConstraint("fcr-container.nt");

        assertIdSuffixConstraint("~fcr-desc");
        assertIdSuffixConstraint("~fcr-acl");
        assertIdSuffixConstraint("~fcr-desc.nt");
        assertIdSuffixConstraint("~fcr-acl.nt");
    }

    @Test
    public void testAsDescribedId() {
        assertAsDescribedId("original" + "/" + FCR_METADATA + "/" + FCR_VERSIONS,
                "original/" + FCR_VERSIONS);
        assertAsDescribedId("original/" + FCR_METADATA + "#blah",
                "original" + "#blah");
        assertAsDescribedId("original/" + FCR_METADATA + "/" + FCR_VERSIONS + "/20200401101901",
                "original/" + FCR_VERSIONS + "/20200401101901");
        assertAsDescribedId("original/" + FCR_VERSIONS,
                "original/" + FCR_VERSIONS);
        assertAsDescribedId("original/" + FCR_VERSIONS + "/20200401101900",
                "original/" + FCR_VERSIONS + "/20200401101900");
        assertAsDescribedId("original/" + FCR_TOMBSTONE,
                "original/" + FCR_TOMBSTONE);
        assertAsDescribedId("original/" + FCR_ACL,
                "original/" + FCR_ACL);
        assertAsDescribedId("original/child/" + FCR_METADATA,
                "original/child");
        assertAsDescribedId("original/child/" + FCR_METADATA + "#sad",
                "original/child#sad");
    }

    private void assertAsMemento(final String original, final String expected) {
        final var id = FedoraId.create(original);
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expected,
                id.asMemento("20200401101900").getFullId());
    }

    private void assertAsTimemap(final String original, final String expected) {
        final var id = FedoraId.create(original);
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expected,
                id.asTimemap().getFullId());
    }

    private void assertAsTombstone(final String original, final String expected) {
        final var id = FedoraId.create(original);
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expected,
                id.asTombstone().getFullId());
    }

    private void assertAsAcl(final String original, final String expected) {
        final var id = FedoraId.create(original);
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expected,
                id.asAcl().getFullId());
    }

    private void assertAsDescription(final String original, final String expected) {
        final var id = FedoraId.create(original);
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expected,
                id.asDescription().getFullId());
    }

    private void assertAsDescribedId(final String original, final String expected) {
        final var id = FedoraId.create(original);
        assertEquals(FEDORA_ID_PREFIX + "/" + expected,
                id.getFullDescribedId());
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
        assertEquals(shortID, fedoraID.getBaseId());
    }

    private void assertIdStringConstraint(final String id) {
        assertInvalidId(id);
        FedoraId.create(id + "-suffix");
        FedoraId.create("prefix-" + id);
    }

    private void assertIdSuffixConstraint(final String suffix) {
        assertInvalidId("prefix" + suffix);
        FedoraId.create(suffix);
        FedoraId.create(suffix + "-suffix");
    }

    private void assertInvalidId(final String id) {
        try {
            FedoraId.create(id);
            fail("should have thrown an exception creating id: " + id);
        } catch (final InvalidResourceIdentifierException e) {
            // expected exception
        }
    }

}
