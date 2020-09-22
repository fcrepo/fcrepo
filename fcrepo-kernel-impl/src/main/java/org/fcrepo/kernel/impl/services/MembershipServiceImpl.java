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

import static org.fcrepo.kernel.api.RdfCollectors.toModel;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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

        if (memberOfRel != null) {
            return new Triple(memberRdfResc.asNode(), memberOfRel.asNode(), membershipResc.asNode());
        } else {
            return new Triple(membershipResc.asNode(), hasMemberRel.asNode(), memberRdfResc.asNode());
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
        final var subject = NodeFactory.createURI(subjectId.getFullId());
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
