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
package org.fcrepo.http.commons.domain;

import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.tryFind;
import static org.fcrepo.http.commons.domain.PreferTag.emptyTag;

import java.text.ParseException;
import java.util.Set;
import java.util.TreeSet;

import org.glassfish.jersey.message.internal.HttpHeaderReader;

import com.google.common.base.Predicate;

/**
 * JAX-RS HTTP parameter parser for the Prefer header
 *
 * @author cabeer
 * @author ajs6f
 */
public class SinglePrefer {

    private final Set<PreferTag> preferTags = new TreeSet<>();

    /**
     * Parse a Prefer: header
     *
     * @param inputValue
     * @throws ParseException
     */
    public SinglePrefer(final String header) throws ParseException {
        preferTags.addAll(HttpHeaderReader.readList(PREFER_CREATOR, header));
    }

    /**
     * Does the Prefer: header have a return tag
     *
     * @return true if the header has a return tag
     */
    public Boolean hasReturn() {
        return any(preferTags(), getPreferTag("return"));
    }

    /**
     * Does the Prefer: header have a return tag
     *
     * @return true if the header has a return tag
     */
    public Boolean hasHandling() {
        return any(preferTags(), getPreferTag("handling"));
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     *
     * @return return tag, or a blank default, if none exists
     */
    public PreferTag getReturn() {
        return tryFind(preferTags(), getPreferTag("return")).or(emptyTag());
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     *
     * @return return tag, or a blank default, if none exists
     */
    public PreferTag getHandling() {
        return tryFind(preferTags(), getPreferTag("handling")).or(emptyTag());
    }

    private static final HttpHeaderReader.ListElementCreator<PreferTag> PREFER_CREATOR =
            new HttpHeaderReader.ListElementCreator<PreferTag>() {

                @Override
                public PreferTag create(final HttpHeaderReader reader) throws ParseException {
                    return new PreferTag(reader);
                }
            };

    private static <T extends PreferTag> Predicate<T> getPreferTag(final String tagName) {
        return new Predicate<T>() {

            @Override
            public boolean apply(final T tag) {
                return tag.getTag().equals(tagName);
            }
        };
    }

    protected Set<PreferTag> preferTags() {
        return preferTags;
    }
}
