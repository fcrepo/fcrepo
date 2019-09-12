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
package org.fcrepo.persistence.api;

import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * An interface that mediates CRUD operations to and from persistence storage.
 *
 * @author dbernstein
 * @author whikloj
 * @since 2019-09-11
 */
public interface PersistentStorage {

    /**
     * Locate an existing resource on disk.
     *
     * @param identifier The id of the resource.
     * @return the FedoraResource
     */
    public FedoraResource findResource(final String identifier);

    /**
     * Save/Update a RdfSource to storage.
     *
     * @param identifier The identifier of the resource.
     * @param content The RdfStream of content.
     * @param informationHeader The information required for headers or other Fedora actions.
     */
    public void saveRdfSource(final String identifier, final RdfStream content,
            final Map<String, String> informationHeader);

    /**
     * Save/Update a NonRdfSource to storage.
     *
     * @param identifier The identifier of the resource.
     * @param content The binary stream.
     * @param informationHeaders The information required for headers or other Fedora actions.
     * @param contentType The mime-type.
     */
    public void saveNonRdfSource(final String identifier, final InputStream content,
            final Map<String, String> informationHeaders, final MediaType contentType);

    /**
     * Delete an object from storage.
     *
     * @param identifier The identifier of the resource.
     */
    public void deleteResource(final String identifier);

}
