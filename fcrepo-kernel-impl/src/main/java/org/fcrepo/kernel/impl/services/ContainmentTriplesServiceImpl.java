/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ContainmentTriplesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;

/**
 * Containment Triples service.
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class ContainmentTriplesServiceImpl implements ContainmentTriplesService {

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Override
    public Stream<Triple> get(final Transaction tx, final FedoraResource resource) {
        final var fedoraId = resource.getFedoraId();
        final var nodeUri = fedoraId.isMemento() ? fedoraId.getBaseId() : fedoraId.getFullId();
        final Node currentNode = createURI(nodeUri);
        return containmentIndex.getContains(tx, fedoraId).map(c ->
                new Triple(currentNode, CONTAINS.asNode(), createURI(c)));
    }

    @Override
    public Stream<Triple> getContainedBy(final Transaction tx, final FedoraResource objectResource) {
        final var objectId = objectResource.getFedoraId();
        final String nodeUri;
        if (objectId.isMemento() || objectId.isDescription()) {
            nodeUri = objectId.getBaseId();
        } else {
            nodeUri = objectId.getFullId();
        }
        final var containedBy = containmentIndex.getContainedBy(tx, objectId);
        if (containedBy == null) {
            return Stream.empty();
        }
        return Stream.of(new Triple(createURI(containedBy),
                        CONTAINS.asNode(),
                        createURI(nodeUri)));
    }
}
