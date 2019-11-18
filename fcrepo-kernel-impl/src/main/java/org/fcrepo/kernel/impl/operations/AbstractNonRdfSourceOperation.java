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
import java.util.Collection;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;

/**
 * An abstract operation for interacting with a non-rdf source
 *
 * @author bbpennel
 */
public abstract class AbstractNonRdfSourceOperation extends AbstractResourceOperation implements
        NonRdfSourceOperation {

    private InputStream content;

    private URI externalHandlingURI;

    private String externalHandlingType;

    private String mimeType;

    private String filename;

    private Collection<URI> digests;

    private long contentSize;

    /**
     * Constructor for external content.
     *
     * @param rescId the internal identifier.
     * @param externalContentURI the URI of the external content.
     * @param externalHandling the type of external content handling (REDIRECT, PROXY)
     * @param mimeType the mime-type of the content.
     * @param filename the filename.
     * @param digests the checksum digests.
     */
    protected AbstractNonRdfSourceOperation(final String rescId, final URI externalContentURI,
                                            final String externalHandling, final String mimeType, final String filename,
                                            final Collection<URI> digests) {
        super(rescId, NON_RDF_SOURCE.toString());
        this.externalHandlingURI = externalContentURI;
        this.mimeType = mimeType;
        this.externalHandlingType = externalHandling;
        this.filename = filename;
        this.digests = digests;
    }

    /**
     * Constructor for internal binaries.
     *
     * @param rescId the internal identifier.
     * @param content the stream of the content.
     * @param mimeType the mime-type of the content.
     * @param contentSize the size of the inputstream.
     * @param filename the filename.
     * @param digests the checksum digests.
     */
    protected AbstractNonRdfSourceOperation(final String rescId, final InputStream content, final String mimeType,
                                            final long contentSize, final String filename, final Collection<URI> digests) {
        super(rescId, NON_RDF_SOURCE.toString());
        this.content = content;
        this.mimeType = mimeType;
        this.contentSize = contentSize;
        this.filename = filename;
        this.digests = digests;
    }

    /**
     * Basic constructor.
     *
     * @param rescId The internal Fedora ID.
     */
    protected AbstractNonRdfSourceOperation(final String rescId) {
        super(rescId, NON_RDF_SOURCE.toString());
    }

    @Override
    public InputStream getContentStream() {
        return content;
    }

    @Override
    public String getExternalHandling() {
        return externalHandlingType;
    }

    @Override
    public URI getContentUri() {
        return externalHandlingURI;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public Collection<URI> getContentDigests() {
        return digests;
    }

    @Override
    public long getContentSize() {
        return contentSize;
    }
}
