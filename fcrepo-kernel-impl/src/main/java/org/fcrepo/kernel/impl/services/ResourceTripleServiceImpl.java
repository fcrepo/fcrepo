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
package org.fcrepo.kernel.impl.services;

import static java.util.stream.Stream.empty;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;
import org.fcrepo.kernel.api.services.ContainmentTriplesService;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.api.services.ResourceTripleService;
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

        if (!preferences.preferNoUserRdf()) {
            // provide user RDF only if we didn't receive an omit=ldp:PreferMinimalContainer
            streams.add(resource.getTriples());
        }
        if (preferences.getMinimal()) {
            if (preferences.prefersServerManaged())  {
                streams.add(this.managedPropertiesService.get(resource));
                //TODO Implement minimal return preference (https://jira.lyrasis.org/browse/FCREPO-3334)
                //streams.add(getTriples(resource, MINIMAL));
            }
        } else {

            // Additional server-managed triples about this resource
            if (preferences.prefersServerManaged()) {
                streams.add(this.managedPropertiesService.get(resource));
            }

            // containment triples about this resource
            if (preferences.prefersContainment()) {
                if (limit == -1) {
                    streams.add(this.containmentTriplesService.get(tx, resource));
                } else {
                    streams.add(this.containmentTriplesService.get(tx, resource).limit(limit));
                }
            }

            // LDP container membership triples for this resource
            if (preferences.prefersMembership()) {
                streams.add(membershipService.getMembership(tx.getId(), resource.getFedoraId()));
            }

            // Include inbound references to this object
            if (preferences.prefersReferences()) {
                streams.add(referenceService.getInboundReferences(tx.getId(), resource));
            }

        }

        return streams.stream().reduce(empty(), Stream::concat);
    }

}
