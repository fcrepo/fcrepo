package org.fcrepo.syndication;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.rdf.GraphSubjects;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

public class RssResources implements UriAwareResourceModelFactory {
    @Override
    public Model createModelForResource(FedoraResource resource, UriInfo uriInfo, GraphSubjects graphSubjects) throws RepositoryException {

        final Model model = ModelFactory.createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType("mode:root")) {
            model.add(s, model.createProperty("http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#link-type-feed"), model.createResource(uriInfo.getBaseUriBuilder().path(RSSPublisher.class).build().toASCIIString()));
        }

        return model;
    }
}
