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
import java.util.Collection;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;

/**
 * Interface for service to replace existing binaries
 *
 * @author mohideen
 */
public interface ReplaceBinariesService {

    /**
     * Replace an existing binary.
     *
     * @param tx The transaction for the request.
     * @param userPrincipal the user performing the service
     * @param fedoraId The internal identifier of the parent.
     * @param filename The filename of the binary.
     * @param contentType The content-type header or null if none.
     * @param digests The binary digest or null if none.
     * @param size The binary size.
     * @param contentBody The request body or null if none.
     * @param externalContent The external content handler or null if none.
     */
    void perform(Transaction tx,
                 String userPrincipal,
                 FedoraId fedoraId,
                 String filename,
                 String contentType,
                 Collection<URI> digests,
                 InputStream contentBody,
                 long size,
                 ExternalContent externalContent);
}
