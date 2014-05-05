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
package org.fcrepo.storage.policy;

/**
 * Note: Mainly for reporting conversion errors & future use
 * (where other exceptions are not adequate).
 *
 * @author awoods
 */
public class StoragePolicyTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param arg0
     */
    public StoragePolicyTypeException(final String arg0) {
        super(arg0);
    }

}
