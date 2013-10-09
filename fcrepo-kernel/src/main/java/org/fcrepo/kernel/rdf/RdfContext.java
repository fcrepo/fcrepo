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

package org.fcrepo.kernel.rdf;

import org.fcrepo.kernel.utils.iterators.RdfStream;

/**
 * A container for the context of an {@link RdfStream}. This might include
 * namespaces, and any triples that shouldn't be considered instance data.
 *
 * @author ajs6f
 * @date Oct 9, 2013
 */
public abstract class RdfContext {

    protected RdfStream context;

    /**
     * Default constructor, creates empty context.
     */
    public RdfContext() {
        context = new RdfStream();
    }

    /**
     * @return The context in question as an {@link RdfStream}
     */
    public RdfStream context() {
        return context;
    }

}
