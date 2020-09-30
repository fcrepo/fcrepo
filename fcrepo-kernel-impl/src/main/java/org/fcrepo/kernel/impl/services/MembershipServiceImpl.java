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

import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.fcrepo.kernel.api.RdfCollectors.toModel;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;

/**
 * Implementation of a service which updates and persists membership properties for resources
 *
 * @author bbpennel
 * @since 6.0.0
 */
@Component
public class MembershipServiceImpl implements MembershipService {
    private static final Logger log = getLogger(MembershipServiceImpl.class);

    public static final Instant NO_END_INSTANT = Instant.parse("9999-12-31T00:00:00.000Z");

    @Inject
    private MembershipIndexManager indexManager;

    @Inject
    private ResourceFactory resourceFactory;

    @Override
    public void resourceCreated(final String txId, final FedoraId fedoraId) {
        final var fedoraResc = getFedoraResource(txId, fedoraId);

        // Only need to compute membership for created containers and binaries
        if (!(fedoraResc instanceof Container || fedoraResc instanceof Binary)) {
            return;
        }

        final var parentResc = getParentResource(fedoraResc);

        if (parentResc instanceof Container && isDirectContainer(parentResc)) {
            final var parentRdfResc = getRdfResource(parentResc);
            final var newMembership = generateDirectMembership(txId, parentRdfResc, fedoraResc);
            indexManager.addMembership(txId, parentResc.getFedoraId(), newMembership, fedoraResc.getCreatedDate());
        }

        // TODO check if fedoraResc is a member of any IndirectContainers, but only if it is a container
    }

    @Override
    public void resourceModified(final String txId, final FedoraId fedoraId) {
        final var fedoraResc = getFedoraResource(txId, fedoraId);

        if (isDirectContainer(fedoraResc)) {
            final var dcRdfResc = getRdfResource(fedoraResc);

            log.debug("Modified DirectContainer {}, recomputing generated membership relations", fedoraId);

            final var dcLastModified = fedoraResc.getLastModifiedDate();

            // Delete/end existing membership from this container
            indexManager.deleteMembershipForSource(txId, fedoraResc.getFedoraId(), dcLastModified);

            // Add updated membership properties for all non-tombstone children
            fedoraResc.getChildren()
                    .filter(child -> !(child instanceof Tombstone))
                    .map(child -> generateDirectMembership(txId, dcRdfResc, child))
                    .forEach(newMembership -> indexManager.addMembership(txId, fedoraId,
                            newMembership, dcLastModified));
            return;
        }
        // TODO handle modification of IndirectContainers and proxies
    }

    private Triple generateDirectMembership(final String txId, final Resource dcRdfResc,
            final FedoraResource memberResc) {
        final var memberRdfResc = getRdfResource(memberResc.getFedoraId());

        final var membershipResc = getMembershipResource(dcRdfResc);
        final var memberOfRel = getMemberOfRelation(dcRdfResc);
        final var hasMemberRel = getHasMemberRelation(dcRdfResc);

        return generateMembershipTriple(membershipResc.asNode(), memberRdfResc.asNode(),
                hasMemberRel.asNode(), memberOfRel == null ? null : memberOfRel.asNode());
    }

    private Triple generateMembershipTriple(final Node membership, final Node member,
            final Node hasMemberRel, final Node memberOfRel) {
        if (memberOfRel != null) {
            return new Triple(member, memberOfRel, membership);
        } else {
            return new Triple(membership, hasMemberRel, member);
        }
    }

    private boolean isDirectContainer(final FedoraResource fedoraResc) {
        return fedoraResc instanceof Container && fedoraResc.hasType(RdfLexicon.DIRECT_CONTAINER.getURI());
    }

    private Resource getRdfResource(final FedoraResource fedoraResc) {
        final var model = fedoraResc.getTriples().collect(toModel());
        return model.getResource(fedoraResc.getFedoraId().getFullId());
    }

    private Resource getRdfResource(final FedoraId fedoraId) {
        return org.apache.jena.rdf.model.ResourceFactory.createResource(fedoraId.getFullId());
    }

    /**
     * @param resc
     * @return the ldp:isMemberOfRelation property for the given resource, or null if none is specified
     */
    private Resource getMemberOfRelation(final Resource resc) {
        final var memberOfRelStmt = resc.getProperty(RdfLexicon.IS_MEMBER_OF_RELATION);
        if (memberOfRelStmt != null) {
            return memberOfRelStmt.getResource();
        } else {
            return null;
        }
    }

    /**
     * @param resc
     * @return the ldp:hasMemberRelation property for the given resource, or ldp:member if none is specified
     */
    private Resource getHasMemberRelation(final Resource resc) {
        final var hasMemberStmt = resc.getProperty(RdfLexicon.HAS_MEMBER_RELATION);
        if (hasMemberStmt != null) {
            return hasMemberStmt.getResource();
        } else {
            return RdfLexicon.LDP_MEMBER;
        }
    }

    private Resource getMembershipResource(final Resource containerResc) {
        return containerResc.getPropertyResourceValue(RdfLexicon.MEMBERSHIP_RESOURCE);
    }

    private FedoraResource getFedoraResource(final String txId, final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(txId, fedoraId);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        }
    }

    private FedoraResource getParentResource(final FedoraResource resc) {
        try {
            return resc.getParent();
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        }
    }

    @Override
    public void resourceDeleted(final String txId, final FedoraId fedoraId) {
        // delete DirectContainer, end all membership for that source
        FedoraResource fedoraResc;
        try {
            fedoraResc = getFedoraResource(txId, fedoraId);
        } catch (final PathNotFoundRuntimeException e) {
            log.debug("Deleted resource {} does not have a tombstone, cleanup any references");
            indexManager.deleteMembershipReferences(txId, fedoraId);
            return;
        }
        if (fedoraResc instanceof Tombstone) {
            fedoraResc = ((Tombstone) fedoraResc).getDeletedObject();
        }

        if (isDirectContainer(fedoraResc)) {
            indexManager.deleteMembershipForSource(txId, fedoraId, fedoraResc.getLastModifiedDate());
        }

        // delete child of DirectContainer, clear from tx and end existing
        final var parentResc = getParentResource(fedoraResc);

        if (isDirectContainer(parentResc)) {
            final var parentRdfResc = getRdfResource(parentResc);
            final var deletedMembership = generateDirectMembership(txId, parentRdfResc, fedoraResc);
            indexManager.deleteMembership(txId, parentResc.getFedoraId(), deletedMembership,
                    fedoraResc.getLastModifiedDate());

        }
        // delete membership resource, do nothing? Membership references will persist on members
    }

    @Override
    public RdfStream getMembership(final String txId, final FedoraId fedoraId) {
        final FedoraId subjectId;
        if (fedoraId.isDescription()) {
            subjectId = fedoraId.asBaseId();
        } else {
            subjectId = fedoraId;
        }
        final var subject = NodeFactory.createURI(subjectId.getBaseId());
        final var membershipStream = indexManager.getMembership(txId, subjectId);
        return new DefaultRdfStream(subject, membershipStream);
    }

    @Override
    public void commitTransaction(final String txId) {
        indexManager.commitTransaction(txId);
    }

    @Override
    public void rollbackTransaction(final String txId) {
        indexManager.deleteTransaction(txId);
    }

    @Override
    public void reset() {
        indexManager.clearIndex();
    }

    @Override
    public void populateMembershipHistory(final String txId, final FedoraId containerId) {
        final FedoraResource fedoraResc = getFedoraResource(txId, containerId);

        if (isDirectContainer(fedoraResc)) {
            final var propertyTimeline = makePropertyTimeline(fedoraResc);

            // get all the members of the DC and index the history for each, accounting for changes to the DC
            fedoraResc.getChildren().forEach(member -> {
                final var memberDeleted = member instanceof Tombstone;
                final var memberNode = NodeFactory.createURI(member.getFedoraId().getFullId());
                log.debug("Populating membership history for DirectContainer {}member {}",
                        memberDeleted ? "deleted " : "", member.getFedoraId());
                final Instant memberCreated;
                // Get the creation time from the deleted object if the member is a tombstone
                if (memberDeleted) {
                    memberCreated = ((Tombstone) member).getDeletedObject().getCreatedDate();
                } else {
                    memberCreated = member.getCreatedDate();
                }
                final var memberModified = member.getLastModifiedDate();
                final var memberEnd = memberDeleted ? memberModified : NO_END_INSTANT;

                // Reduce timeline to just states in effect after the member was created
                var timelineStream = propertyTimeline.stream()
                        .filter(e -> e.endDatetime.compareTo(memberCreated) > 0);
                // If the member was deleted, then reduce timeline to states before the deletion
                if (memberDeleted) {
                    timelineStream = timelineStream.filter(e -> e.mementoDatetime.compareTo(memberModified) < 0);
                }
                // Keep track of the last membership relation added in case it was deleted
                final var lastMembership = new AtomicReference<Triple>();
                // Index each addition or change to the membership generated by this member
                timelineStream.forEach(e -> {
                    lastMembership.set(generateMembershipTriple(e.membershipResource,
                            memberNode, e.hasMemberRelation, e.isMemberOfRelation));
                    // Start time of the membership is the later of member creation or membership resc memento time
                    indexManager.addMembership(txId, containerId,
                            lastMembership.get(),
                            instantMax(memberCreated, e.mementoDatetime),
                            instantMin(memberEnd, e.endDatetime));
                });
            });
        }
    }

    private Instant instantMax(final Instant first, final Instant second) {
        if (first.isAfter(second)) {
            return first;
        } else {
            return second;
        }
    }

    private Instant instantMin(final Instant first, final Instant second) {
        if (first.isBefore(second)) {
            return first;
        } else {
            return second;
        }
    }

    /**
     * Creates a timeline of states for a DirectContainer, tracking changes to its
     * properties that impact membership.
     * @param fedoraResc resource subject of the timeline
     * @return timeline
     */
    private List<DirectContainerProperties> makePropertyTimeline(final FedoraResource fedoraResc) {
        final var entryList = fedoraResc.getTimeMap().getChildren()
                .sorted((m1, m2) -> m1.getFedoraId().getFullId().compareTo(m1.getFedoraId().getFullId()))
                .map(memento -> new DirectContainerProperties(memento))
                .collect(Collectors.toList());

        // Reduce timeline to entries where significant properties change
        final var changeEntries = new ArrayList<DirectContainerProperties>();
        var curr = entryList.get(0);
        changeEntries.add(curr);
        int i = 1;
        while (i < entryList.size()) {
            final var next = entryList.get(i);
            if (!Objects.equals(next.membershipResource, curr.membershipResource)
                    || !Objects.equals(next.hasMemberRelation, curr.hasMemberRelation)
                    || !Objects.equals(next.isMemberOfRelation, curr.isMemberOfRelation)) {
                curr.endDatetime = next.mementoDatetime;
                changeEntries.add(next);
                curr = next;
            }
            i++;
        }
        return changeEntries;
    }

    /**
     * The properties of a Direct or Indirect Container at a point in time.
     * @author bbpennel
     */
    private static class DirectContainerProperties {
        public Node membershipResource;
        public Node hasMemberRelation;
        public Node isMemberOfRelation;
        public Instant mementoDatetime;
        public Instant endDatetime = NO_END_INSTANT;

        /**
         * @param memento memento of the resource from which the properties will be extracted
         */
        public DirectContainerProperties(final FedoraResource memento) {
            mementoDatetime = memento.getMementoDatetime();
            memento.getTriples().forEach(triple -> {
                if (RdfLexicon.MEMBERSHIP_RESOURCE.asNode().equals(triple.getPredicate())) {
                    membershipResource = triple.getObject();
                } else if (RdfLexicon.HAS_MEMBER_RELATION.asNode().equals(triple.getPredicate())) {
                    hasMemberRelation = triple.getObject();
                } else if (RdfLexicon.IS_MEMBER_OF_RELATION.asNode().equals(triple.getPredicate())) {
                    isMemberOfRelation = triple.getObject();
                }
            });
        }
    }

    /**
     * @param indexManager the indexManager to set
     */
    public void setMembershipIndexManager(final MembershipIndexManager indexManager) {
        this.indexManager = indexManager;
    }

    /**
     * @param resourceFactory the resourceFactory to set
     */
    public void setResourceFactory(final ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }
}
