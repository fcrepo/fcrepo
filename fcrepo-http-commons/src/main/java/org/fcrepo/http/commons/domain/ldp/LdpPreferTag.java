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
package org.fcrepo.http.commons.domain.ldp;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_CONTAINMENT;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MEMBERSHIP;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_SERVER_MANAGED;
import static org.fcrepo.kernel.api.rdf.LdpTriplePreferences.PreferChoice.EXCLUDE;
import static org.fcrepo.kernel.api.rdf.LdpTriplePreferences.PreferChoice.INCLUDE;

import java.util.List;
import java.util.Optional;

import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;

import org.apache.jena.rdf.model.Property;

/**
 * A subclass of {@link PreferTag} that contemplates the possible preferences for Linked Data Platform requests.
 *
 * @author ajs6f
 */
public class LdpPreferTag extends PreferTag implements LdpTriplePreferences {

    private final PreferChoice minimal;

    private final PreferChoice membership;

    private final PreferChoice containment;

    private final PreferChoice references;

    private final PreferChoice embed;

    private final PreferChoice managedProperties;

    final List<String> includes;

    final List<String> omits;

    /**
     * Standard constructor.
     *
     * @param preferTag the prefer tag
     */
    public LdpPreferTag(final PreferTag preferTag) {
        super(preferTag);

        final Optional<String> include = ofNullable(preferTag.getParams().get("include"));
        final Optional<String> omit = ofNullable(preferTag.getParams().get("omit"));

        includes = asList(include.orElse(" ").split(" "));
        omits = asList(omit.orElse(" ").split(" "));

        minimal = getChoice(PREFER_MINIMAL_CONTAINER);

        membership = getChoice(PREFER_MEMBERSHIP);

        containment = getChoice(PREFER_CONTAINMENT);

        references = getChoice(INBOUND_REFERENCES);

        embed = getChoice(EMBED_CONTAINED);

        managedProperties = getChoice(PREFER_SERVER_MANAGED);
    }

    /**
     * Determine what this tag's place in the Prefer header is.
     * @param tag the tag to look for
     * @return Whether the tag was included, omitted or not mentioned.
     */
    private PreferChoice getChoice(final Property tag) {
        if (includes.contains(tag.toString())) {
            return PreferChoice.INCLUDE;
        } else if (omits.contains(tag.toString())) {
            return PreferChoice.EXCLUDE;
        }
        return PreferChoice.SILENT;
    }

    @Override
    public boolean displayUserRdf() {
        // Displayed by default unless we asked to exclude minimal container.
        return !minimal.equals(EXCLUDE);
    }

    @Override
    public boolean displayMembership() {
        // Displayed by default unless we specifically asked for it or didn't specifically ask for a minimal container
        // AND ( we didn't exclude either managed properties or membership ).
        return membership.equals(INCLUDE) || notIncludeMinimal() && (
                        notExcludeManaged() && !membership.equals(EXCLUDE)
        );
    }

    @Override
    public boolean displayContainment() {
        // Displayed by default unless we specifically asked for it or didn't specifically ask for a minimal container
        // AND ( we didn't exclude either managed properties or containment ).
        return containment.equals(INCLUDE) || notIncludeMinimal() && (
                        notExcludeManaged() && !containment.equals(EXCLUDE)
        );
    }

    @Override
    public boolean displayReferences() {
        // If we did ask for references. (Not shown by default).
        return references.equals(INCLUDE);
    }

    @Override
    public boolean displayEmbed() {
        // If we did ask for embedded resources. (Not shown by default).
        return embed.equals(INCLUDE);
    }

    @Override
    public boolean displayServerManaged() {
        // Displayed by default, unless excluded minimal container or managed properties.
        return !minimal.equals(EXCLUDE) && notExcludeManaged();
    }

    /**
     * @return whether we did not explicitly ask for a minimal container.
     */
    private boolean notIncludeMinimal() {
        return !minimal.equals(INCLUDE);
    }

    /**
     * @return whether we did not explicitly exclude managed properties.
     */
    private boolean notExcludeManaged() {
        return !managedProperties.equals(EXCLUDE);
    }
}
