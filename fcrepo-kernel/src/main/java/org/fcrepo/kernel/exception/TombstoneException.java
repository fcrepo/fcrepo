/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.exception;

import org.fcrepo.kernel.models.FedoraResource;

/**
 * Exception when a Tombstone {@link org.fcrepo.kernel.models.FedoraResource}
 * is used where a real object is expected
 *
 * @author cabeer
 * @since 10/16/14
 */
public class TombstoneException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    private final FedoraResource fedoraResource;
    private final String uri;

    /**
     * Construct a new tombstone exception for a resource
     * @param fedoraResource
     */
    public TombstoneException(final FedoraResource fedoraResource) {
        this(fedoraResource, null);
    }

    /**
     * Create a new tombstone exception with a URI to the tombstone resource
     * @param fedoraResource
     * @param uri
     */
    public TombstoneException(final FedoraResource fedoraResource, final String uri) {
        super("Discovered tombstone resource at " + fedoraResource);
        this.fedoraResource = fedoraResource;
        this.uri = uri;
    }

    /**
     * Get the tombstone resource
     * @return
     */
    public FedoraResource getResource() {
        return fedoraResource;
    }

    /**
     * Get a URI to the tombstone resource
     * @return
     */
    public String getURI() {
        return uri;
    }
}
