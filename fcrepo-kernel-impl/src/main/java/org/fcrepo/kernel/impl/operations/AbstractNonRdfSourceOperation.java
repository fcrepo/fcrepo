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

    private long contentSize = -1;

    /**
     * Constructor for external content.
     *
     * @param rescId the internal identifier.
     * @param externalContentURI the URI of the external content.
     * @param externalHandling the type of external content handling (REDIRECT, PROXY)
     */
    protected AbstractNonRdfSourceOperation(final FedoraId rescId, final URI externalContentURI,
            final String externalHandling) {
        super(rescId);
        this.externalHandlingURI = externalContentURI;
        this.externalHandlingType = externalHandling;
    }

    /**
     * Constructor for internal binaries.
     *
     * @param rescId the internal identifier.
     * @param content the stream of the content.
     */
    protected AbstractNonRdfSourceOperation(final FedoraId rescId, final InputStream content) {
        super(rescId);
        this.content = content;
    }

    /**
     * Basic constructor.
     *
     * @param rescId The internal Fedora ID.
     */
    protected AbstractNonRdfSourceOperation(final FedoraId rescId) {
        super(rescId);
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

    /**
     * @return the content
     */
    protected InputStream getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    protected void setContent(final InputStream content) {
        this.content = content;
    }

    /**
     * @return the externalHandlingURI
     */
    protected URI getExternalHandlingURI() {
        return externalHandlingURI;
    }

    /**
     * @param externalHandlingURI the externalHandlingURI to set
     */
    protected void setExternalHandlingURI(final URI externalHandlingURI) {
        this.externalHandlingURI = externalHandlingURI;
    }

    /**
     * @return the externalHandlingType
     */
    protected String getExternalHandlingType() {
        return externalHandlingType;
    }

    /**
     * @param externalHandlingType the externalHandlingType to set
     */
    protected void setExternalHandlingType(final String externalHandlingType) {
        this.externalHandlingType = externalHandlingType;
    }

    /**
     * @return the digests
     */
    protected Collection<URI> getDigests() {
        return digests;
    }

    /**
     * @param digests the digests to set
     */
    protected void setDigests(final Collection<URI> digests) {
        this.digests = digests;
    }

    /**
     * @param mimeType the mimeType to set
     */
    protected void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @param filename the filename to set
     */
    protected void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * @param contentSize the contentSize to set
     */
    protected void setContentSize(final long contentSize) {
        this.contentSize = contentSize;
    }
}
