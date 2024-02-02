/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.services.ReplaceBinariesService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.MultiDigestInputStreamWrapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a service for replacing/updating binary resources
 *
 * @author bbpennel
 */
@Component
public class ReplaceBinariesServiceImpl extends AbstractService implements ReplaceBinariesService {

    private static final Logger LOGGER = getLogger(ReplaceBinariesServiceImpl.class);

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private NonRdfSourceOperationFactory factory;

    @Override
    public void perform(final Transaction tx,
                        final String userPrincipal,
                        final FedoraId fedoraId,
                        final String filename,
                        final String contentType,
                        final Collection<URI> digests,
                        final InputStream contentBody,
                        final long contentSize,
                        final ExternalContent externalContent) {
        try {
            final PersistentStorageSession pSession = this.psManager.getSession(tx);

            String mimeType = contentType;
            long size = contentSize;
            final NonRdfSourceOperationBuilder builder;
            if (externalContent == null || externalContent.isCopy()) {
                var contentInputStream = contentBody;
                if (externalContent != null) {
                    LOGGER.debug("External content COPY '{}', '{}'", fedoraId, externalContent.getURL());
                    contentInputStream = externalContent.fetchExternalContent();
                }

                builder = factory.updateInternalBinaryBuilder(tx, fedoraId, contentInputStream);
            } else {
                builder = factory.updateExternalBinaryBuilder(tx, fedoraId,
                        externalContent.getHandling(),
                        externalContent.getURI());

                if (contentSize == -1L) {
                    size = externalContent.getContentSize();
                }
                if (!digests.isEmpty()) {
                    final var multiDigestWrapper = new MultiDigestInputStreamWrapper(
                            externalContent.fetchExternalContent(),
                            digests,
                            Collections.emptyList());
                    multiDigestWrapper.checkFixity();
                }
            }

            if (externalContent != null && externalContent.getContentType() != null) {
                mimeType = externalContent.getContentType();
            }

            builder.mimeType(mimeType)
                   .contentSize(size)
                   .filename(filename)
                   .contentDigests(digests)
                   .userPrincipal(userPrincipal);
            final var replaceOp = builder.build();

            lockArchivalGroupResource(tx, pSession, fedoraId);
            tx.lockResource(fedoraId);
            // Descriptions are always under the binary, so just lock it.
            tx.lockResource(fedoraId.asDescription());

            pSession.persist(replaceOp);
            this.searchIndex.addUpdateIndex(tx, pSession.getHeaders(fedoraId, null));
            recordEvent(tx, fedoraId, replaceOp);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(format("failed to replace binary %s",
                    fedoraId), ex);
        }
    }

}
