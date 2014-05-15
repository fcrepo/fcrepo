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
package org.fcrepo.kernel.utils;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import javax.jcr.RepositoryException;

/**
 * A CacheEntry abstraction for the various possible types of entries
 * @author fasseg
 *
 */
public interface CacheEntry {

    /**
     * Check the fixity of a {@link CacheEntry}
     * @param checksum the checksum previously generated for the entry
     * @param size the size of the entry
     * @return a {@link FixityResult} containing the relevant data
     * @throws RepositoryException
     */
    Collection<FixityResult> checkFixity(URI checksum, long size)
        throws RepositoryException;

    /**
     * Get a raw input stream from the underlying store
     * @return the content for this entry
     * @throws RepositoryException
     */
    abstract InputStream getInputStream() throws RepositoryException;

    /**
     * Generate a human-readable identifier for the location of this entry
     *
     * @return human-readable identifier for the location of this entry
     * @throws RepositoryException
     */
    abstract String getExternalIdentifier() throws RepositoryException;
}
