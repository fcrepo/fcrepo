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
package org.fcrepo.kernel.api.operations;

import java.net.URI;
import java.util.Collection;

/**
 * Builder for an operation for interacting with a non-rdf source
 *
 * @author bbpennel
 */
public interface NonRdfSourceOperationBuilder extends ResourceOperationBuilder {

    /**
     * Set the mimetype for content in this resource
     *
     * @param mimetype the mime-type.
     * @return the builder.
     */
    NonRdfSourceOperationBuilder mimeType(String mimetype);

    /**
     * Set the filename
     *
     * @param filename name of the file.
     * @return the builder.
     */
    NonRdfSourceOperationBuilder filename(String filename);

    /**
     * Collection of digests for content in this resource
     *
     * @param digests collection of digests
     * @return the builder.
     */
    NonRdfSourceOperationBuilder contentDigests(Collection<URI> digests);

    /**
     * Set the number of bytes for the content
     *
     * @param size size of the content in bytes
     * @return the builder
     */
    NonRdfSourceOperationBuilder contentSize(long size);

    @Override
    NonRdfSourceOperationBuilder userPrincipal(String userPrincipal);

    @Override
    NonRdfSourceOperation build();
}
