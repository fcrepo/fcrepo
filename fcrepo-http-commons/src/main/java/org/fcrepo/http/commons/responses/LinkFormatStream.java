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
package org.fcrepo.http.commons.responses;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;

import javax.ws.rs.core.Link;

/**
 * Stream of links for Memento TimeMaps
 *
 * @author whikloj
 * @since 2017-10-24
 */
public class LinkFormatStream implements AutoCloseable {

    private final Stream<Link> stream;

    /**
     * Constructor
     *
     * @param stream the stream of Links
     */
    public LinkFormatStream(final Stream<Link> stream) {
        requireNonNull(stream);
        this.stream = stream;
    }

    /**
     * Generic getter
     * 
     * @return the Stream of Links
     */
    public Stream<Link> getStream() {
        return stream;
    }

    @Override
    public void close() {
        stream.close();
    }

}
