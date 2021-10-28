/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;


/**
 * Implementation of an LDP Container resource
 *
 * @author bbpennel
 */
public class ContainerImpl extends FedoraResourceImpl implements Container {

    private static final URI RDF_SOURCE_URI = URI.create(RDF_SOURCE.toString());
    private static final URI CONTAINER_URI = URI.create(CONTAINER.toString());
    private static final URI FEDORA_CONTAINER_URI = URI.create(FEDORA_CONTAINER.toString());

    /**
     * Construct the container
     *
     * @param fedoraID internal identifier
     * @param transaction transaction
     * @param pSessionManager session manager
     * @param resourceFactory resource factory
     * @param userTypesCache the user types cache
     */
    public ContainerImpl(final FedoraId fedoraID,
                         final Transaction transaction,
                         final PersistentStorageSessionManager pSessionManager,
                         final ResourceFactory resourceFactory,
                         final UserTypesCache userTypesCache) {
        super(fedoraID, transaction, pSessionManager, resourceFactory, userTypesCache);
    }

    @Override
    public FedoraResource getDescribedResource() {
        return this;
    }

    @Override
    public List<URI> getSystemTypes(final boolean forRdf) {
        var types = resolveSystemTypes(forRdf);

        if (types == null) {
            types = super.getSystemTypes(forRdf);
            types.add(RDF_SOURCE_URI);
            types.add(CONTAINER_URI);
            types.add(FEDORA_CONTAINER_URI);
        }

        return types;
    }

    @Override
    public Stream<FedoraResource> getChildren(final Boolean recursive) {
        return resourceFactory.getChildren(transaction, fedoraId);
    }
}
