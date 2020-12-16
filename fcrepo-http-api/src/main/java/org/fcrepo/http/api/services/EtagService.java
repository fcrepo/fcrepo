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
package org.fcrepo.http.api.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.fcrepo.kernel.api.ContainmentIndex;
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
     * @param txId transaction id
     * @param resource resource
     * @param prefers LDP preference headers for the request
     * @param acceptableMediaTypes collection of acceptable media types for the response
     * @return etag for the request
     */
    public String getRdfResourceEtag(final String txId, final FedoraResource resource,
            final LdpTriplePreferences prefers, final Collection<MediaType> acceptableMediaTypes) {
        // Start etag based on the current state of the fedora resource, using the state token
        final StringBuilder etag = new StringBuilder(resource.getStateToken());

        // Factor in the requested mimetype(s)
        final String mimetype = acceptableMediaTypes.stream()
                .map(MediaType::toString)
                .sorted()
                .collect(Collectors.joining(";"));
        addComponent(etag, mimetype);

        // Factor in preferences which change which triples are included in the response
        etag.append('|');
        if (prefers.prefersContainment()) {
            final var lastUpdated = containmentIndex.containmentLastUpdated(txId, resource.getFedoraId());
            if (lastUpdated != null) {
                etag.append(lastUpdated);
            }
        }
        etag.append('|');
        if (prefers.prefersMembership()) {
            final var lastUpdated = membershipService.getLastUpdatedTimestamp(txId, resource.getFedoraId());
            if (lastUpdated != null) {
                etag.append(lastUpdated);
            }
        }
        addComponent(etag, prefers.prefersEmbed());
        addComponent(etag, prefers.preferNoUserRdf());
        addComponent(etag, prefers.prefersReferences());
        addComponent(etag, prefers.prefersServerManaged());

        // Compute a digest of all these components to use as the etag
        final String etagMd5 = DigestUtils.md5Hex(etag.toString()).toUpperCase();
        LOGGER.debug("Produced etag {} for {} from {}", etagMd5, resource.getId(), etag);

        return etagMd5;
    }

    private void addComponent(final StringBuilder etag, final Object component) {
        etag.append('|').append(component);
    }
}
