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
package org.fcrepo.http.commons.domain;

import java.util.Set;

import javax.ws.rs.HeaderParam;

/**
 * Aggregates information from multiple Prefer HTTP headers.
 *
 * @author ajs6f
 * @since 23 October 2014
 */
public class MultiPrefer extends SinglePrefer {

    /**
     * @param header the header
     */
    public MultiPrefer(final String header) {
        super(header);
    }

    /**
     * @param prefers the prefers
     */
    public MultiPrefer(final @HeaderParam("Prefer") Set<SinglePrefer> prefers) {
        super("");
        prefers.forEach(p -> preferTags().addAll(p.preferTags()));
    }
}
