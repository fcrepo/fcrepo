/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.api.rdf;

import jakarta.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.models.FedoraResource;

import org.apache.jena.rdf.model.Model;

/**
 * Helper to generate an RDF model for a FedoraResourceImpl that (likely) creates
 * relations from our resource to other HTTP components
 *
 * @author awoods
 */
public interface UriAwareResourceModelFactory {

    /**
     * Given a resource, the UriInfo and a way to generate graph subjects,
     * create a model with triples to inject into an RDF response for the
     * resource (e.g. to add HATEOAS links)
     *
     * @param resource the resource
     * @param uriInfo the uri info
     * @return model containing triples for the given resource
     */
    Model createModelForResource(final FedoraResource resource, final UriInfo uriInfo);
}
