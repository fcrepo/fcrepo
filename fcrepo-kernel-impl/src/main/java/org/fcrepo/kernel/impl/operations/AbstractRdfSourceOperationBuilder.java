/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.checkTripleForDisallowed;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import org.apache.jena.rdf.model.Model;

/**
 * Abstract builder for interacting with an Rdf Source Operation Builder
 * @author bseeger
 */
public abstract class AbstractRdfSourceOperationBuilder extends AbstractRelaxableResourceOperationBuilder
                                                        implements RdfSourceOperationBuilder {

    /**
     * Holds the stream of user's triples.
     */
    protected RdfStream tripleStream;

    /**
     * Principal of the user performing the operation
     */
    protected String userPrincipal;

    /**
     * The interaction model of this resource, null in case of update.
     */
    protected final String interactionModel;

    protected AbstractRdfSourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                final String model,
                                                final ServerManagedPropsMode serverManagedPropsMode) {
        super(transaction, rescId, serverManagedPropsMode);
        interactionModel = model;
    }

    @Override
    public RdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    @Override
    public RdfSourceOperationBuilder triples(final RdfStream triples) {
        if (this.serverManagedPropsMode.equals(ServerManagedPropsMode.RELAXED)) {
            // Filter out server managed properties, they should only matter to the relaxedProperties method.
            this.tripleStream = new DefaultRdfStream(triples.topic(), triples.filter(t -> {
                try {
                    checkTripleForDisallowed(t);
                } catch (final Exception e) {
                    return false;
                }
                return true;
            }));
        } else {
            this.tripleStream = triples;
        }
        return this;
    }

    @Override
    public RdfSourceOperationBuilder relaxedProperties(final Model model) {
        return (RdfSourceOperationBuilder) super.relaxedProperties(model);
    }
}
