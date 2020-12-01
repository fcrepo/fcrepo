/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import javax.inject.Inject;
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
        final var txId = tx.getId();
        try {
            final PersistentStorageSession pSession = this.psManager.getSession(txId);

            String mimeType = contentType;
            long size = contentSize;
            final NonRdfSourceOperationBuilder builder;
            if (externalContent == null || externalContent.isCopy()) {
                var contentInputStream = contentBody;
                if (externalContent != null) {
                    LOGGER.debug("External content COPY '{}', '{}'", fedoraId, externalContent.getURL());
                    contentInputStream = externalContent.fetchExternalContent();
                }

                builder = factory.updateInternalBinaryBuilder(fedoraId, contentInputStream);
            } else {
                builder = factory.updateExternalBinaryBuilder(fedoraId,
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
            tx.lockResource(fedoraId.asDescription());

            pSession.persist(replaceOp);
            recordEvent(txId, fedoraId, replaceOp);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(format("failed to replace binary %s",
                  fedoraId), ex);
        }
    }

}
