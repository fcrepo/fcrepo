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
package org.fcrepo.kernel.api.exception;

/**
 * An extension of {@link RepositoryRuntimeException} that may be thrown when attempting a
 * operation (or instantiation) of a {@link org.fcrepo.kernel.api.models.FedoraResource}
 * on a different (and incompatible) type.
 *
 * @author Mike Durbin
 */
public class ResourceTypeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     * @param message the message
     */
    public ResourceTypeException(final String message) {
        super(message);
    }

}
