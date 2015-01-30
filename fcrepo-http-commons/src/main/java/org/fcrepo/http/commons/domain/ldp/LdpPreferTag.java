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
package org.fcrepo.http.commons.domain.ldp;

import static com.google.common.base.Optional.fromNullable;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.RdfLexicon.EMBED_CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.SERVER_MANAGED;

import java.util.List;

import org.fcrepo.http.commons.domain.PreferTag;

import com.google.common.base.Optional;

/**
 * A subclass of {@link PreferTag} that contemplates the possible preferences for Linked Data Platform requests.
 *
 * @author ajs6f
 */
public class LdpPreferTag extends PreferTag {

    private final boolean membership;

    private final boolean containment;

    private final boolean references;

    private final boolean preferMinimalContainer;

    private final boolean embed;

    private final boolean managedProperties;

    /**
     * Standard constructor.
     *
     * @param preferTag
     */
    public LdpPreferTag(final PreferTag preferTag) {
        super(preferTag);

        final Optional<String> include = fromNullable(preferTag.getParams().get("include"));
        final Optional<String> omit = fromNullable(preferTag.getParams().get("omit"));
        final Optional<String> received = fromNullable(preferTag.getParams().get("received"));

        final List<String> includes = asList(include.or(" ").split(" "));
        final List<String> omits = asList(omit.or(" ").split(" "));

        final boolean minimal = preferTag.getValue().equals("minimal") || received.or("").equals("minimal");

        preferMinimalContainer = includes.contains(LDP_NAMESPACE + "PreferMinimalContainer") || minimal;

        membership = (!preferMinimalContainer && !omits.contains(LDP_NAMESPACE + "PreferMembership")) ||
                includes.contains(LDP_NAMESPACE + "PreferMembership");

        containment = (!preferMinimalContainer && !omits.contains(LDP_NAMESPACE + "PreferContainment")) ||
                includes.contains(LDP_NAMESPACE + "PreferContainment");

        references = includes.contains(INBOUND_REFERENCES.toString());

        embed = includes.contains(EMBED_CONTAINS.toString());

        managedProperties = includes.contains(SERVER_MANAGED.toString())
                || (!omits.contains(SERVER_MANAGED.toString()) && !minimal);
    }

    /**
     * @return Whether this prefer tag demands membership triples.
     */
    public boolean prefersMembership() {
        return membership;
    }

    /**
     * @return Whether this prefer tag demands containment triples.
     */
    public boolean prefersContainment() {
        return containment;
    }

    /**
     * @return Whether this prefer tag demands references triples.
     */
    public boolean prefersReferences() {
        return references;
    }
    /**
     * @return Whether this prefer tag demands embedded triples.
     */
    public boolean prefersEmbed() {
        return embed;
    }
    /**
     * @return Whether this prefer tag demands server managed properties.
     */
    public boolean prefersServerManaged() {
        return managedProperties;
    }
}