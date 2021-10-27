/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Service for creating versions of resources
 *
 * @author bbpennel
 * @author whikloj
 * @since Feb 19, 2014
 */
public interface VersionService {

    /**
     * To format a datetime for use as a Memento path.
     */
    DateTimeFormatter MEMENTO_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(UTC);

    /**
     * To format a datetime as RFC-1123 with correct timezone.
     */
    DateTimeFormatter MEMENTO_RFC_1123_FORMATTER = RFC_1123_DATE_TIME.withZone(UTC);

    /**
     * Explicitly creates a version for the resource at the path provided.
     *
     * @param transaction the transaction in which the resource resides
     * @param fedoraId the internal resource id
     * @param userPrincipal the user principal
     */
    void createVersion(Transaction transaction, FedoraId fedoraId, String userPrincipal);

}
