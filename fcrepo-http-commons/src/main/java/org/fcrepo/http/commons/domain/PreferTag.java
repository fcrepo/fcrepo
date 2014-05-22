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

import com.sun.jersey.core.header.reader.HttpHeaderReader;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse a single prefer tag, value and any optional parameters
 *
 * @author cabeer
 */
public class PreferTag {
    private final String tag;
    private String value = "";
    private Map<String, String> params = new HashMap<>();

    /**
     * Parse the prefer tag and parameters out of the header
     * @param reader
     * @throws ParseException
     */
    public PreferTag(final HttpHeaderReader reader) throws ParseException {

        // Skip any white space
        reader.hasNext();

        tag = reader.nextToken();

        if (reader.hasNextSeparator('=', true)) {
            reader.next();

            value = reader.nextTokenOrQuotedString();
        }

        if (reader.hasNext()) {
            params = HttpHeaderReader.readParameters(reader);
        }
    }

    /**
     * Create a blank prefer tag
     * @param inputTag
     */
    public PreferTag(final String inputTag) {
        tag = inputTag;
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
}
