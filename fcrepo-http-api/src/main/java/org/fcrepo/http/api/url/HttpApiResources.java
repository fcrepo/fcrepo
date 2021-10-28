/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.url;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_ROOT;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.http.api.Transactions;
import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;

import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

/**
 * Inject our HTTP API methods into the object graphs
 *
 * @author awoods
 */
@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @Override
    public Model createModelForResource(final FedoraResource resource,
        final UriInfo uriInfo) {

        final Model model = createDefaultModel();

        final Resource s = createResource(resource.getFedoraId().getFullId());

        if (resource.hasType(REPOSITORY_ROOT.getURI())) {
            addRepositoryStatements(uriInfo, model, s);
        }

        if (resource instanceof NonRdfSourceDescription && !resource.isMemento()) {
            addContentStatements((Binary)resource.getDescribedResource(), model);
        }
        return model;
    }

    private static void addContentStatements(final Binary resource,
                                             final Model model) {
        // fcr:fixity
        final Resource subject = createResource(resource.getFedoraId().getFullId());
        model.add(subject, HAS_FIXITY_SERVICE, createResource(subject.getURI() +
                "/fcr:fixity"));
    }


    private void addRepositoryStatements(final UriInfo uriInfo, final Model model,
        final Resource s) {
        // fcr:tx
        model.add(s, HAS_TRANSACTION_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(Transactions.class)
                .build().toASCIIString()));
    }

}
