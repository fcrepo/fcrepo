package org.fcrepo.webhooks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.rdf.GraphSubjects;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

public class WebhooksResources implements UriAwareResourceModelFactory {
    @Override
    public Model createModelForResource(FedoraResource resource, UriInfo uriInfo, GraphSubjects graphSubjects) throws RepositoryException {
        final Model model = ModelFactory.createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType("mode:root")) {
            model.add(s, model.createProperty("http://microformats.org/wiki/rel-subscription"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraWebhooks.class).build().toASCIIString()));
        }

        return model;
    }
}
