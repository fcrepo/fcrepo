/**
 * Copyright 2013 DuraSpace, Inc.
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

import org.modeshape.connector.filesystem.FileSystemConnector;

import javax.jcr.Binary;

/**
 * Cache entry for a projected binary
 *
 * @author fasseg
 */
public class ProjectedCacheEntry extends BinaryCacheEntry {

    /**
     * Create a new ProjectedCacheEntry
     * @param binary
     */
    public ProjectedCacheEntry(final Binary binary, final String externalUri) {
        super(binary, "/" + FileSystemConnector.class.getName() + ":projections:" + externalUri);
    }

}
