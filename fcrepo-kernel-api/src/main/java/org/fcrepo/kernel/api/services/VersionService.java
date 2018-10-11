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
package org.fcrepo.kernel.api.services;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;

/**
 * Service for creating versions of resources
 *
 * @author bbpennel
 * @author whikloj
 * @since Feb 19, 2014
 */
public interface VersionService {

    /**
     * To format a datetime for use as a Memento path.
     */
    DateTimeFormatter MEMENTO_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("UTC"));

    /**
     * To format a datetime as RFC-1123 with correct timezone.
     */
    DateTimeFormatter MEMENTO_RFC_1123_FORMATTER = RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));

    /**
     * Explicitly creates a version for the resource at the path provided.
     *
     * @param session the session in which the resource resides
     * @param resource the resource to version
     * @param idTranslator translator for producing URI of resources
     * @param dateTime the date/time of the version
     * @return the version
     */
    FedoraResource createVersion(FedoraSession session, FedoraResource resource,
            IdentifierConverter<Resource, FedoraResource> idTranslator, Instant dateTime);

    /**
     * Explicitly creates a version for the resource at the path provided for the date/time provided.
     *
     * @param session the session in which the resource resides
     * @param resource the resource to version
     * @param idTranslator translator for producing URI of resources
     * @param dateTime the date/time of the version
     * @param rdfInputStream if provided, this stream will provide the properties of the new memento. If null, then
     *        the state of the current resource will be used.
     * @param rdfFormat RDF language format name
     * @return the version
     */
    FedoraResource createVersion(FedoraSession session, FedoraResource resource,
            IdentifierConverter<Resource, FedoraResource> idTranslator, Instant dateTime, InputStream rdfInputStream,
            Lang rdfFormat);

    /**
     * Explicitly creates a version of a binary resource. If contentStream is provided, then it will be used as the
     * content of the binary memento. Otherwise, the current state of the binary will be used.
     *
     * @param session the session in which the resource resides
     * @param resource the binary resource to version
     * @param dateTime the date/time of the version
     * @param contentStream if provided, the content in this stream will be used as the content of the new binary
     *        memento. If null, then the current state of the binary will be used.
     * @param checksums Collection of checksum URIs of the content (optional)
     * @param storagePolicyDecisionPoint optional storage policy
     * @throws InvalidChecksumException if there are errors applying checksums
     * @return the version
     */
    FedoraBinary createBinaryVersion(FedoraSession session,
            FedoraBinary resource,
            Instant dateTime,
            InputStream contentStream,
            Collection<URI> checksums,
            StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException;

    /**
     * Explicitly creates a version of a binary resource from the current state of that binary.
     *
     * @param session the session in which the resource resides
     * @param resource the binary resource to version
     * @param dateTime the date/time of the version
     * @param storagePolicyDecisionPoint optional storage policy
     * @return the version
     * @throws InvalidChecksumException on error
     */
    FedoraBinary createBinaryVersion(FedoraSession session, FedoraBinary resource, Instant dateTime,
            StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException;

    /**
     * @param session the session in which the resource resides
     * @param resource the binary resource to version
     * @param dateTime the date/time of the version
     * @param checksums Collection of checksum URIs of the content (optional)
     * @param externalHandling What type of handling the external resource needs (proxy or redirect)
     * @param externalUrl Url for the external resourcej
     * @return the version
     * @throws InvalidChecksumException if there are errors applying checksums
     */
    FedoraBinary createExternalBinaryVersion(final FedoraSession session,
            final FedoraBinary resource,
            final Instant dateTime,
            final Collection<URI> checksums,
            final String externalHandling,
            final String externalUrl)
            throws InvalidChecksumException;
}
