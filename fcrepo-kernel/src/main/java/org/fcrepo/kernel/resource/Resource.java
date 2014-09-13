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

package org.fcrepo.kernel.resource;

import java.util.Date;

/**
 * @author cabeer
 * @since 9/13/14
 */
public interface Resource<T> {

    /**
     * @return The node that backs this object.
     */
    T getNode();

    /**
     * Get the path to the JCR node
     * @return path
     */
    String getPath();

    /**
     * Check if this object uses a given mixin
     * @return a collection of mixin names
     * @throws javax.jcr.RepositoryException
     */
    boolean hasType(final String type);

    /**
     * Check if a resource was created in this session
     * @return if resource created in this session
     */
    Boolean isNew();

    /**
     * Get the date this datastream was created
     * @return created date
     */
    Date getCreatedDate();

    /**
     * Get the date this datastream was last modified
     * @return last modified date
     */
    Date getLastModifiedDate();

    /**
     * Construct an ETag value from the last modified date and path. JCR has a
     * mix:etag type, but it only takes into account binary properties. We
     * actually want whole-object etag data. TODO : construct and store an ETag
     * value on object modify
     *
     * @return constructed etag value
     */
    String getEtagValue();

}
