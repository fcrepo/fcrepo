/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;

/**
 * Implementation of a non-rdf source description
 *
 * @author bbpennel
 */
public class NonRdfSourceDescriptionImpl extends FedoraResourceImpl implements NonRdfSourceDescription {

    private static final URI RDF_SOURCE_URI = URI.create(RDF_SOURCE.getURI());

    /**
     * Construct a description resource
     *
     * @param fedoraID internal identifier
     * @param transaction transaction
     * @param pSessionManager session manager
     * @param resourceFactory resource factory
     * @param userTypesCache the user types cache
     */
    public NonRdfSourceDescriptionImpl(final FedoraId fedoraID,
                                       final Transaction transaction,
                                       final PersistentStorageSessionManager pSessionManager,
                                       final ResourceFactory resourceFactory,
                                       final UserTypesCache userTypesCache) {
        super(fedoraID, transaction, pSessionManager, resourceFactory, userTypesCache);
    }

    @Override
    public String getId() {
        return getFedoraId().getResourceId();
    }

    @Override
    public FedoraResource getDescribedResource() {
        // Get a FedoraId for the binary
        FedoraId describedId = getFedoraId().asBaseId();
        if (getFedoraId().isMemento()) {
            describedId = describedId.asMemento(getFedoraId().getMementoInstant());
        }
        try {
            return this.resourceFactory.getResource(transaction, describedId);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<URI> getSystemTypes(final boolean forRdf) {
        var types = resolveSystemTypes(forRdf);

        if (types == null) {
            types = super.getSystemTypes(forRdf);
            // NonRdfSource gets the ldp:Resource and adds ldp:RDFSource types.
            types.add(RDF_SOURCE_URI);
        }

        return types;
    }

    @Override
    public RdfStream getTriples() {
        // Remap the subject to the described resource
        // TODO: With FCREPO-3819 and FCREPO-3820, this will no longer be necessary.
        //       But existing sites might have RDF with NonRdfSourceDescription subjects, so leave it for now.
        final Node describedID = createURI(this.getDescribedResource().getId());
        final Stream<Triple> triples = super.getTriples().map(t -> {
            if (t.getSubject().hasURI(this.getId())) {
                return Triple.create(describedID, t.getPredicate(), t.getObject());
            } else {
                return t;
            }
        });
        return new DefaultRdfStream(describedID, triples);
    }
}
