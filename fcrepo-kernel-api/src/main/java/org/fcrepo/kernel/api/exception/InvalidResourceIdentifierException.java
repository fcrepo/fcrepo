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
 * This exception is used for invalid resource identifiers, such as when a resource path has empty segments.
 * Note: This exception is *not* used for valid identifiers that point to non-existent resources.
 * Additionally, the notion of "valid" will depend on the {@link org.fcrepo.kernel.api.identifiers.IdentifierConverter}
 * chain in use.
 *
 * @author awoods
 * @since July 14, 2015
 */
public class InvalidResourceIdentifierException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param msg root cause
     */
    public InvalidResourceIdentifierException(final String msg) {
        super(msg);
    }
}
