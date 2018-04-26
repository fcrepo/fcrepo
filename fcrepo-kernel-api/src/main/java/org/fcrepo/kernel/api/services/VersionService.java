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

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Service for creating versions of resources
 *
 * @author bbpennel
 * @author whikloj
 * @since Feb 19, 2014
 */
public interface VersionService {

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
     * Explicitly creates a version of a binary resource. If no contentStream is provided, then
     *
     * @param session the session in which the resource resides
     * @param resource the resource to version
     * @param dateTime the date/time of the version
     * @param contentStream if provided, the content in this stream will be used as the content of the new binary
     *        memento. If null, then the current state of the binary will be used.
     * @param filename filename of the binary
     * @param mimetype mimetype of the binary
     * @param checksums Collection of checksum URIs of the content (optional)
     * @return the version
     */
    FedoraResource createBinaryVersion(FedoraSession session, FedoraResource resource, Instant dateTime,
            InputStream contentStream, String filename, String mimetype, Collection<URI> checksums);


}
