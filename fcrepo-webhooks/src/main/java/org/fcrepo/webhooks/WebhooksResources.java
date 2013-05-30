package org.fcrepo.webhooks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.FedoraResource;
import org.fcrepo.RdfLexicon;
import org.fcrepo.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

public class WebhooksResources implements UriAwareResourceModelFactory {
    @Override
    public Model createModelForResource(FedoraResource resource, UriInfo uriInfo, GraphSubjects graphSubjects) throws RepositoryException {
        final Model model = ModelFactory.createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType(FedoraJcrTypes.ROOT)) {
            model.add(s, RdfLexicon.HAS_SUBSCRIPTION_SERVICE, model.createResource(uriInfo.getBaseUriBuilder().path(FedoraWebhooks.class).build().toASCIIString()));
        }

        return model;
    }
}
