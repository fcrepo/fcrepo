/**
 * Copyright 2014 DuraSpace, Inc.
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

import javax.jcr.RepositoryException;

/**
 * An extension of RepositoryException that may be thrown when attempting a
 * operation (or instantiation) of one fedora resource type (Object, Datastream)
 * on a different (and incompatible) type.
 *
 * @author Mike Durbin
 */
public class ResourceTypeException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ResourceTypeException(final String message) {
        super(message);
    }

}
