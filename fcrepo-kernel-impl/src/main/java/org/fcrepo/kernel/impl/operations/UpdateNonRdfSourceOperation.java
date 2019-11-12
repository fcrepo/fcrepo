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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.kernel.api.operations.ResourceOperationType;

/**
 * Operation for updating a non-rdf source
 *
 * @author bbpennel
 */
public class UpdateNonRdfSourceOperation extends AbstractNonRdfSourceOperation {

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
    protected UpdateNonRdfSourceOperation(final String rescId, final URI externalContentURI,
                                            final String externalHandling, final String mimeType, final String filename,
                                            final Collection<URI> digests) {
        super(rescId, externalContentURI, externalHandling, mimeType, filename, digests);
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
    protected UpdateNonRdfSourceOperation(final String rescId, final InputStream content, final String mimeType,
                                            final long contentSize, final String filename, final Collection<URI> digests) {
        super(rescId, content, mimeType, contentSize, filename, digests);
    }

    @Override
    public ResourceOperationType getType() {
        return UPDATE;
    }
}
