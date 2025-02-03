/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import javax.annotation.Nonnull;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An simple in-memory implementation of the {@link FedoraToOcflObjectIndex} used for testing
 *
 * @author pwinckles
 */
public class TestOcflObjectIndex implements FedoraToOcflObjectIndex {

    private static Logger LOGGER = LoggerFactory.getLogger(TestOcflObjectIndex.class);

    private Map<FedoraId, FedoraOcflMapping> fedoraOcflMappingMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public FedoraOcflMapping getMapping(final Transaction transaction, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException {

        LOGGER.debug("getting {}", fedoraResourceIdentifier);
        final FedoraOcflMapping m = fedoraOcflMappingMap.get(fedoraResourceIdentifier);
        if (m == null) {
            throw new FedoraOcflMappingNotFoundException(fedoraResourceIdentifier.getFullId());
        }

        return m;
    }

    @Override
    public FedoraOcflMapping addMapping(@Nonnull final Transaction transaction,
                                        final FedoraId fedoraResourceIdentifier,
                                        final FedoraId fedoraRootObjectResourceId,
                                        final String ocflObjectId) {
        FedoraOcflMapping mapping = fedoraOcflMappingMap.get(fedoraRootObjectResourceId);

        if (mapping == null) {
            mapping = new FedoraOcflMapping(fedoraRootObjectResourceId, ocflObjectId);
            fedoraOcflMappingMap.put(fedoraRootObjectResourceId, mapping);
        }

        if (!fedoraResourceIdentifier.equals(fedoraRootObjectResourceId)) {
            fedoraOcflMappingMap.put(fedoraResourceIdentifier, mapping);
        }

        LOGGER.debug("added mapping {} for {}", mapping, fedoraResourceIdentifier);
        return mapping;
    }

    @Override
    public void removeMapping(@Nonnull final Transaction transaction, final FedoraId fedoraResourceIdentifier) {
        fedoraOcflMappingMap.remove(fedoraResourceIdentifier);
    }

    @Override
    public void reset() {
        fedoraOcflMappingMap.clear();
    }

    @Override
    public void commit(@Nonnull final Transaction session) {

    }

    @Override
    public void rollback(@Nonnull final Transaction session) {

    }

    @Override
    public void clearAllTransactions() {

    }

}
