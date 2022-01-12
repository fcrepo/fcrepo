
/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.fcrepo.kernel.api.RdfCollectors.toModel;

import javax.inject.Inject;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.kernel.api.services.UpdatePropertiesService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.springframework.stereotype.Component;

/**
 * This class implements the update properties operation.
 *
 * @author dbernstein
 */
@Component
public class UpdatePropertiesServiceImpl extends AbstractService implements UpdatePropertiesService {

    @Inject
    private ReplacePropertiesService replacePropertiesService;

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Override
    public void updateProperties(final Transaction tx, final String userPrincipal,
                                 final FedoraId fedoraId, final String sparqlUpdateStatement)
            throws MalformedRdfException, AccessDeniedException {
        try {
            final var psession = persistentStorageSessionManager.getSession(tx);
            final var triples = psession.getTriples(fedoraId, null);
            final Model model = triples.collect(toModel());
            final UpdateRequest request = UpdateFactory.create(sparqlUpdateStatement, fedoraId.getFullId());
            UpdateAction.execute(request, model);
            replacePropertiesService.perform(tx, userPrincipal, fedoraId, model);
        } catch (final PersistentItemNotFoundException ex) {
            throw new ItemNotFoundException(ex.getMessage(), ex);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(ex.getMessage(), ex);
        }

    }
}
