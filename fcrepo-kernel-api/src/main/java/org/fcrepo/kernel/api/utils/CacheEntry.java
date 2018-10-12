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
package org.fcrepo.kernel.api.utils;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;

/**
 * A CacheEntry abstraction for the various possible types of entries
 * @author fasseg
 *
 */
public interface CacheEntry {

    /**
     * Check the fixity of a {@link CacheEntry}
     * @param algorithm the given algorithm
     * @return a {@link FixityResult} containing the relevant data
     */
    Collection<FixityResult> checkFixity(final String algorithm);

    /**
     * Check the fixity of a {@link CacheEntry} with list of digest algorithms
     * @param algorithms the given algorithms
     * @return a {@link FixityResult} containing the relevant data
     * @throws UnsupportedAlgorithmException in case the fixity check tries to use an unsupported algorithm
     */
    Collection<URI> checkFixity(final Collection<String> algorithms) throws UnsupportedAlgorithmException;

    /**
     * Get a raw input stream from the underlying store
     * @return the content for this entry
     */
    InputStream getInputStream();

    /**
     * Generate a human-readable identifier for the location of this entry
     *
     * @return human-readable identifier for the location of this entry
     */
    String getExternalIdentifier();
}
