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

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

/**
 * An operation for interacting with a non-RDF source resource.
 *
 * @author bbpennel
 */
public interface NonRdfSourceOperation extends ResourceOperation {

    /**
     * @return the content stream for a local binary
     */
    InputStream getContentStream();

    /**
     * @return the handling method for external content in this resource
     */
    String getExternalHandling();

    /**
     * @return the URI for external content in this resource
     */
    URI getContentUri();

    /**
     * @return The MimeType of content associated with this resource.
     */
    String getMimeType();

    /**
     * Return the file name for the binary content
     *
     * @return original file name for the binary content, or the object's id.
     */
    String getFilename();

    /**
     * @return the URIs of digests for the content in this resource
     */
    Collection<URI> getContentDigests();

    /**
     * @return The size in bytes of content associated with this resource.
     */
    long getContentSize();
}
