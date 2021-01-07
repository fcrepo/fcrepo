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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import org.fcrepo.config.OcflPropsConfig;
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
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

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

    @Inject
    private OcflPropsConfig propsConfig;

    private enum ContainerType {
        Direct, Indirect;
    }

    @Override
    @Transactional
    public void resourceCreated(final String txId, final FedoraId fedoraId) {
        final var fedoraResc = getFedoraResource(txId, fedoraId);

        // Only need to compute membership for created containers and binaries
        if (!(fedoraResc instanceof Container || fedoraResc instanceof Binary)) {
            return;
        }

        final var parentResc = getParentResource(fedoraResc);
        final var containerProperties = new DirectContainerProperties(parentResc);

        if (containerProperties.containerType != null) {
            final var newMembership = generateMembership(containerProperties, fedoraResc);
            indexManager.addMembership(txId, parentResc.getFedoraId(), fedoraResc.getFedoraId(),
                    newMembership, fedoraResc.getCreatedDate());
        }
    }

    @Override
    @Transactional
    public void resourceModified(final String txId, final FedoraId fedoraId) {
        final var fedoraResc = getFedoraResource(txId, fedoraId);
        final var containerProperties = new DirectContainerProperties(fedoraResc);

        if (containerProperties.containerType != null) {
            log.debug("Modified DirectContainer {}, recomputing generated membership relations", fedoraId);

            if (propsConfig.isAutoVersioningEnabled()) {
                modifyDCAutoversioned(txId, fedoraResc, containerProperties);
            } else {
                modifyDCOnDemandVersioning(txId, fedoraResc);
            }
            return;
        }
        // TODO handle modification of IndirectContainers and proxies
    }

    private void modifyDCAutoversioned(final String txId, final FedoraResource dcResc,
            final DirectContainerProperties containerProperties) {
        final var dcId = dcResc.getFedoraId();
        final var dcLastModified = dcResc.getLastModifiedDate();
        // Delete/end existing membership from this container
        indexManager.endMembershipForSource(txId, dcResc.getFedoraId(), dcLastModified);

        // Add updated membership properties for all non-tombstone children
        dcResc.getChildren()
                .filter(child -> !(child instanceof Tombstone))
                .forEach(child -> {
                    final var newMembership = generateMembership(containerProperties, child);
                    indexManager.addMembership(txId, dcId, child.getFedoraId(),
                            newMembership, dcLastModified);
                });
    }

    private void modifyDCOnDemandVersioning(final String txId, final FedoraResource dcResc) {
        final var dcId = dcResc.getFedoraId();
        final var mementoDatetimes = dcResc.getTimeMap().listMementoDatetimes();
        final Instant lastVersionDatetime;
        if (mementoDatetimes.size() == 0) {
            // If no previous versions of DC, then cleanup and repopulate everything
            lastVersionDatetime = null;
        } else {
            // If at least one past version, then reindex membership involving the last version and after
            lastVersionDatetime = mementoDatetimes.get(mementoDatetimes.size() - 1);
        }
        indexManager.deleteMembershipForSourceAfter(txId, dcId, lastVersionDatetime);
        populateMembershipHistory(txId, dcResc, lastVersionDatetime);
    }

    private Triple generateMembership(final DirectContainerProperties properties, final FedoraResource childResc) {
        final var childRdfResc = getRdfResource(childResc.getFedoraId());

        final Node memberNode;
        if (ContainerType.Indirect.equals(properties.containerType)) {
            // Special case to use child as the member subject
            if (properties.insertedContentRelation.equals(RdfLexicon.MEMBER_SUBJECT)) {
                memberNode = childRdfResc.asNode();
            } else {
                // get the member node from the child resource's insertedContentRelation property
                final var childModelResc = getRdfResource(childResc);
                final Statement stmt = childModelResc.getProperty(properties.insertedContentRelation);
                // Ignore the child if it is missing the insertedContentRelation or its object is not a resource
                if (stmt == null || !(stmt.getObject() instanceof Resource)) {
                    return null;
                }
                memberNode = stmt.getResource().asNode();
            }
        } else {
            memberNode = childRdfResc.asNode();
        }

        return generateMembershipTriple(properties.membershipResource, memberNode,
                properties.hasMemberRelation, properties.isMemberOfRelation);
    }

    private Triple generateMembershipTriple(final Node membership, final Node member,
            final Node hasMemberRel, final Node memberOfRel) {
        if (memberOfRel != null) {
            return new Triple(member, memberOfRel, membership);
        } else {
            return new Triple(membership, hasMemberRel, member);
        }
    }

    private Resource getRdfResource(final FedoraResource fedoraResc) {
        final var model = fedoraResc.getTriples().collect(toModel());
        return model.getResource(fedoraResc.getFedoraId().getFullId());
    }

    private Resource getRdfResource(final FedoraId fedoraId) {
        return org.apache.jena.rdf.model.ResourceFactory.createResource(fedoraId.getFullId());
    }

    private FedoraResource getFedoraResource(final String txId, final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(txId, fedoraId);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e.getMessage(), e);
        }
    }

    private FedoraResource getParentResource(final FedoraResource resc) {
        try {
            return resc.getParent();
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void resourceDeleted(final String txId, final FedoraId fedoraId) {
        // delete DirectContainer, end all membership for that source
        FedoraResource fedoraResc;
        try {
            fedoraResc = getFedoraResource(txId, fedoraId);
        } catch (final PathNotFoundRuntimeException e) {
            log.debug("Deleted resource {} does not have a tombstone, cleanup any references", fedoraId);
            indexManager.deleteMembershipReferences(txId, fedoraId);
            return;
        }
        if (fedoraResc instanceof Tombstone) {
            fedoraResc = ((Tombstone) fedoraResc).getDeletedObject();
        }

        final var resourceContainerType = getContainerType(fedoraResc);
        if (ContainerType.Direct.equals(resourceContainerType)) {
            indexManager.endMembershipForSource(txId, fedoraId, fedoraResc.getLastModifiedDate());
        }

        // delete child of DirectContainer, clear from tx and end existing
        final var parentResc = getParentResource(fedoraResc);
        final var parentContainerType = getContainerType(parentResc);

        if (parentContainerType != null) {
            log.debug("Ending membership in tx {} for {} at {}",
                    txId, parentResc.getFedoraId(), fedoraResc.getLastModifiedDate());
            indexManager.endMembershipFromChild(txId, parentResc.getFedoraId(), fedoraResc.getFedoraId(),
                    fedoraResc.getLastModifiedDate());
        }
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

    @Transactional
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
    @Transactional
    public void populateMembershipHistory(final String txId, final FedoraId containerId) {
        final FedoraResource fedoraResc = getFedoraResource(txId, containerId);
        final var containerType = getContainerType(fedoraResc);

        if (containerType != null) {
            populateMembershipHistory(txId, fedoraResc, null);
        }
    }

    private void populateMembershipHistory(final String txId, final FedoraResource fedoraResc,
            final Instant afterTime) {
        final var containerId = fedoraResc.getFedoraId();
        final var propertyTimeline = makePropertyTimeline(fedoraResc);
        final List<DirectContainerProperties> timeline;
        // If provided, filter the timeline to just entries active on or after the specified time
        if (afterTime != null) {
            timeline = propertyTimeline.stream().filter(e -> e.startDatetime.compareTo(afterTime) >= 0
                    || e.endDatetime.compareTo(afterTime) >= 0)
                .collect(Collectors.toList());
        } else {
            timeline = propertyTimeline;
        }

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
            var timelineStream = timeline.stream()
                    .filter(e -> e.endDatetime.compareTo(memberCreated) > 0);
            // If the member was deleted, then reduce timeline to states before the deletion
            if (memberDeleted) {
                timelineStream = timelineStream.filter(e -> e.startDatetime.compareTo(memberModified) < 0);
            }
            // Index each addition or change to the membership generated by this member
            timelineStream.forEach(e -> {
                generateMembership(e, member);
                // Start time of the membership is the later of member creation or membership resc memento time
                indexManager.addMembership(txId, containerId, member.getFedoraId(),
                        generateMembershipTriple(e.membershipResource,
                                memberNode, e.hasMemberRelation, e.isMemberOfRelation),
                        instantMax(memberCreated, e.startDatetime),
                        instantMin(memberEnd, e.endDatetime));
            });
        });
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
                .map(memento -> new DirectContainerProperties(memento))
                .collect(Collectors.toCollection(ArrayList::new));
        // For versioning on demand, add the head version to the timeline
        if (!propsConfig.isAutoVersioningEnabled()) {
            entryList.add(new DirectContainerProperties(fedoraResc));
        }
        // First entry starts at creation time of the resource
        entryList.get(0).startDatetime = fedoraResc.getCreatedDate();

        // Reduce timeline to entries where significant properties change
        final var changeEntries = new ArrayList<DirectContainerProperties>();
        var curr = entryList.get(0);
        changeEntries.add(curr);
        for (int i = 1; i < entryList.size(); i++) {
            final var next = entryList.get(i);
            if (!Objects.equals(next.membershipResource, curr.membershipResource)
                    || !Objects.equals(next.hasMemberRelation, curr.hasMemberRelation)
                    || !Objects.equals(next.isMemberOfRelation, curr.isMemberOfRelation)) {
                // Adjust the end the previous entry before the next state begins
                curr.endDatetime = next.startDatetime;
                changeEntries.add(next);
                curr = next;
            }
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
        public Property insertedContentRelation;
        public ContainerType containerType;
        public Instant startDatetime;
        public Instant endDatetime = NO_END_INSTANT;

        /**
         * @param fedoraResc resource/memento from which the properties will be extracted
         */
        public DirectContainerProperties(final FedoraResource fedoraResc) {
            this.containerType = getContainerType(fedoraResc);
            if (containerType == null) {
                return;
            }
            startDatetime = fedoraResc.isMemento() ?
                    fedoraResc.getMementoDatetime() : fedoraResc.getLastModifiedDate();
            fedoraResc.getTriples().forEach(triple -> {
                if (RdfLexicon.MEMBERSHIP_RESOURCE.asNode().equals(triple.getPredicate())) {
                    membershipResource = triple.getObject();
                } else if (RdfLexicon.HAS_MEMBER_RELATION.asNode().equals(triple.getPredicate())) {
                    hasMemberRelation = triple.getObject();
                } else if (RdfLexicon.IS_MEMBER_OF_RELATION.asNode().equals(triple.getPredicate())) {
                    isMemberOfRelation = triple.getObject();
                } else if (RdfLexicon.INSERTED_CONTENT_RELATION.asNode().equals(triple.getPredicate())) {
                    insertedContentRelation = createProperty(triple.getObject().getURI());
                }
            });
        }
    }

    private static ContainerType getContainerType(final FedoraResource fedoraResc) {
        if (!(fedoraResc instanceof Container)) {
            return null;
        }
        if (fedoraResc.hasType(RdfLexicon.INDIRECT_CONTAINER.getURI())) {
            return ContainerType.Indirect;
        }

        if (fedoraResc.hasType(RdfLexicon.DIRECT_CONTAINER.getURI())) {
            return ContainerType.Direct;
        }

        return null;
    }

    @Override
    public Instant getLastUpdatedTimestamp(final String txId, final FedoraId fedoraId) {
        return indexManager.getLastUpdated(txId, fedoraId);
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
