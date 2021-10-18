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
package org.fcrepo.kernel.impl.operations;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperationBuilder;
import org.springframework.stereotype.Component;

/**
 * Factory for operations to update non-rdf sources
 *
 * @author bbpennel
 */
@Component
public class NonRdfSourceOperationFactoryImpl implements NonRdfSourceOperationFactory {

    @Override
    public UpdateNonRdfSourceOperationBuilder updateExternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
                                                                          final String handling,
                                                                          final URI contentUri) {
        return new UpdateNonRdfSourceOperationBuilder(transaction, rescId, handling, contentUri);
    }

    @Override
    public UpdateNonRdfSourceOperationBuilder updateInternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
                                                                    final InputStream contentStream) {
        return new UpdateNonRdfSourceOperationBuilder(transaction, rescId, contentStream);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder createExternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
            final String handling, final URI contentUri) {
        return new CreateNonRdfSourceOperationBuilderImpl(transaction, rescId, handling, contentUri);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder createInternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
            final InputStream contentStream) {
        return new CreateNonRdfSourceOperationBuilderImpl(transaction, rescId, contentStream);
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder updateHeadersBuilder(final Transaction transaction,
                                                               final FedoraId rescId,
                                                               final ServerManagedPropsMode serverManagedPropsMode) {
        return new UpdateNonRdfSourceHeadersOperationBuilderImpl(transaction, rescId, serverManagedPropsMode);
    }
}
