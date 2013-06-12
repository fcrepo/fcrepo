package org.fcrepo.api.rdf;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraResource;
import org.fcrepo.rdf.GraphSubjects;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Helper to generate an RDF model for a FedoraResource that (likely) creates relations
 * from our resource to other HTTP components
 */
public interface UriAwareResourceModelFactory {
    Model createModelForResource(final FedoraResource resource, final UriInfo uriInfo, GraphSubjects graphSubjects) throws RepositoryException;
}
