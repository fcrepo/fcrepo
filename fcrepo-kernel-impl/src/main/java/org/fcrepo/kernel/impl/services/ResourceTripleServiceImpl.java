/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static java.util.stream.Stream.empty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;
import org.fcrepo.kernel.api.services.ContainmentTriplesService;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.api.services.ResourceTripleService;

import org.apache.jena.graph.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Implementation of the ResourceTripleService
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class ResourceTripleServiceImpl implements ResourceTripleService {

    @Inject
    private ManagedPropertiesService managedPropertiesService;

    @Inject
    private ContainmentTriplesService containmentTriplesService;

    @Autowired
    @Qualifier("referenceService")
    private ReferenceService referenceService;

    @Inject
    private MembershipService membershipService;

    @Override
    public Stream<Triple> getResourceTriples(final Transaction tx, final FedoraResource resource,
                                             final LdpTriplePreferences preferences, final int limit) {
        final List<Stream<Triple>> streams = new ArrayList<>();

        // Provide user RDF if we didn't ask for omit=ldp:PreferMinimalContainer.
        if (preferences.displayUserRdf()) {
            streams.add(resource.getTriples());
        }
        // Provide server-managed triples if we didn't ask for omit=fedora:ServerManaged or
        // omit=ldp:PreferMinimalContainer
        if (preferences.displayServerManaged()) {
            streams.add(this.managedPropertiesService.get(resource));
        }
        // containment triples about this resource, return by default. Containment is server managed so also
        // check for that prefer tag.
        if (preferences.displayContainment()) {
            if (limit == -1) {
                streams.add(this.containmentTriplesService.get(tx, resource));
            } else {
                streams.add(this.containmentTriplesService.get(tx, resource).limit(limit));
            }
        }

        // LDP container membership triples for this resource, returned by default. Membership is server managed so
        // also check that tag.
        if (preferences.displayMembership()) {
            streams.add(membershipService.getMembership(tx, resource.getFedoraId()));
        }

        // Include inbound references to this object, NOT returned by default.
        if (preferences.displayReferences()) {
            streams.add(referenceService.getInboundReferences(tx, resource));
            streams.add(membershipService.getMembershipByObject(tx, resource.getFedoraId()));
            streams.add(containmentTriplesService.getContainedBy(tx, resource));
        }

        return streams.stream().reduce(empty(), Stream::concat);
    }

}
