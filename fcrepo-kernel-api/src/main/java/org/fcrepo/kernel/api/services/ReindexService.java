/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * A service which handles reindexing operations
 *
 * @author dbernstein
 */
public interface ReindexService {
    void reindexByFedoraId(Transaction transaction, String principal, FedoraId fedoraId);
}
