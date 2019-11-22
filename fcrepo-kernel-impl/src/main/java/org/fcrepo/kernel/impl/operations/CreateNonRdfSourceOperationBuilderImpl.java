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

import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;


/**
 * Builder for operations to create new non-rdf sources
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperationBuilderImpl implements CreateNonRdfSourceOperationBuilder {

    /**
     * The resource id.
     */
    private final String resourceId;

    private String parentId;

    private String userPrincipal;

    private String mimeType;
    private String filename;
    private Collection<URI> digests;

    private Long contentSize;
    private InputStream content;
    private URI externalURI;
    private String externalType;

    /**
     * Constructor for external binary.
     *
     * @param rescId      the internal identifier
     * @param handling    the external content handling type.
     * @param externalUri the external content URI.
     */
    protected CreateNonRdfSourceOperationBuilderImpl(final String rescId, final String handling,
            final URI externalUri) {
        this(rescId);
        this.externalURI = externalUri;
        this.externalType = handling;
    }

    /**
     * Constructor for internal binary.
     *
     * @param rescId the internal identifier.
     * @param stream the content stream.
     */
    protected CreateNonRdfSourceOperationBuilderImpl(final String rescId, final InputStream stream) {
        this(rescId);
        this.content = stream;
    }

    /**
     * Constructor
     *
     * @param rescId the internal identifier.
     */
    private CreateNonRdfSourceOperationBuilderImpl(final String rescId) {
        this.resourceId = rescId;
    }

    @Override
    public CreateNonRdfSourceOperationBuilderImpl mimeType(final String mimetype) {
        this.mimeType = mimetype;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperationBuilderImpl filename(final String filename) {
        this.filename = filename;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperationBuilderImpl contentDigests(final Collection<URI> digests) {
        this.digests = digests;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperationBuilderImpl contentSize(final long size) {
        this.contentSize = size;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperationBuilderImpl userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperationBuilder parentId(final String parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperation build() {
        final CreateNonRdfSourceOperation operation;
        if (externalURI != null && externalType != null) {
            operation = new CreateNonRdfSourceOperation(resourceId, externalURI, externalType);
        } else {
            operation = new CreateNonRdfSourceOperation(resourceId, content);
        }

        operation.setUserPrincipal(userPrincipal);
        operation.setDigests(digests);
        operation.setFilename(filename);
        operation.setContentSize(contentSize);
        operation.setMimeType(mimeType);
        operation.setParentId(parentId);

        return operation;
    }
}
