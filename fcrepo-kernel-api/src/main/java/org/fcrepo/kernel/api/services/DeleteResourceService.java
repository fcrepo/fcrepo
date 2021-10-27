/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * A service interface for deleting Fedora resources.
 * @author dbernstein
 * @since 6.0.0
 */
public interface DeleteResourceService {
  /**
   * Delete the specified resource
   *
   * @param tx the transaction associated with the operation
   * @param fedoraResource The Fedora resource to delete
   * @param userPrincipal the principal of the user performing the operation
   */
  void perform(final Transaction tx, final FedoraResource fedoraResource, final String userPrincipal);
}
