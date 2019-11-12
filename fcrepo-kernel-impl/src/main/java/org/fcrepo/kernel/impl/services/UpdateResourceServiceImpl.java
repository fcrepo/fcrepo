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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;

import org.fcrepo.kernel.api.models.ExternalContent;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.UpdateResourceService;
import org.fcrepo.kernel.impl.operations.AbstractResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import javax.inject.Inject;

import java.io.InputStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class UpdateResourceServiceImpl extends AbstractService implements UpdateResourceService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Inject
    private NonRdfSourceOperationFactory nonRdfSourceOperationFactory;

    @Override
    public void perform(final String txId, final String fedoraId, final String filename,
            final String contentType, final Collection<String> digest, final InputStream requestBody,
            final long size, final ExternalContent externalContent) {

        final PersistentStorageSession pSession = this.psManager.getSession(txId);
        final Collection<URI> uriDigests = digest.stream().map(URI::create).collect(Collectors.toCollection(HashSet::new));

        // TODO: Verify permissions Write or Append permissions on parent.

        final NonRdfSourceOperationBuilder builder;
        if (externalContent == null) {
            builder = nonRdfSourceOperationFactory.updateInternalBinaryBuilder(fedoraId, requestBody)
                        .filename(filename)
                        .contentSize(size);
        } else {
            builder = nonRdfSourceOperationFactory.updateExternalBinaryBuilder(fedoraId, externalContent.getHandling(),
                    URI.create(externalContent.getURL()));
        }
        final ResourceOperation updateOp = builder.contentDigests(uriDigests).mimeType(contentType).build();

        // Set server managed is only on AbstractResourceOperation.
        ((AbstractResourceOperation)updateOp).setServerManagedProperties(getServerManagedStream(fedoraId));

        try {
            pSession.persist(updateOp);
        } catch (PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to update resource %s", fedoraId), exc);
        }

    }

    @Override
    public void perform(final String txId, final String fedoraId, final String contentType, final Model model) {
        final PersistentStorageSession pSession = this.psManager.getSession(txId);

        final RdfStream stream = fromModel(model.getResource(fedoraId).asNode(), model);

        final ResourceOperation updateOp = rdfSourceOperationFactory.updateBuilder(fedoraId)
                    .triples(stream).build();

        // Set server managed is only on AbstractResourceOperation.
        ((AbstractResourceOperation)updateOp).setServerManagedProperties(getServerManagedStream(fedoraId));

        try {
            pSession.persist(updateOp);
        } catch (PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to update resource %s", fedoraId), exc);
        }

    }


    @Override
    void populateServerManagedTriples(final String fedoraId) {
        super.populateServerManagedTriples(fedoraId);
        final ZonedDateTime now = ZonedDateTime.now();
        serverManagedProperties.add(new Triple(
                asNode(fedoraId),
                asNode(FEDORA_LASTMODIFIED),
                asLiteral(now.format(DateTimeFormatter.RFC_1123_DATE_TIME), XSDDatatype.XSDdateTime))
        );
        // TODO: get current user.
        // this.serverManagedProperties.add(new Triple(
        //      asNode(fedoraId),
        //      asNode(FEDORA_LASTMODIFIEDBY),
        //      asLiteral(user))
        // );
    }
}
