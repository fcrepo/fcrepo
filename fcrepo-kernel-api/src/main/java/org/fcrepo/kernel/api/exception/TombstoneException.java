/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
