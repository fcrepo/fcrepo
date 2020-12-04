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
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fcrepo.kernel.api.exception.InvalidMementoPathException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

/**
 * Class to store contextual information about a Fedora ID.
 *
 * Differentiates between the original ID of the request and the actual resource we are operating on.
 *
 * Resource Id : the shortened ID of the base resource, mostly needed to access the correct persistence object.
 * fullId : the full ID from the request, used in most cases.
 *
 * So a fullId of info:fedora/object1/another/fcr:versions/20000101121212 has an id of info:fedora/object1/another
 *
 * @author whikloj
 * @since 6.0.0
 */
public class FedoraId {

    /**
     * These are strings that can cause problems with our storage layout
     */
    private static final Set<String> FORBIDDEN_ID_PART_STRINGS = Set.of(
            "fcr-root",
            ".fcrepo",
            "fcr-container.nt"
    );
    private static final Set<String> FORBIDDEN_ID_PART_SUFFIXES = Set.of(
            "~fcr-desc",
            "~fcr-acl",
            "~fcr-desc.nt",
            "~fcr-acl.nt"
    );

    /**
     * The Fedora ID with prefix and extensions. eg info:fedora/object1/another/fcr:versions/20000101121212
     */
    private final String fullId;

    /**
     * The Fedora ID with prefix but without extensions. eg info:fedora/object1/another
     */
    private final String baseId;

    /**
     * The Fedora ID without prefix but with extensions. eg /object1/another/fcr:versions/20000101121212
     */
    private final String fullPath;

    /**
     * The Fedora ID prefix and extensions URL encoded.
     */
    private final String encodedFullId;

    private String hashUri;
    private boolean isRepositoryRoot = false;
    private boolean isNonRdfSourceDescription = false;
    private boolean isAcl = false;
    private boolean isMemento = false;
    private boolean isTimemap = false;
    private boolean isTombstone = false;
    private Instant mementoDatetime;
    private String mementoDatetimeStr;

    private final static Set<Pattern> extensions = Set.of(FCR_TOMBSTONE, FCR_METADATA, FCR_ACL, FCR_VERSIONS)
            .stream().map(Pattern::compile).collect(Collectors.toSet());

    private final static Escaper fedoraIdEscaper = new PercentEscaper("-._~!$'()*,;&=@:+/?#", false);

    /**
     * Basic constructor.
     * @param fullId The full identifier or null if root.
     * @throws IllegalArgumentException If ID does not start with expected prefix.
     */
    private FedoraId(final String fullId) {
        this.fullId = ensurePrefix(fullId).replaceAll("/+$", "");
        // Carry the path of the request for any exceptions.
        this.fullPath = this.fullId.substring(FEDORA_ID_PREFIX.length());
        checkForInvalidPath();
        this.baseId = processIdentifier();
        enforceStorageLayoutNamingConstraints();
        this.encodedFullId = fedoraIdEscaper.escape(this.fullId);
    }

    /**
     * Static create method
     * @param additions One or more strings to build an ID.
     * @return The FedoraId.
     */
    @JsonCreator
    public static FedoraId create(final String... additions) {
        return new FedoraId(idBuilder(additions));
    }

    /**
     * Get a FedoraId for repository root.
     * @return The FedoraId for repository root.
     */
    public static FedoraId getRepositoryRootId() {
        return new FedoraId(null);
    }

    /**
     * Is the identifier for the repository root.
     * @return true of id is equal to info:fedora/
     */
    public boolean isRepositoryRoot() {
        return isRepositoryRoot;
    }

    /**
     * Is the identifier for a Memento?
     * @return true if the id is for the fcr:versions endpoint and has a memento datetime string after it.
     */
    public boolean isMemento() {
        return isMemento;
    }

    /**
     * Is the identifier for an ACL?
     * @return true if the id is for the fcr:acl endpoint.
     */
    public boolean isAcl() {
        return isAcl;
    }

    /**
     * Is the identifier for a timemap?
     * @return true if id for the fcr:versions endpoint and NOT a memento.
     */
    public boolean isTimemap() {
        return isTimemap;
    }

    /**
     * Is the identifier for a nonRdfSourceDescription?
     * @return true if id for the fcr:metadata endpoint
     */
    public boolean isDescription() {
        return isNonRdfSourceDescription;
    }

    /**
     * Is the identifier for a tombstone
     * @return true if id for the fcr:tombstone endpoint
     */
    public boolean isTombstone() {
        return isTombstone;
    }

    /**
     * Is the identifier for a hash uri?
     * @return true if full id referenced a hash uri.
     */
    public boolean isHashUri() {
        return hashUri != null;
    }

    /**
     * Get the hash uri.
     * @return the hash uri from the id or null if none.
     */
    public String getHashUri() {
        return hashUri;
    }

    /**
     * Returns the ID string for the physical resource the Fedora ID describes. In most cases, this ID is the same as
     * the full resource ID. However, if the resource is a memento, timemap, or tombstone, then the ID returned here
     * will be for the resource that contains it. Here are some examples:
     *
     * <ul>
     *     <li>"info:fedora/object1/another/fcr:versions/20000101121212" =&gt; "info:fedora/object1/another"</li>
     *     <li>"info:fedora/object1/another/fcr:metadata" =&gt; "info:fedora/object1/another/fcr:metadata"</li>
     *     <li>"info:fedora/object1/another" =&gt; "info:fedora/object1/another"</li>
     * </ul>
     *
     * @return the ID of the associated physical resource
     */
    public String getResourceId() {
        if (isNonRdfSourceDescription) {
            return baseId + "/" + FCR_METADATA;
        } else if (isAcl) {
            return baseId + "/" + FCR_ACL;
        }
        return baseId;
    }

    /**
     * Behaves the same as {@link #getResourceId()} except it returns a FedoraId rather than a String.
     *
     * @return the ID of the associated physical resource
     */
    public FedoraId asResourceId() {
        return FedoraId.create(getResourceId());
    }

    /**
     * Returns the ID string for the base ID the Fedora ID describes. This value is the equivalent of the full ID
     * with all extensions removed.
     *
     * <ul>
     *     <li>"info:fedora/object1/another/fcr:versions/20000101121212" =&gt; "info:fedora/object1/another"</li>
     *     <li>"info:fedora/object1/another/fcr:metadata" =&gt; "info:fedora/object1/another"</li>
     *     <li>"info:fedora/object1/another" =&gt; "info:fedora/object1/another"</li>
     * </ul>
     *
     * @return the ID of the associated base resource
     */
    public String getBaseId() {
        return baseId;
    }

    /**
     * Behaves the same as {@link #getBaseId()} except it returns a FedoraId rather than a String.
     *
     * @return the ID of the associated base resource
     */
    public FedoraId asBaseId() {
        return FedoraId.create(getBaseId());
    }

    /**
     * Return the original full ID.
     * @return the id.
     */
    public String getFullId() {
        return fullId;
    }

    /**
     * Return the original full ID without the info:fedora prefix.
     * @return the full id path part
     */
    public String getFullIdPath() {
        return fullPath;
    }

    /**
     * @return The encoded full ID.
     */
    public String getEncodedFullId() {
        return encodedFullId;
    }

    /**
     * Return the Memento datetime as Instant.
     * @return The datetime or null if not a memento.
     */
    public Instant getMementoInstant() {
        return mementoDatetime;
    }

    /**
     * Return the Memento datetime string.
     * @return The yyyymmddhhiiss memento datetime or null if not a Memento.
     */
    public String getMementoString() {
        return mementoDatetimeStr;
    }

    /**
     * Creates a new Fedora ID by joining the base ID of this Fedora ID with the specified string part. Any extensions
     * that this Fedora ID contains are discarded. For example:
     * <p>
     * Resolving "child" against "info:fedora/object1/another/fcr:versions/20000101121212" yields
     * "info:fedora/object1/another/child".
     *
     * @param child the part to join
     * @return new Fedora ID in the form baseId/child
     */
    public FedoraId resolve(final String child) {
        if (StringUtils.isBlank(child)) {
            throw new IllegalArgumentException("Child cannot be blank");
        }
        return FedoraId.create(baseId, child);
    }

    /**
     * Creates a new Fedora ID based on this ID that points to an ACL resource. The base ID, full ID without extensions,
     * is always used to construct an ACL ID. If this ID is already an ACL, then it returns itself.
     *
     * @return ACL resource ID
     */
    public FedoraId asAcl() {
        if (isAcl()) {
            return this;
        }

        return FedoraId.create(getBaseId(), FCR_ACL);
    }

    /**
     * Creates a new Fedora ID based on this ID that points to a binary description resource. There is no guarantee that
     * the binary description resource exists. If this ID is already a description, then it returns itself. Otherwise,
     * it uses the base ID, without extensions, to construct the new ID. If this Fedora ID is a timemap or memento or
     * a hash uri, then these extensions are applied to new description ID as well.
     *
     * @return description resource ID
     */
    public FedoraId asDescription() {
        if (isDescription()) {
            return this;
        }

        if (isTimemap()) {
            return FedoraId.create(getBaseId(), FCR_METADATA, FCR_VERSIONS);
        }

        if (isMemento()) {
            return FedoraId.create(getBaseId(), FCR_METADATA, FCR_VERSIONS, appendHashIfPresent(getMementoString()));
        }

        return FedoraId.create(getBaseId(), appendHashIfPresent(FCR_METADATA));
    }

    /**
     * Creates a new Fedora ID based on this ID that points to a tombstone resource. If this ID is already a tombstone,
     * then it returns itself. Otherwise, it uses the base ID, without extensions, to construct the new ID.
     *
     * @return tombstone resource ID
     */
    public FedoraId asTombstone() {
        if (isTombstone()) {
            return this;
        }

        return FedoraId.create(getBaseId(), FCR_TOMBSTONE);
    }

    /**
     * Creates a new Fedora ID based on this ID that points to a timemap resource. If this ID is already a timemap,
     * then it returns itself. Otherwise, it uses the base ID, without extensions, to construct the new ID. Unless
     * this ID is a binary description, in which case the new ID is constructed using the full ID.
     *
     * @return timemap resource ID
     */
    public FedoraId asTimemap() {
        if (isTimemap()) {
            return this;
        }

        if (isDescription()) {
            return FedoraId.create(getBaseId(), FCR_METADATA, FCR_VERSIONS);
        }

        return FedoraId.create(getBaseId(), FCR_VERSIONS);
    }

    /**
     * Creates a new Fedora ID based on this ID that points to a memento resource. If this ID is already a memento,
     * then it returns itself. If this ID is an ACL, tombstone, or timemap, then the new ID is constructed using this
     * ID's base ID. Otherwise, the full ID is used.
     *
     * @param mementoInstant memento representation
     * @return memento resource ID
     */
    public FedoraId asMemento(final Instant mementoInstant) {
        return asMemento(MEMENTO_LABEL_FORMATTER.format(mementoInstant));
    }

    /**
     * Creates a new Fedora ID based on this ID that points to a memento resource. If this ID is already a memento,
     * then it returns itself. If this ID is an ACL, tombstone, or timemap, then the new ID is constructed using this
     * ID's base ID. If this ID is a description, then the new ID is appended to the description ID.
     *
     * @param mementoString string memento representation
     * @return memento resource ID
     */
    public FedoraId asMemento(final String mementoString) {
        if (isMemento()) {
            return this;
        }

        if (isDescription()) {
            return FedoraId.create(getBaseId(), FCR_METADATA, FCR_VERSIONS, appendHashIfPresent(mementoString));
        }

        if (isAcl() || isTombstone() || isTimemap()) {
            return FedoraId.create(getBaseId(), FCR_VERSIONS, mementoString);
        }

        return FedoraId.create(getBaseId(), FCR_VERSIONS, appendHashIfPresent(mementoString));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof FedoraId)) {
            return false;
        }

        final var testObj = (FedoraId) obj;
        return Objects.equals(testObj.getFullId(), this.getFullId());
    }

    @Override
    public int hashCode() {
        return getFullId().hashCode();
    }

    @JsonValue
    @Override
    public String toString() {
        return getFullId();
    }

    /**
     * Concatenates all the parts with slashes
     * @param parts array of strings
     * @return the concatenated string.
     */
    private static String idBuilder(final String... parts) {
        if (parts != null && parts.length > 0) {
            return Arrays.stream(parts).filter(Objects::nonNull)
                    .map(s -> s.startsWith("/") ? s.substring(1) : s)
                    .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1 ) : s)
                    .collect(Collectors.joining("/"));
        }
        return "";
    }

    /**
     * Ensure the ID has the info:fedora/ prefix.
     * @param id the identifier, if null assume repository root (info:fedora/)
     * @return the identifier with the info:fedora/ prefix.
     */
    private static String ensurePrefix(final String id) {
        if (id == null) {
            return FEDORA_ID_PREFIX;
        }
        return id.startsWith(FEDORA_ID_PREFIX) ? id : FEDORA_ID_PREFIX + "/" + id;
    }

    /**
     * Process the original ID into its parts without using a regular expression.
     */
    private String processIdentifier() {
        // Regex pattern which decomposes a http resource uri into components
        // The first group determines if it is an fcr:metadata non-rdf source.
        // The second group determines if the path is for a memento or timemap.
        // The third group allows for a memento identifier.
        // The fourth group for allows ACL.
        // The fifth group allows for any hashed suffixes.
        // ".*?(/" + FCR_METADATA + ")?(/" + FCR_VERSIONS + "(/\\d{14})?)?(/" + FCR_ACL + ")?(\\#\\S+)?$");
        if (this.fullId.contains("//")) {
            throw new InvalidResourceIdentifierException(String.format("Path contains empty element! %s", fullPath));
        }
        String processID = this.fullId;
        if (processID.equals(FEDORA_ID_PREFIX)) {
            this.isRepositoryRoot = true;
            return this.fullId;
        }
        if (processID.contains("#")) {
            final String[] hashSplits = StringUtils.splitPreserveAllTokens(processID, "#");
            if (hashSplits.length > 2) {
                throw new InvalidResourceIdentifierException(String.format(
                        "Path <%s> is invalid. It may not contain more than one #",
                        fullPath));
            }
            this.hashUri = hashSplits[1];
            processID = hashSplits[0];
        }
        if (processID.contains(FCR_TOMBSTONE)) {
            processID = removePart(processID, FCR_TOMBSTONE);
            this.isTombstone = true;
        }
        if (processID.contains(FCR_ACL)) {
            processID = removePart(processID, FCR_ACL);
            this.isAcl = true;
        }
        if (processID.contains(FCR_VERSIONS)) {
            final String[] versionSplits = split(processID, FCR_VERSIONS);
            if (versionSplits.length > 2) {
                throw new InvalidResourceIdentifierException(String.format(
                        "Path <%s> is invalid. May not contain multiple %s parts.",
                        fullPath, FCR_VERSIONS));
            } else if (versionSplits.length == 2 && versionSplits[1].isEmpty()) {
                this.isTimemap = true;
            } else {
                final String afterVersion = versionSplits[1];
                if (afterVersion.matches("/\\d{14}")) {
                    this.isMemento = true;
                    this.mementoDatetimeStr = afterVersion.substring(1);
                    try {
                        this.mementoDatetime = Instant.from(MEMENTO_LABEL_FORMATTER.parse(this.mementoDatetimeStr));
                    } catch (final DateTimeParseException e) {
                        throw new InvalidMementoPathException(String.format("Invalid request for memento at %s",
                                fullPath));
                    }
                } else if (afterVersion.equals("/")) {
                    // Possible trailing slash?
                    this.isTimemap = true;
                } else {
                    throw new InvalidMementoPathException(String.format("Invalid request for memento at %s", fullPath));
                }
            }
            processID = versionSplits[0];
        }
        if (processID.contains(FCR_METADATA)) {
            processID = removePart(processID, FCR_METADATA);
            this.isNonRdfSourceDescription = true;
        }
        if (processID.endsWith("/")) {
            processID = processID.replaceAll("/+$", "");
        }

        return processID;
    }

    private String removePart(final String original, final String part) {
        final String[] split = split(original, part);
        if (split.length > 2 || (split.length == 2 && !split[1].isEmpty())) {
            throw new InvalidResourceIdentifierException("Path is invalid:" + fullPath);
        }
        return split[0];
    }

    private String[] split(final String original, final String part) {
        return StringUtils.splitByWholeSeparatorPreserveAllTokens(original, "/" + part);
    }

    /**
     * Check for obvious path errors.
     */
    private void checkForInvalidPath() {
        // Check for combinations of endpoints not allowed.
        if (
            // ID contains fcr:acl or fcr:tombstone AND fcr:metadata or fcr:versions
            ((this.fullId.contains(FCR_ACL) || this.fullId.contains(FCR_TOMBSTONE)) &&
                (this.fullId.contains(FCR_METADATA) || this.fullId.contains(FCR_VERSIONS))) ||
            // or ID contains fcr:acl AND fcr:tombstone
            (this.fullId.contains(FCR_TOMBSTONE) && this.fullId.contains(FCR_ACL))
        ) {
            throw new InvalidResourceIdentifierException(String.format("Path is invalid: %s", fullPath));
        }
        // Ensure we don't have 2 of any of the extensions, ie. info:fedora/object/fcr:acl/fcr:acl, etc.
        for (final Pattern extension : extensions) {
            if (extension.matcher(this.fullId).results().count() > 1) {
                throw new InvalidResourceIdentifierException(String.format("Path is invalid: %s", fullPath));
            }
        }
    }

    /**
     * Ensures that the Fedora ID does not violate any naming restrictions that are in place prevent collisions on disk.
     * These restrictions are based on the following naming conventions:
     *      https://wiki.lyrasis.org/display/FF/Design+-+Fedora+OCFL+Object+Structure
     *
     * All ids should be validated on resource creation
     */
    private void enforceStorageLayoutNamingConstraints() {
        final var finalPart = StringUtils.substringAfterLast(baseId, "/");

        if (FORBIDDEN_ID_PART_STRINGS.contains(finalPart)) {
            throw new InvalidResourceIdentifierException(
                    String.format("Invalid resource ID. IDs may not contain the string '%s'.", finalPart));
        }

        FORBIDDEN_ID_PART_SUFFIXES.forEach(suffix -> {
            if (finalPart.endsWith(suffix) && !finalPart.equals(suffix)) {
                throw new InvalidResourceIdentifierException(
                        String.format("Invalid resource ID. IDs may not end with '%s'.", suffix));
            }
        });
    }

    private String appendHashIfPresent(final String original) {
        if (isHashUri()) {
            return original + "#" + getHashUri();
        }
        return original;
    }

}
