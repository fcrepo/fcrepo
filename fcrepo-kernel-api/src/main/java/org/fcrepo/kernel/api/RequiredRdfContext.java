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
package org.fcrepo.kernel.api;

/**
 * A collection of RDF contexts that can be used to extract triples from FedoraResources. All implementations of the
 * Fedora kernel are required to support these {@link TripleCategory}s, but may choose to support others.
 *
 * @author acoburn
 * @since Dec 4, 2015
 */
public enum RequiredRdfContext implements TripleCategory {

    /* A Minimal representation of Rdf Triples */
    MINIMAL,

    /* Versions Context */
    VERSIONS,

    /* fedora:EmbedResources Context: embedded child resources */
    EMBED_RESOURCES,

    /* fedora:InboundReferences Context: assertions from other Fedora resources */
    INBOUND_REFERENCES,

    /* fedora:PreferMembership Context: ldp membership triples */
    LDP_MEMBERSHIP,

    /* fedora:PreferContainment Context: ldp containment triples */
    LDP_CONTAINMENT
}

