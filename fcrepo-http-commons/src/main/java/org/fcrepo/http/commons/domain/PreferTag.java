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

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_SERVER_MANAGED;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;

import org.glassfish.jersey.message.internal.HttpHeaderReader;

import javax.servlet.http.HttpServletResponse;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parse a single prefer tag, value and any optional parameters
 *
 * @author cabeer
 */
public class PreferTag implements Comparable<PreferTag> {
    private final String tag;
    private String value = "";
    private Map<String, String> params = new HashMap<>();

    /**
     * Create an empty PreferTag
     * @return the empty PreferTag
     */
    public static PreferTag emptyTag() {
        return new PreferTag((String)null);
    }

    /**
     * Create a new PreferTag from an existing tag
     * @param preferTag the preferTag
     */
    protected PreferTag(final PreferTag preferTag) {
        tag = preferTag.getTag();
        value = preferTag.getValue();
        params = preferTag.getParams();
    }

    /**
     * Parse the prefer tag and parameters out of the header
     * @param reader the reader
     */
    private PreferTag(final HttpHeaderReader reader) {

        // Skip any white space
        reader.hasNext();

        if (reader.hasNext()) {
            try {
                tag = Optional.ofNullable(reader.nextToken())
                          .map(CharSequence::toString).orElse(null);

                if (reader.hasNextSeparator('=', true)) {
                    reader.next();

                    value = Optional.ofNullable(reader.nextTokenOrQuotedString())
                            .    map(CharSequence::toString)
                                .orElse(null);
                }

                if (reader.hasNext()) {
                    params = HttpHeaderReader.readParameters(reader);
                    if ( params == null ) {
                        params = new HashMap<>();
                    }
                }
            } catch (final ParseException e) {
                throw new IllegalArgumentException("Could not parse 'Prefer' header", e);
            }
        } else {
            tag = "";
        }
    }

    /**
     * Create a blank prefer tag
     * @param inputTag the input tag
     */
    public PreferTag(final String inputTag) {
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
     * @param servletResponse the servlet response
     */
    public void addResponseHeaders(final HttpServletResponse servletResponse) {

        final String receivedParam = ofNullable(params.get("received")).orElse("");
        final List<String> includes = asList(ofNullable(params.get("include")).orElse(" ").split(" "));
        final List<String> omits = asList(ofNullable(params.get("omit")).orElse(" ").split(" "));

        final StringBuilder includeBuilder = new StringBuilder();
        final StringBuilder omitBuilder = new StringBuilder();

        if (!(value.equals("minimal") || receivedParam.equals("minimal"))) {
            final List<String> appliedPrefs = asList(PREFER_SERVER_MANAGED.toString(),
                    PREFER_MINIMAL_CONTAINER.toString(),
                    PREFER_MEMBERSHIP.toString(),
                    PREFER_CONTAINMENT.toString());
            final List<String> includePrefs = asList(EMBED_CONTAINED.toString(),
                    INBOUND_REFERENCES.toString());
            includes.forEach(param -> includeBuilder.append(
                    (appliedPrefs.contains(param) || includePrefs.contains(param)) ? param + " " : ""));

            // Note: include params prioritized over omits during implementation
            omits.forEach(param -> omitBuilder.append(
                    (appliedPrefs.contains(param) && !includes.contains(param)) ? param + " " : ""));
        }

        // build the header for Preference Applied
        final String appliedReturn = value.equals("minimal") ? "return=minimal" : "return=representation";
        final String appliedReceived = receivedParam.equals("minimal") ? "received=minimal" : "";

        final StringBuilder preferenceAppliedBuilder = new StringBuilder(appliedReturn);
        preferenceAppliedBuilder.append(appliedReceived.length() > 0 ? "; " + appliedReceived : "");
        appendHeaderParam(preferenceAppliedBuilder, "include", includeBuilder.toString().trim());
        appendHeaderParam(preferenceAppliedBuilder, "omit", omitBuilder.toString().trim());

        servletResponse.addHeader("Preference-Applied", preferenceAppliedBuilder.toString().trim());

        servletResponse.addHeader("Vary", "Prefer");
    }

    private void appendHeaderParam(final StringBuilder builder, final String paramName, final String paramValue) {
        if (paramValue.length() > 0) {
            builder.append("; " + paramName + "=\"" + paramValue.trim() + "\"");
        }
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

    @Override
    public boolean equals(final Object obj) {
        if ((obj instanceof PreferTag)) {
            return getTag().equals(((PreferTag) obj).getTag());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (getTag() == null) {
            return 0;
        }
        return getTag().hashCode();
    }
}
