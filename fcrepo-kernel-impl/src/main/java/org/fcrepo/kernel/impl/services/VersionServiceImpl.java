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

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;

/**
 * Implementation of {@link VersionService}
 *
 * @author dbernstein
 */
@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    @Override
    public FedoraResource createVersion(final Transaction transaction, final FedoraResource resource,
                                        final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final Instant dateTime) {
        return null;
    }

    @Override
    public FedoraResource createVersion(final Transaction transaction, final FedoraResource resource,
                                        final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final Instant dateTime, final InputStream rdfInputStream,
                                        final Lang rdfFormat) {
        return null;
    }

    @Override
    public Binary createBinaryVersion(final Transaction transaction, final Binary resource, final Instant dateTime,
                                      final InputStream contentStream, final Collection<URI> checksums,
                                      final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {
        return null;
    }

    @Override
    public Binary createBinaryVersion(final Transaction transaction, final Binary resource, final Instant dateTime,
                                      final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {
        return null;
    }

    @Override
    public Binary createExternalBinaryVersion(final Transaction transaction, final Binary resource,
                                              final Instant dateTime, final Collection<URI> checksums,
                                              final String externalHandling, final String externalUrl)
            throws InvalidChecksumException {
        return null;
    }
}
