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

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;

/**
 * Operation for creating a new non-rdf source
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperation extends AbstractNonRdfSourceOperation implements CreateResourceOperation {

    private FedoraId parentId;

    /**
     * Constructor for external content.
     *
     * @param rescId the internal identifier.
     * @param externalContentURI the URI of the external content.
     * @param externalHandling the type of external content handling (REDIRECT, PROXY)
     */
    protected CreateNonRdfSourceOperation(final FedoraId rescId, final URI externalContentURI,
                                          final String externalHandling) {
        super(rescId, externalContentURI, externalHandling);
    }

    /**
     * Constructor for internal binaries.
     *
     * @param rescId the internal identifier.
     * @param content the stream of the content.
     */
    protected CreateNonRdfSourceOperation(final FedoraId rescId, final InputStream content) {
        super(rescId, content);
    }

    @Override
    public String getInteractionModel() {
        return NON_RDF_SOURCE.toString();
    }

    @Override
    public boolean isArchivalGroup() {
        return false;
    }

    @Override
    public FedoraId getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(final FedoraId parentId) {
        this.parentId = parentId;
    }

}
