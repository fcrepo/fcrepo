/*
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
package org.fcrepo.http.commons.responses;

import java.util.Map;

import org.fcrepo.kernel.api.RdfStream;

/**
 * A simple type to collect an RdfStream and associated Namespace mappings
 *
 * @author acoburn
 * @since 2/13/16
 */
public class RdfNamespacedStream {

    public final RdfStream stream;

    public final Map<String, String> namespaces;

    /**
     * Creates an object to hold an RdfStream and an associated namespace mapping.
     *
     * @param stream the RdfStream
     * @param namespaces the namespace mapping
     */
    public RdfNamespacedStream(final RdfStream stream, final Map<String, String> namespaces) {
        this.stream = stream;
        this.namespaces = namespaces;
    }
}
