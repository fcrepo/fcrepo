/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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

