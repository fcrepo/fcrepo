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

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.services.ReplaceBinariesService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import static java.lang.String.format;

/**
 * Implementation of a service for replacing/updating binary resources
 *
 * @author bbpennel
 */
@Component
public class ReplaceBinariesServiceImpl extends AbstractService implements ReplaceBinariesService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private NonRdfSourceOperationFactory factory;

    @Override
    public void perform(final String txId,
                        final String userPrincipal,
                        final FedoraId fedoraId,
                        final String filename,
                        final String contentType,
                        final Collection<URI> digests,
                        final InputStream contentBody,
                        final Long size,
                        final ExternalContent externalContent) {
        try {
            final PersistentStorageSession pSession = this.psManager.getSession(txId);

            hasRestrictedPath(fedoraId.getFullId());

            String mimeType = contentType;
            final NonRdfSourceOperationBuilder builder;
            if (externalContent == null) {
                builder = factory.updateInternalBinaryBuilder(fedoraId, contentBody);
            } else {
                builder = factory.updateExternalBinaryBuilder(fedoraId,
                        externalContent.getHandling(),
                        externalContent.getURI());

                if (externalContent.getContentType() != null) {
                    mimeType = externalContent.getContentType();
                }
            }

            builder.mimeType(mimeType)
                   .contentSize(size)
                   .filename(filename)
                   .contentDigests(digests)
                   .userPrincipal(userPrincipal);
            final var replaceOp = builder.build();

            pSession.persist(replaceOp);
            recordEvent(txId, fedoraId, replaceOp);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(format("failed to replace binary %s",
                  fedoraId), ex);
        }
    }

}
