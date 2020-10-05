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
package org.fcrepo.persistence.api.exceptions;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * Generic exception for things PersistentStorage related.
 *
 * @author whikloj
 * @since 2019-09-20
 */
public class PersistentStorageException extends RepositoryRuntimeException {

    /**
     * version UID.
     */
    private static final long serialVersionUID = -1L;

    /**
     * Constructor.
     *
     * @param msg the message
     */
    public PersistentStorageException(final String msg) {
        super(msg);
    }

    /**
     * Constructor
     *
     * @param msg message
     * @param e cause
     */
    public PersistentStorageException(final String msg, final Throwable e) {
        super(msg, e);
    }

}
