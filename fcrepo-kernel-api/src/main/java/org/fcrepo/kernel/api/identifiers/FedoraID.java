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

import org.fcrepo.kernel.api.exception.InvalidMementoPathException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;

/**
 * Class to store contextual information about a Fedora ID.
 * @author whikloj
 * @since 6.0.0
 */
public class FedoraID {

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
    private String externalPath;

    /**
     * Basic constructor.
     * @param fullId The full identifier.
     * @throws IllegalArgumentException If ID does not start with expected prefix.
     */
    public FedoraID(final String fullId) {
        if (!fullId.startsWith(FEDORA_ID_PREFIX)) {
            throw new IllegalArgumentException(String.format("ID must begin with %s", FEDORA_ID_PREFIX));
        }
        this.fullId = fullId;
        if (!this.fullId.equals(FEDORA_ID_PREFIX)) {
            // Only strip trailing slashes the ID is more than the info:fedora/ prefix.
            this.fullId = this.fullId.replaceAll("/+$", "");
        }
        // Carry the path of the request for any exceptions.
        this.externalPath = fullId.substring(FEDORA_ID_PREFIX.length());

        processIdentifier();
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
            throw new InvalidResourceIdentifierException(String.format("Path contains empty element! %s",
                    externalPath));
        }
        String processID = this.fullId;
        if (processID.equals(FEDORA_ID_PREFIX)) {
            this.isRepositoryRoot = true;
            this.id = this.fullId;
            // Root has no other possible endpoints, so short circuit out.
            return;
        }
        if (processID.contains("#")) {
            this.hashUri = processID.split("#")[1];
            processID = processID.split("#")[0];
        }
        if (processID.contains(FCR_ACL)) {
            final String[] aclSplits = processID.split("/" + FCR_ACL);
            if (aclSplits.length > 1) {
                throw new InvalidResourceIdentifierException(String.format("Path not found %s", externalPath));
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
                    } catch (DateTimeParseException e) {
                        throw new InvalidMementoPathException(String.format("Invalid request for memento at %s",
                                externalPath));
                    }
                } else if (afterVersion.equals("/")) {
                    // Possible trailing slash?
                    this.isTimemap = true;
                } else {
                    throw new InvalidMementoPathException(String.format("Invalid request for memento at %s",
                            externalPath));
                }
            } else {
                this.isTimemap = true;
            }
            processID = versionSplits[0];
        }
        if (processID.contains(FCR_METADATA)) {
            final String[] metadataSplits = processID.split("/" + FCR_METADATA);
            if (metadataSplits.length > 1) {
                throw new InvalidResourceIdentifierException(String.format("Path is invalid: %s", externalPath));
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
