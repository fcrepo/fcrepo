
/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import org.apache.jena.rdf.model.Model;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;

import java.util.List;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * This class mediates update operations between the kernel and persistent storage layers
 * @author bseeger
 */
@Component
public class ReplacePropertiesServiceImpl extends AbstractService implements ReplacePropertiesService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private RdfSourceOperationFactory factory;

    @Inject
    private NonRdfSourceOperationFactory nonRdfFactory;

    @Inject
    private Cache<String, Optional<ACLHandle>> authHandleCache;

    @Override
    public void perform(final Transaction tx,
                        final String userPrincipal,
                        final FedoraId fedoraId,
                        final Model inputModel) throws MalformedRdfException {
        try {
            final PersistentStorageSession pSession = psManager.getSession(tx);

            final var headers = pSession.getHeaders(fedoraId, null);
            final var interactionModel = headers.getInteractionModel();

            ensureValidDirectContainer(fedoraId, interactionModel, inputModel);
            ensureValidACLAuthorization(inputModel);
            // Extract triples which impact the headers of binary resources from incoming description RDF
            final BinaryHeaderDetails binHeaders = extractNonRdfSourceHeaderTriples(fedoraId, inputModel);

            final var rdfStream = fromModel(
                    inputModel.createResource(fedoraId.getFullDescribedId()).asNode(), inputModel);
            final var serverManagedMode = fedoraPropsConfig.getServerManagedPropsMode();

            // create 2 updates -- one for the properties coming in and one for and server managed properties
            final ResourceOperation primaryOp;
            final Optional<ResourceOperation> secondaryOp;
            if (fedoraId.isDescription()) {
                primaryOp = factory.updateBuilder(tx, fedoraId, serverManagedMode)
                                   .userPrincipal(userPrincipal)
                                   .triples(rdfStream)
                                   .build();

                // we need to use the description id until we write the headers in order to resolve properties
                secondaryOp = Optional.of(nonRdfFactory.updateHeadersBuilder(tx, fedoraId, serverManagedMode)
                                                 .relaxedProperties(inputModel)
                                                 .userPrincipal(userPrincipal)
                                                 .filename(binHeaders.getFilename())
                                                 .mimeType(binHeaders.getMimetype())
                                                 .build());
            } else {
                primaryOp = factory.updateBuilder(tx, fedoraId, serverManagedMode)
                                   .relaxedProperties(inputModel)
                                   .userPrincipal(userPrincipal)
                                   .triples(rdfStream)
                                   .build();
                secondaryOp = Optional.empty();
            }

            lockArchivalGroupResource(tx, pSession, fedoraId);
            tx.lockResource(fedoraId);
            if (RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI.equals(interactionModel)) {
                tx.lockResource(fedoraId.asBaseId());
            }

            pSession.persist(primaryOp);

            userTypesCache.cacheUserTypes(fedoraId,
                    fromModel(inputModel.getResource(fedoraId.getFullId()).asNode(), inputModel), pSession.getId());

            updateReferences(tx, fedoraId, userPrincipal, inputModel);
            membershipService.resourceModified(tx, fedoraId);
            searchIndex.addUpdateIndex(tx, pSession.getHeaders(fedoraId, null));
            recordEvent(tx, fedoraId, primaryOp);
            secondaryOp.ifPresent(operation -> updateBinaryHeaders(tx, pSession, operation));
            if (fedoraId.isAcl()) {
                // Flush ACL cache on any ACL creation/update/deletion.
                authHandleCache.invalidateAll();
            }
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(String.format("failed to replace resource %s",
                    fedoraId), ex);
        }
    }

    private void updateBinaryHeaders(final Transaction tx,
                                     final PersistentStorageSession pSession,
                                     final ResourceOperation operation) {
        pSession.persist(operation);
        recordEvent(tx, operation.getResourceId(), operation);
    }

    protected BinaryHeaderDetails extractNonRdfSourceHeaderTriples(final FedoraId fedoraId, final Model model) {
        if (!fedoraId.isDescription()) {
            return null;
        }
        final BinaryHeaderDetails details = new BinaryHeaderDetails();
        final Resource binResc = model.getResource(fedoraId.getBaseId());
        if (binResc.hasProperty(RdfLexicon.HAS_MIME_TYPE)) {
            final List<Statement> mimetypes = binResc.listProperties(RdfLexicon.HAS_MIME_TYPE).toList();
            if (mimetypes.size() > 1) {
                throw new MalformedRdfException("Invalid RDF, cannot provided multiple values for property "
                        + RdfLexicon.HAS_MIME_TYPE);
            }
            details.setMimetype(mimetypes.get(0).getString());
            binResc.removeAll(RdfLexicon.HAS_MIME_TYPE);
        }
        if (binResc.hasProperty(RdfLexicon.HAS_ORIGINAL_NAME)) {
            final List<Statement> filenames = binResc.listProperties(RdfLexicon.HAS_ORIGINAL_NAME).toList();
            if (filenames.size() > 1) {
                throw new MalformedRdfException("Invalid RDF, cannot provided multiple values for property "
                        + RdfLexicon.HAS_ORIGINAL_NAME);
            }
            details.setFilename(filenames.get(0).getString());
            binResc.removeAll(RdfLexicon.HAS_ORIGINAL_NAME);
        }
        return details;
    }

    private static class BinaryHeaderDetails {
        private String mimetype;
        private String filename;

        public String getMimetype() {
            return mimetype;
        }

        public void setMimetype(final String mimetype) {
            this.mimetype = mimetype;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(final String filename) {
            this.filename = filename;
        }
    }
}