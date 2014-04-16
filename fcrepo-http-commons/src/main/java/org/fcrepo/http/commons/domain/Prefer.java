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
package org.fcrepo.http.commons.domain;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.sun.jersey.core.header.reader.HttpHeaderReader;

import java.text.ParseException;
import java.util.List;

public class Prefer {

    private final List<PreferTag> preferTags;

    /**
     * Parse a Prefer: header
     *
     * @param inputValue
     * @throws ParseException
     */
    public Prefer(final String inputValue) throws ParseException {
        preferTags = HttpHeaderReader.readList(PREFER_CREATOR, inputValue);
    }

    /**
     * Does the Prefer: header have a return tag
     * @return
     */
    public Boolean hasReturn() {
        return Iterators.any(preferTags.iterator(), getPreferTag("return"));
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     * @return
     */
    public PreferTag getReturn() {
        final Optional<PreferTag> aReturn = Iterators.tryFind(preferTags.iterator(), getPreferTag("return"));

        if (aReturn.isPresent()) {
            return aReturn.get();
        } else {
            return new PreferTag("");
        }
    }


    private static final HttpHeaderReader.ListElementCreator<PreferTag> PREFER_CREATOR =
        new HttpHeaderReader.ListElementCreator<PreferTag>() {
            public PreferTag create(HttpHeaderReader reader) throws ParseException {
                return new PreferTag(reader);
            }
        };

    private Predicate<PreferTag> getPreferTag(final String tagName) {
        return new Predicate<PreferTag>() {
            @Override
            public boolean apply(PreferTag tag) {
                return tag.getTag().equals(tagName);
            }
        };
    }
}
