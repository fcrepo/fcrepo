/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_WEBAC_ACL_URI;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;

import java.util.Optional;

import javax.inject.Inject;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.services.WebacAclService;
import org.fcrepo.kernel.impl.models.WebacAclImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

import org.apache.jena.rdf.model.Model;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * Implementation of {@link WebacAclService}
 *
 * @author dbernstein
 */
@Component
public class WebacAclServiceImpl extends AbstractService implements WebacAclService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Inject
    private Cache<String, Optional<ACLHandle>> authHandleCache;

    @Override
    public WebacAcl find(final Transaction transaction, final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(transaction, fedoraId, WebacAclImpl.class);
        } catch (final PathNotFoundException exc) {
            throw new PathNotFoundRuntimeException(exc.getMessage(), exc);
        }
    }

    @Override
    public void create(final Transaction transaction, final FedoraId fedoraId, final String userPrincipal,
                                 final Model model) {
        final PersistentStorageSession pSession = this.psManager.getSession(transaction);

        ensureValidACLAuthorization(model);

        final RdfStream stream = fromModel(model.getResource(fedoraId.getFullId()).asNode(), model);

        final RdfSourceOperation createOp = rdfSourceOperationFactory
                .createBuilder(transaction, fedoraId, FEDORA_WEBAC_ACL_URI,
                        fedoraPropsConfig.getServerManagedPropsMode())
                .parentId(fedoraId.asBaseId())
                .triples(stream)
                .relaxedProperties(model)
                .userPrincipal(userPrincipal)
                .build();

        lockParent(transaction, pSession, fedoraId.asBaseId());
        transaction.lockResource(fedoraId);

        try {
            pSession.persist(createOp);
            recordEvent(transaction, fedoraId, createOp);
            // Flush ACL cache on any ACL creation/update/deletion.
            authHandleCache.invalidateAll();
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }
    }

}
