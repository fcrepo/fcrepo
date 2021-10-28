/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.common;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Helper utilities for populate resource headers
 *
 * @author bbpennel
 */
public class ResourceHeaderUtils {

    /**
     * Private constructor
     */
    private ResourceHeaderUtils() {
    }

    /**
     * Construct and populate minimal headers expected for a new resource
     *
     * @param parentId identifier of the parent
     * @param fedoraId identifier of the new resource
     * @param interactionModel interaction model of the resource
     * @return new resource headers object
     */
    public static ResourceHeadersImpl newResourceHeaders(final FedoraId parentId, final FedoraId fedoraId,
                                                         final String interactionModel) {
        final ResourceHeadersImpl headers = new ResourceHeadersImpl();
        headers.setHeadersVersion(ResourceHeaders.V1_0);
        headers.setId(fedoraId);
        headers.setParent(parentId);
        headers.setInteractionModel(interactionModel);

        return headers;
    }

    /**
     * Update creation headers to the current state
     *
     * @param headers headers object to update
     * @param userPrincipal user principal performing the change
     */
    public static void touchCreationHeaders(final ResourceHeadersImpl headers, final String userPrincipal) {
        touchCreationHeaders(headers, userPrincipal, null);
    }

    /**
     * Update creation headers to the current state.
     *
     * @param headers headers object to update
     * @param userPrincipal user principal performing the change
     * @param createdDate time created. Defaults to now if not provided.
     */
    public static void touchCreationHeaders(final ResourceHeadersImpl headers, final String userPrincipal,
            final Instant createdDate) {
        final Instant instant;
        if (createdDate == null) {
            final ZonedDateTime now = ZonedDateTime.now();
            instant = now.toInstant();
        } else {
            instant = createdDate;
        }
        headers.setCreatedDate(instant);
        headers.setCreatedBy(userPrincipal);
        headers.setMementoCreatedDate(instant);
    }

    /**
     * Update modification headers to the current state
     *
     * @param headers headers object to update
     * @param userPrincipal user principal performing the change
     */
    public static void touchModificationHeaders(final ResourceHeadersImpl headers, final String userPrincipal) {
        touchModificationHeaders(headers, userPrincipal, null);
    }

    /**
     * Update modification headers to the current state
     *
     * @param headers headers object to update
     * @param userPrincipal user principal performing the change
     * @param modifiedDate modified time. Defaults to now if not provided.
     */
    public static void touchModificationHeaders(final ResourceHeadersImpl headers, final String userPrincipal,
            final Instant modifiedDate) {
        final Instant instant;
        if (modifiedDate == null) {
            final ZonedDateTime now = ZonedDateTime.now();
            instant = now.toInstant();
        } else {
            instant = modifiedDate;
        }
        headers.setLastModifiedDate(instant);
        headers.setLastModifiedBy(userPrincipal);
        touchMementoCreateHeaders(headers, instant);

        final String stateToken = DigestUtils.md5Hex(String.valueOf(instant.toEpochMilli())).toUpperCase();
        headers.setStateToken(stateToken);
    }

    /**
     * Update the mementoCreatedDate header
     * @param headers headers object to update.
     * @param versionDate time this version is created.
     */
    public static void touchMementoCreateHeaders(final ResourceHeadersImpl headers, final Instant versionDate) {
        final Instant instant;
        if (versionDate == null) {
            final ZonedDateTime now = ZonedDateTime.now();
            instant = now.toInstant();
        } else {
            instant = versionDate;
        }
        headers.setMementoCreatedDate(instant);
    }

    public static void touchMementoCreateHeaders(final ResourceHeadersImpl headers) {
        touchMementoCreateHeaders(headers, null);
    }

    /**
     * Populate general binary resource headers
     *
     * @param headers headers object to update
     * @param mimetype mimetype
     * @param filename filename
     * @param filesize filesize
     * @param digests digests
     */
    public static void populateBinaryHeaders(final ResourceHeadersImpl headers, final String mimetype,
            final String filename, final long filesize, final Collection<URI> digests) {
        headers.setMimeType(mimetype);
        headers.setDigests(digests);
        headers.setFilename(filename);
        headers.setContentSize(filesize);
    }

    /**
     * Populate external binary related headers
     *
     * @param headers headers object to update
     * @param externalUrl url of external binary content
     * @param externalHandling handling for the external content
     */
    public static void populateExternalBinaryHeaders(final ResourceHeadersImpl headers,
            final String externalUrl, final String externalHandling) {
        headers.setExternalHandling(externalHandling);
        headers.setExternalUrl(externalUrl);
    }
}
