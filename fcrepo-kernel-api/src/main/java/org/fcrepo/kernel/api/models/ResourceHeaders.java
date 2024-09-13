/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

/**
 * Header information for fedora resources.
 *
 * @author bbpennel
 */
public interface ResourceHeaders {

    String V1_0 = "1.0";

    /**
     * Get the identifier for the described resource.
     *
     * @return identifier for the resource.
     */
    FedoraId getId();

    /**
     * Get the identifier of the parent of the resource
     *
     * @return identifier of the parent
     */
    FedoraId getParent();

    /**
     * Get the identifier of the archival group resource that contains this resource, or null if the resource is not
     * an archival part resource
     *
     * @return identifier of the containing archival group resource or null
     */
    FedoraId getArchivalGroupId();

    /**
     * Get the State Token value for the resource.
     *
     * @return state-token value
     */
    String getStateToken();

    /**
     * Get the interaction model for the resource
     *
     * @return interaction model URI
     */
    String getInteractionModel();

    /**
     * Get the mimetype describing the content contained by this resource
     *
     * @return mimetype
     */
    String getMimeType();

    /**
     * Get the filename for the content of this resource
     *
     * @return filename
     */
    String getFilename();

    /**
     * Get the size in bytes of the content of this resource. May be -1 if the size is unknown or there is no content.
     *
     * @return size
     */
    long getContentSize();

    /**
     * Get the list of all digest URIs recorded for this resource
     *
     * @return digest URIs
     */
    Collection<URI> getDigests();

    /**
     * Get the url of external content associated with this resource.
     *
     * @return external url
     */
    String getExternalUrl();

    /**
     * Get the handling type for external content associated with this resource.
     *
     * @return external handling value
     */
    String getExternalHandling();

    /**
     * Get the date this resource was created
     *
     * @return created date
     */
    Instant getCreatedDate();

    /**
     * Get the created by for the resource
     *
     * @return created by
     */
    String getCreatedBy();

    /**
     * Get the date this resource was last modified
     *
     * @return last modified date
     */
    Instant getLastModifiedDate();

    /**
     * Get the last modified by value for the resource
     *
     * @return last modified by
     */
    String getLastModifiedBy();

    /**
     * Get the date a memento for this resource was created. This field should generally be kept in sync with the
     * last modified date, but they may not be the same, in the case that a memento was created as a result of an
     * update to a different resource. Additionally, this date is NOT the same as the actual memento timestamp, which
     * is determined by the timestamp on the OCFL version.
     *
     * @return memento created date
     */
    Instant getMementoCreatedDate();

    /**
     * Determine whether a resource is an Archival Group
     * @return Archival Group status
     */
    boolean isArchivalGroup();

    /**
     * Determine whether a resource is the object root
     * @return true if the resource is at the root of a persistence object
     */
    boolean isObjectRoot();

    /**
     * Determine if the resource is now a tombstone.
     * @return Deleted status.
     */
    boolean isDeleted();

    /**
     * Returns the path to the content file the resource headers are associated with
     * @return path the content file
     */
    String getContentPath();

    /**
     * @return the header version
     */
    String getHeadersVersion();

    /**
     * @return the content path relative to the OCFL object root
     */
    String getStorageRelativePath();
}
