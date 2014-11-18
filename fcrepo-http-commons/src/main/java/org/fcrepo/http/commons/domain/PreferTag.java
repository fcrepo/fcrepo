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
package org.fcrepo.http.commons.domain;

import static java.util.Objects.hash;

import org.glassfish.jersey.message.internal.HttpHeaderReader;

import javax.servlet.http.HttpServletResponse;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse a single prefer tag, value and any optional parameters
 *
 * @author cabeer
 * @author ajs6f
 */
public class PreferTag implements Comparable<PreferTag> {
    private final String tag;
    private String value = "";
    private Map<String, String> params = new HashMap<>();

    /**
     * Create an empty PreferTag
     * @return
     */
    public static PreferTag emptyTag() {
        try {
            return new PreferTag((String)null);
        } catch (final ParseException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Create a new PreferTag from an existing tag
     * @param preferTag
     */
    public PreferTag(final PreferTag preferTag) {
        tag = preferTag.getTag();
        value = preferTag.getValue();
        params = preferTag.getParams();
    }

    /**
     * Parse the prefer tag and parameters out of the header
     * @param reader
     * @throws ParseException
     */
    public PreferTag(final HttpHeaderReader reader) throws ParseException {

        // Skip any white space
        reader.hasNext();

        if (reader.hasNext()) {
            tag = reader.nextToken();

            if (reader.hasNextSeparator('=', true)) {
                reader.next();

                value = reader.nextTokenOrQuotedString();
            }

            if (reader.hasNext()) {
                params = HttpHeaderReader.readParameters(reader);
            }
        } else {
            tag = "";
        }
    }

    /**
     * Create a blank prefer tag
     * @param inputTag
     */
    public PreferTag(final String inputTag) throws ParseException {
        this(HttpHeaderReader.newInstance(inputTag));
    }

    /**
     * Get the tag name
     * @return tag name
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get the default value for the tag
     * @return default value for the tag
     */
    public String getValue() {
        return value;
    }

    /**
     * Get any additional parameters for the prefer tag
     * @return additional parameters for the prefer tag
     */
    public Map<String,String> getParams() {
        return params;
    }

    /**
     * Add appropriate response headers to indicate that the incoming preferences were acknowledged
     * @param servletResponse
     */
    public void addResponseHeaders(final HttpServletResponse servletResponse) {
        if (!value.equals("minimal")) {
            servletResponse.addHeader("Preference-Applied", "return=representation");
        } else {
            servletResponse.addHeader("Preference-Applied", "return=minimal");
        }
        servletResponse.addHeader("Vary", "Prefer");
    }

    /**
     * We consider tags with the same name to be equal, because <a
     * href="http://tools.ietf.org/html/rfc7240#page-4">the definition of Prefer headers</a> does not permit that tags
     * with the same name be consumed except by selecting for the first appearing tag.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final PreferTag otherTag) {
        return getTag().compareTo(otherTag.getTag());
    }

    /**
     * We consider tags with the same name to be equal, because <a
     * href="http://tools.ietf.org/html/rfc7240#page-4">the definition of Prefer headers</a> does not permit that tags
     * with the same name be consumed except by selecting for the first appearing tag.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof PreferTag) {
            return getTag().equals(((PreferTag) other).getTag());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash(tag, value, params);
    }

}
