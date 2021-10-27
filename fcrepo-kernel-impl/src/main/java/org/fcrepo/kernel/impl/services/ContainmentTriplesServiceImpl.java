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

}
