/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;
import org.fcrepo.kernel.api.services.MembershipService;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Service for computing etags for request responses
 *
 * @author bbpennel
 */
@Component
public class EtagService {
    private static final Logger LOGGER = getLogger(EtagService.class);

    @Inject
    private ContainmentIndex containmentIndex;

    @Inject
    private MembershipService membershipService;

    /**
     * Produces etag for a request for an RDF resource. It is based on factors related to the
     * current state of the resource, as well as request options which change the
     * representation of the resource.
     *
     * @param transaction transaction
     * @param resource resource
     * @param prefers LDP preference headers for the request
     * @param acceptableMediaTypes collection of acceptable media types for the response
     * @return etag for the request
     */
    public String getRdfResourceEtag(final Transaction transaction, final FedoraResource resource,
                                     final LdpTriplePreferences prefers,
                                     final Collection<MediaType> acceptableMediaTypes) {
        final String stateToken = (resource.getStateToken() != null ? resource.getStateToken() : "");
        // Start etag based on the current state of the fedora resource, using the state token
        final StringBuilder etag = new StringBuilder(stateToken);

        // Factor in the requested mimetype(s)
        final String mimetype = acceptableMediaTypes.stream()
                .map(MediaType::toString)
                .sorted()
                .collect(Collectors.joining(";"));
        addComponent(etag, mimetype);

        // Factor in preferences which change which triples are included in the response
        etag.append('|');
        if (prefers.displayContainment()) {
            final var lastUpdated = containmentIndex.containmentLastUpdated(transaction, resource.getFedoraId());
            if (lastUpdated != null) {
                etag.append(lastUpdated);
            }
        }
        etag.append('|');
        if (prefers.displayMembership()) {
            final var lastUpdated = membershipService.getLastUpdatedTimestamp(transaction, resource.getFedoraId());
            if (lastUpdated != null) {
                etag.append(lastUpdated);
            }
        }
        addComponent(etag, prefers.displayEmbed());
        addComponent(etag, prefers.displayUserRdf());
        addComponent(etag, prefers.displayReferences());
        addComponent(etag, prefers.displayServerManaged());

        // Compute a digest of all these components to use as the etag
        final String etagMd5 = DigestUtils.md5Hex(etag.toString()).toUpperCase();
        LOGGER.debug("Produced etag {} for {} from {}", etagMd5, resource.getId(), etag);

        return etagMd5;
    }

    private void addComponent(final StringBuilder etag, final Object component) {
        etag.append('|').append(component);
    }
}
