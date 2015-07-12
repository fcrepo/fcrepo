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

package org.fcrepo.http.commons.test.util;

import com.hp.hpl.jena.sparql.modify.GraphStoreWrapper;
import com.hp.hpl.jena.update.GraphStore;

/**
 * Adds the standard {@link AutoCloseable} semantic to Jena's {@link GraphStore} for convenient use with Java 7's
 * <code>try-with-resources</code> syntax.
 *
 * @author ajs6f
 */
public class CloseableGraphStore extends GraphStoreWrapper implements AutoCloseable {

    /**
     * Default constructor.
     *
     * @param graphStore GraphStore to wrap
     */
    public CloseableGraphStore(final GraphStore graphStore) {
        super(graphStore);
    }
}