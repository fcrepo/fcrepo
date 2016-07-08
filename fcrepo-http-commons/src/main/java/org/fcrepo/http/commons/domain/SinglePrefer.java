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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.fcrepo.http.commons.domain.PreferTag.emptyTag;

import java.util.Set;
import java.util.TreeSet;

/**
 * JAX-RS HTTP parameter parser for the Prefer header
 *
 * @author cabeer
 * @author ajs6f
 * @author acoburn
 */
public class SinglePrefer {

    private final Set<PreferTag> preferTags = new TreeSet<>();

    /**
     * Parse a Prefer: header
     *
     * @param header the header
     */
    public SinglePrefer(final String header) {
        preferTags.addAll(stream(header.split(","))
                .map(PreferTag::new)
                .collect(toSet()));
    }

    /**
     * Does the Prefer: header have a return tag
     *
     * @return true if the header has a return tag
     */
    public Boolean hasReturn() {
        return preferTags().stream().map(PreferTag::getTag).anyMatch("return"::equals);
    }

    /**
     * Does the Prefer: header have a return tag
     *
     * @return true if the header has a return tag
     */
    public Boolean hasHandling() {
        return preferTags().stream().map(PreferTag::getTag).anyMatch("handling"::equals);
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     *
     * @return return tag, or a blank default, if none exists
     */
    public PreferTag getReturn() {
        return preferTags().stream()
                .filter(x -> x.getTag().equals("return"))
                .findFirst().orElse(emptyTag());
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     *
     * @return return tag, or a blank default, if none exists
     */
    public PreferTag getHandling() {
        return preferTags().stream()
                .filter(x -> x.getTag().equals("handling"))
                .findFirst().orElse(emptyTag());
    }

    protected Set<PreferTag> preferTags() {
        return preferTags;
    }
}
