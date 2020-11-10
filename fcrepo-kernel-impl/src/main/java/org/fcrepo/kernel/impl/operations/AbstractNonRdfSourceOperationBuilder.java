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
import java.util.Collection;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;

/**
 * An abstract operation for interacting with a non-rdf source
 *
 * @author bbpennel
 */
public abstract class AbstractNonRdfSourceOperationBuilder implements NonRdfSourceOperationBuilder {

    protected FedoraId resourceId;

    protected InputStream content;

    protected URI externalURI;

    protected String externalType;

    protected String mimeType;

    protected String filename;

    protected Collection<URI> digests;

    protected long contentSize = -1;

    protected String userPrincipal;

    /**
     * Constructor for external binary.
     *
     * @param rescId the internal identifier
     * @param handling the external content handling type.
     * @param externalUri the external content URI.
     */
    protected AbstractNonRdfSourceOperationBuilder(final FedoraId rescId, final String handling,
            final URI externalUri) {
        this.resourceId = rescId;
        this.externalURI = externalUri;
        this.externalType = handling;
    }

    /**
     * Constructor for internal binary.
     *
     * @param rescId the internal identifier.
     * @param stream the content stream.
     */
    protected AbstractNonRdfSourceOperationBuilder(final FedoraId rescId, final InputStream stream) {
        this.resourceId = rescId;
        this.content = stream;
    }

    @Override
    public NonRdfSourceOperationBuilder mimeType(final String mimetype) {
        this.mimeType = mimetype;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder filename(final String filename) {
        this.filename = filename;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder contentDigests(final Collection<URI> digests) {
        this.digests = digests;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder contentSize(final long size) {
        this.contentSize = size;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }
}
