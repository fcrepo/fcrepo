/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
import org.fcrepo.kernel.api.Transaction;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
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
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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
    public void resourceCreated(final Transaction tx, final FedoraId fedoraId) {
        final var fedoraResc = getFedoraResource(tx, fedoraId);

        // Only need to compute membership for created containers and binaries
        if (!(fedoraResc instanceof Container || fedoraResc instanceof Binary)) {
            return;
        }

        if (isChildOfRoot(fedoraResc)) {
            return;
        }

        final var parentResc = getParentResource(fedoraResc);
        final var containerProperties = new DirectContainerProperties(parentResc);

        if (containerProperties.containerType != null) {
            final var newMembership = generateMembership(containerProperties, fedoraResc);
            indexManager.addMembership(tx, parentResc.getFedoraId(), fedoraResc.getFedoraId(),
                    newMembership, fedoraResc.getCreatedDate());
        }
    }

    @Override
    public void resourceModified(final Transaction tx, final FedoraId fedoraId) {
        final var fedoraResc = getFedoraResource(tx, fedoraId);
        final var containerProperties = new DirectContainerProperties(fedoraResc);

        if (containerProperties.containerType != null) {
            log.debug("Modified DirectContainer {}, recomputing generated membership relations", fedoraId);

            if (propsConfig.isAutoVersioningEnabled()) {
                modifyDCAutoversioned(tx, fedoraResc, containerProperties);
            } else {
                modifyDCOnDemandVersioning(tx, fedoraResc);
            }
        }

        if (isChildOfRoot(fedoraResc)) {
            return;
        }

        final var parentResc = getParentResource(fedoraResc);
        final var parentProperties = new DirectContainerProperties(parentResc);

        // Handle updates of proxies in IndirectContainer
        if (ContainerType.Indirect.equals(parentProperties.containerType)) {
            modifyProxy(tx, fedoraResc, parentProperties);
        }
    }

    private void modifyProxy(final Transaction tx, final FedoraResource proxyResc,
            final DirectContainerProperties containerProperties) {
        final var lastModified = proxyResc.getLastModifiedDate();

        if (propsConfig.isAutoVersioningEnabled()) {
            // end existing stuff
            indexManager.endMembershipFromChild(tx, containerProperties.id, proxyResc.getFedoraId(), lastModified);
            // add new membership
        } else {
            final var mementoDatetimes = proxyResc.getTimeMap().listMementoDatetimes();
            final Instant lastVersionDatetime;
            if (mementoDatetimes.size() == 0) {
                // If no previous versions of proxy, then cleanup and repopulate everything
                lastVersionDatetime = null;
            } else {
                // If at least one past version, then reindex membership involving the last version and after
                lastVersionDatetime = mementoDatetimes.get(mementoDatetimes.size() - 1);
            }
            indexManager.deleteMembershipForProxyAfter(tx, containerProperties.id,
                    proxyResc.getFedoraId(), lastVersionDatetime);
        }

        indexManager.addMembership(tx, containerProperties.id, proxyResc.getFedoraId(),
                generateMembership(containerProperties, proxyResc), lastModified);
    }

    private void modifyDCAutoversioned(final Transaction tx, final FedoraResource dcResc,
            final DirectContainerProperties containerProperties) {
        final var dcId = dcResc.getFedoraId();
        final var dcLastModified = dcResc.getLastModifiedDate();
        // Delete/end existing membership from this container
        indexManager.endMembershipForSource(tx, dcResc.getFedoraId(), dcLastModified);

        // Add updated membership properties for all non-tombstone children
        dcResc.getChildren()
                .filter(child -> !(child instanceof Tombstone))
                .forEach(child -> {
                    final var newMembership = generateMembership(containerProperties, child);
                    indexManager.addMembership(tx, dcId, child.getFedoraId(),
                            newMembership, dcLastModified);
                });
    }

    private void modifyDCOnDemandVersioning(final Transaction tx, final FedoraResource dcResc) {
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
        indexManager.deleteMembershipForSourceAfter(tx, dcId, lastVersionDatetime);
        populateMembershipHistory(tx, dcResc, lastVersionDatetime);
    }

    private Triple generateMembership(final DirectContainerProperties properties, final FedoraResource childResc) {
        final var childRdfResc = getRdfResource(childResc.getFedoraId());

        final Node memberNode;
        if (ContainerType.Indirect.equals(properties.containerType)) {
            // Special case to use child as the member subject
            if (RdfLexicon.MEMBER_SUBJECT.equals(properties.insertedContentRelation)) {
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
            return Triple.create(member, memberOfRel, membership);
        } else {
            return Triple.create(membership, hasMemberRel, member);
        }
    }

    private Resource getRdfResource(final FedoraResource fedoraResc) {
        final var model = fedoraResc.getTriples().collect(toModel());
        return model.getResource(fedoraResc.getFedoraId().getFullId());
    }

    private Resource getRdfResource(final FedoraId fedoraId) {
        return org.apache.jena.rdf.model.ResourceFactory.createResource(fedoraId.getFullId());
    }

    private FedoraResource getFedoraResource(final Transaction transaction, final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(transaction, fedoraId);
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
    public void resourceDeleted(@Nonnull final Transaction transaction, final FedoraId fedoraId) {
        // delete DirectContainer, end all membership for that source
        FedoraResource fedoraResc;
        try {
            fedoraResc = getFedoraResource(transaction, fedoraId);
        } catch (final PathNotFoundRuntimeException e) {
            log.debug("Deleted resource {} does not have a tombstone, cleanup any references", fedoraId);
            indexManager.deleteMembershipReferences(transaction.getId(), fedoraId);
            return;
        }
        if (fedoraResc instanceof Tombstone) {
            fedoraResc = ((Tombstone) fedoraResc).getDeletedObject();
        }

        final var resourceContainerType = getContainerType(fedoraResc);
        if (resourceContainerType != null) {
            log.debug("Ending membership for deleted Direct/IndirectContainer {} in {}", fedoraId, transaction);
            indexManager.endMembershipForSource(transaction, fedoraId, fedoraResc.getLastModifiedDate());
        }

        if (isChildOfRoot(fedoraResc)) {
            return;
        }

        // delete child of DirectContainer, clear from tx and end existing
        final var parentResc = getParentResource(fedoraResc);
        final var parentContainerType = getContainerType(parentResc);

        if (parentContainerType != null) {
            log.debug("Ending membership for deleted proxy or member in tx {} for {} at {}",
                    transaction, parentResc.getFedoraId(), fedoraResc.getLastModifiedDate());
            indexManager.endMembershipFromChild(transaction, parentResc.getFedoraId(), fedoraResc.getFedoraId(),
                    fedoraResc.getLastModifiedDate());
        }
    }

    @Override
    public RdfStream getMembership(final Transaction tx, final FedoraId fedoraId) {
        final FedoraId subjectId;
        if (fedoraId.isDescription()) {
            subjectId = fedoraId.asBaseId();
        } else {
            subjectId = fedoraId;
        }
        final var subject = NodeFactory.createURI(subjectId.getBaseId());
        final var membershipStream = indexManager.getMembership(tx, subjectId);
        return new DefaultRdfStream(subject, membershipStream);
    }

    @Override
    public RdfStream getMembershipByObject(final Transaction tx, final FedoraId fedoraId) {
        final FedoraId objectId;
        if (fedoraId.isDescription()) {
            objectId = fedoraId.asBaseId();
        } else {
            objectId = fedoraId;
        }
        final var subject = NodeFactory.createURI(objectId.getBaseId());
        final var membershipStream = indexManager.getMembershipByObject(tx, objectId);
        return new DefaultRdfStream(subject, membershipStream);
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        indexManager.commitTransaction(tx);
    }

    @Override
    public void rollbackTransaction(final Transaction tx) {
        indexManager.deleteTransaction(tx);
    }

    @Override
    public void reset() {
        indexManager.clearIndex();
    }

    @Override
    public void populateMembershipHistory(@Nonnull final Transaction transaction, final FedoraId containerId) {
        final FedoraResource fedoraResc = getFedoraResource(transaction, containerId);
        final var containerType = getContainerType(fedoraResc);

        if (containerType != null) {
            populateMembershipHistory(transaction, fedoraResc, null);
        }
    }

    private void populateMembershipHistory(final Transaction tx, final FedoraResource fedoraResc,
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
            // must ensure the tx does not close before indexing is complete
            tx.refresh();

            final var memberDeleted = member instanceof Tombstone;
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
                final var membershipTriple = generateMembership(e, member);
                if (membershipTriple == null) {
                    log.warn("Skipping membership indexing for member {} of container {} "
                            + "due to missing insertedContentRelation", member.getFedoraId(), containerId);
                    return;
                }
                // Start time of the membership is the later of member creation or membership resc memento time
                indexManager.addMembership(tx, containerId, member.getFedoraId(),
                        membershipTriple,
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

    @Override
    public void clearAllTransactions() {
        indexManager.clearAllTransactions();
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
        public FedoraId id;
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
            id = fedoraResc.getFedoraId();
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
            if (hasMemberRelation == null && isMemberOfRelation == null) {
                hasMemberRelation = RdfLexicon.LDP_MEMBER.asNode();
            }
        }
    }

    private static ContainerType getContainerType(final FedoraResource fedoraResc) {
        if (!(fedoraResc instanceof Container)) {
            return null;
        }

        if (RdfLexicon.INDIRECT_CONTAINER.getURI().equals(fedoraResc.getInteractionModel())) {
            return ContainerType.Indirect;
        }

        if (RdfLexicon.DIRECT_CONTAINER.getURI().equals(fedoraResc.getInteractionModel())) {
            return ContainerType.Direct;
        }

        return null;
    }

    @Override
    public Instant getLastUpdatedTimestamp(final Transaction transaction, final FedoraId fedoraId) {
        return indexManager.getLastUpdated(transaction, fedoraId);
    }

    private boolean isChildOfRoot(final FedoraResource resource) {
        return resource.getParentId().isRepositoryRoot();
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
