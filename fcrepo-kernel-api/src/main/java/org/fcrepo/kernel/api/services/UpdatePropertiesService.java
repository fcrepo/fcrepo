/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * @author peichman
 * @since 6.0.0
 */
public interface UpdatePropertiesService {
  /**
   * Update the provided properties with a SPARQL Update query. The updated
   * properties may be serialized to the persistence layer.
   *
   * @param tx the Transaction
   * @param userPrincipal the user performing the service
   * @param fedoraId the internal Id of the fedora resource to update
   * @param sparqlUpdateStatement sparql update statement
   * @throws MalformedRdfException if malformed rdf exception occurred
   * @throws AccessDeniedException if access denied in updating properties
   */
  void updateProperties(final Transaction tx,
                        final String userPrincipal,
                        final FedoraId fedoraId,
                        final String sparqlUpdateStatement) throws MalformedRdfException, AccessDeniedException;
}
