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
package org.fcrepo.kernel.api.exception;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Exception when a Tombstone {@link org.fcrepo.kernel.api.models.FedoraResource}
 * is used where a real object is expected
 *
 * @author cabeer
 * @since 10/16/14
 */
public class TombstoneException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    private final String uri;

    private static DateTimeFormatter isoFormatter = ISO_INSTANT.withZone(UTC);

    /**
     * Construct a new tombstone exception for a resource
     * @param resource the fedora resource
     */
    public TombstoneException(final FedoraResource resource) {
        this(resource, null);
    }

    /**
     * Create a new tombstone exception with a URI to the tombstone resource
     * @param resource the fedora resource
     * @param tombstoneUri the uri to the tombstone resource for the Link header.
     */
    public TombstoneException(final FedoraResource resource, final String tombstoneUri) {
        super("Discovered tombstone resource at " + resource.getFedoraId().getFullIdPath() +
                (Objects.nonNull(resource.getLastModifiedDate()) ? ", departed at: " +
                isoFormatter.format(resource.getLastModifiedDate()) : ""));
        this.uri = tombstoneUri;
    }

    /**
     * Get a URI to the tombstone resource
     * @return the URI to the tombstone resource
     */
    public String getURI() {
        return uri;
    }
}
