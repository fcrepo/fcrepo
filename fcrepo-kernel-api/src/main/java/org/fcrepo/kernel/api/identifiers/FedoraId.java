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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.InvalidMementoPathException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;

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

    private String id;
    private String fullId;
    private String hashUri;
    private boolean isRepositoryRoot = false;
    private boolean isNonRdfSourceDescription = false;
    private boolean isAcl = false;
    private boolean isMemento = false;
    private boolean isTimemap = false;
    private Instant mementoDatetime;
    private String mementoDatetimeStr;
    private String pathOnly;

    /**
     * Basic constructor.
     * @param fullId The full identifier or null if root.
     * @throws IllegalArgumentException If ID does not start with expected prefix.
     */
    private FedoraId(final String fullId) {
        this.fullId = ensurePrefix(fullId);
        this.fullId = this.fullId.replaceAll("/+$", "");
        // Carry the path of the request for any exceptions.
        this.pathOnly = this.fullId.substring(FEDORA_ID_PREFIX.length());

        processIdentifier();
    }

    /**
     * Static create method
     * @param additions One or more strings to build an ID.
     * @return The FedoraId.
     */
    public static FedoraId create(final String... additions) {
        final var newId = idBuilder(additions);
        return new FedoraId(newId);
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
     * Return the ID of the base resource for this request.
     * @return the shorten id.
     */
    public String getResourceId() {
        return id;
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
        return pathOnly;
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
     * Descriptions are needed to retrieve from the persistence, but otherwise is just an addendum to the binary.
     * @return The description ID or null if not a description.
     */
    public String getDescriptionId() {
        if (isDescription()) {
            return getResourceId() + "/" + FCR_METADATA;
        }
        return null;
    }

    /**
     * Resolve the string or strings against this ID to create a new one.
     *
     * A string starting with a slash (/) will resolve from the resource ID, a string not starting with a slash
     * will resolve against the full ID.
     *
     * {@code
     *     final FedoraId descId = FedoraId.create("info:fedora/object1/child/fcr:metadata");
     *     final var binaryTimemap = descId.resolve("/" + FCR_VERSIONS); // info:fedora/object1/child/fcr:versions
     *     final var descTimemap = descId.resolve(FCR_VERSIONS); // info:fedora/object1/child/fcr:metadata/fcr:versions
     * }
     * @param addition the string or strings to add to the ID.
     * @return a new FedoraId instance
     */
    public FedoraId resolve(final String... addition) {
        if (addition == null || addition.length == 0 || addition[0].isBlank()) {
            throw new IllegalArgumentException("You must provide at least one string to resolve");
        }
        final String[] parts;
        if (addition[0].startsWith("/")) {
            parts = Stream.of(new String[]{this.getResourceId()}, addition).flatMap(Stream::of).toArray(String[]::new);
            return FedoraId.create(parts);
        }
        parts = Stream.of(new String[]{this.getFullId()}, addition).flatMap(Stream::of).toArray(String[]::new);
        return FedoraId.create(parts);
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
            final String id = Arrays.stream(parts).filter(Objects::nonNull)
                    .map(s -> s.startsWith("/") ? s.substring(1) : s)
                    .map(s -> s.endsWith("/") ? s.substring(0, s.length() -1 ) : s)
                    .collect(Collectors.joining("/"));
            return id;
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
    private void processIdentifier() {
        // Regex pattern which decomposes a http resource uri into components
        // The first group determines if it is an fcr:metadata non-rdf source.
        // The second group determines if the path is for a memento or timemap.
        // The third group allows for a memento identifier.
        // The fourth group for allows ACL.
        // The fifth group allows for any hashed suffixes.
        // ".*?(/" + FCR_METADATA + ")?(/" + FCR_VERSIONS + "(/\\d{14})?)?(/" + FCR_ACL + ")?(\\#\\S+)?$");
        if (this.fullId.contains("//")) {
            throw new InvalidResourceIdentifierException(String.format("Path contains empty element! %s", pathOnly));
        }
        String processID = this.fullId;
        if (processID.equals(FEDORA_ID_PREFIX)) {
            this.isRepositoryRoot = true;
            this.id = this.fullId;
            // Root has no other possible endpoints, so short circuit out.
            return;
        }
        if (processID.contains("#")) {
            final String[] hashSplits = processID.split("#");
            this.hashUri = hashSplits[1];
            processID = hashSplits[0];
        }
        if (processID.contains(FCR_ACL)) {
            final String[] aclSplits = processID.split("/" + FCR_ACL);
            if (aclSplits.length > 1) {
                throw new InvalidResourceIdentifierException(String.format("Path is invalid: %s", pathOnly));
            }
            this.isAcl = true;
            processID = aclSplits[0];
        }
        if (processID.contains(FCR_VERSIONS)) {
            final String[] versionSplits = processID.split( "/" + FCR_VERSIONS);
            if (versionSplits.length == 2) {
                final String afterVersion = versionSplits[1];
                if (afterVersion.matches("/\\d{14}")) {
                    this.isMemento = true;
                    this.mementoDatetimeStr = afterVersion.substring(1);
                    try {
                        this.mementoDatetime = Instant.from(MEMENTO_LABEL_FORMATTER.parse(this.mementoDatetimeStr));
                    } catch (final DateTimeParseException e) {
                        throw new InvalidMementoPathException(String.format("Invalid request for memento at %s",
                                pathOnly));
                    }
                } else if (afterVersion.equals("/")) {
                    // Possible trailing slash?
                    this.isTimemap = true;
                } else {
                    throw new InvalidMementoPathException(String.format("Invalid request for memento at %s", pathOnly));
                }
            } else {
                this.isTimemap = true;
            }
            processID = versionSplits[0];
        }
        if (processID.contains(FCR_METADATA)) {
            final String[] metadataSplits = processID.split("/" + FCR_METADATA);
            if (metadataSplits.length > 1) {
                throw new InvalidResourceIdentifierException(String.format("Path is invalid: %s", pathOnly));
            }
            this.isNonRdfSourceDescription = true;
            processID = metadataSplits[0];
        }
        if (processID.endsWith("/")) {
            processID = processID.replaceAll("/+$", "");
        }
        this.id = processID;
    }
}
