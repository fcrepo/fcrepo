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
package org.fcrepo.kernel.api.rdf;

/**
 * Kernel level API to hold the LdpPreferTag decisions.
 * @author whikloj
 * @since 6.0.0
 */
public interface LdpTriplePreferences {

    /**
     * @return Whether to return a minimal container.
     */
    boolean getMinimal();

    /**
     * @return Whether this prefer tag demands membership triples.
     */
    boolean prefersMembership();

    /**
     * @return Whether this prefer tag demands containment triples.
     */
    boolean prefersContainment();

    /**
     * @return Whether this prefer tag demands references triples.
     */
    boolean prefersReferences();

    /**
     * @return Whether this prefer tag demands embedded triples.
     */
    boolean prefersEmbed();

    /**
     * @return Whether this prefer tag demands server managed properties.
     */
    boolean prefersServerManaged();

    /**
     * @return Whether this prefer tag demands no minimal container, ie. no user RDF.
     */
    boolean preferNoUserRdf();
}
