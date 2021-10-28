/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Tombstone class
 *
 * @author whikloj
 */
public class TombstoneImpl extends FedoraResourceImpl implements Tombstone {

    private FedoraResource originalResource;

    protected TombstoneImpl(final FedoraId fedoraID,
                            final Transaction transaction,
                            final PersistentStorageSessionManager pSessionManager,
                            final ResourceFactory resourceFactory,
                            final FedoraResource original) {
        super(fedoraID, transaction, pSessionManager, resourceFactory, null);
        this.originalResource = original;
    }

    @Override
    public FedoraResource getDeletedObject() {
        return originalResource;
    }

    @Override
    public FedoraId getFedoraId() {
        return this.originalResource.getFedoraId();
    }

    @Override
    public List<URI> getUserTypes() {
        return Collections.emptyList();
    }

    @Override
    public List<URI> getSystemTypes(final boolean forRdf) {
        return Collections.emptyList();
    }
}
