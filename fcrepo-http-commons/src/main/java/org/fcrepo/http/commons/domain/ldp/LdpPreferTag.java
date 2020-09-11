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

import java.util.List;
import java.util.Optional;

import org.fcrepo.http.commons.domain.PreferTag;

/**
 * A subclass of {@link PreferTag} that contemplates the possible preferences for Linked Data Platform requests.
 *
 * @author ajs6f
 */
public class LdpPreferTag extends PreferTag {

    private final boolean membership;

    private final boolean containment;

    private final boolean references;

    private final boolean embed;

    private final boolean managedProperties;

    private final boolean noMinimal;

    /**
     * Standard constructor.
     *
     * @param preferTag the prefer tag
     */
    public LdpPreferTag(final PreferTag preferTag) {
        super(preferTag);

        final Optional<String> include = ofNullable(preferTag.getParams().get("include"));
        final Optional<String> omit = ofNullable(preferTag.getParams().get("omit"));
        final Optional<String> received = ofNullable(preferTag.getParams().get("received"));

        final List<String> includes = asList(include.orElse(" ").split(" "));
        final List<String> omits = asList(omit.orElse(" ").split(" "));

        final boolean minimal = preferTag.getValue().equals("minimal") || received.orElse("").equals("minimal");

        final boolean preferMinimalContainer = (!omits.contains(PREFER_MINIMAL_CONTAINER.toString()) &&
                (includes.contains(PREFER_MINIMAL_CONTAINER.toString()) || minimal));

        noMinimal = omits.contains(PREFER_MINIMAL_CONTAINER.toString());

        membership = (!preferMinimalContainer && !omits.contains(PREFER_MEMBERSHIP.toString())) ||
                includes.contains(PREFER_MEMBERSHIP.toString());

        containment = (!preferMinimalContainer && !omits.contains(PREFER_CONTAINMENT.toString()) &&
                !omits.contains(PREFER_SERVER_MANAGED.toString()))
                || includes.contains(PREFER_CONTAINMENT.toString());

        references = includes.contains(INBOUND_REFERENCES.toString());

        embed = includes.contains(EMBED_CONTAINED.toString());

        managedProperties = includes.contains(PREFER_SERVER_MANAGED.toString())
                || (!omits.contains(PREFER_SERVER_MANAGED.toString()) && !minimal);
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

    /**
     * @return Whether this prefer tag demands no minimal container, ie. no user RDF.
     */
    public boolean preferNoUserRdf() {
        return noMinimal;
    }
}
