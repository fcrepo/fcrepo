
package org.fcrepo.url;

import static com.google.common.collect.ImmutableBiMap.of;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_NAMESPACE_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_SEARCH_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.RdfLexicon.HAS_SITEMAP;
import static org.fcrepo.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_VERSION_HISTORY;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraResource;
import org.fcrepo.api.FedoraExport;
import org.fcrepo.api.FedoraFieldSearch;
import org.fcrepo.api.FedoraFixity;
import org.fcrepo.api.FedoraSitemap;
import org.fcrepo.api.FedoraTransactions;
import org.fcrepo.api.FedoraVersions;
import org.fcrepo.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @javax.annotation.Resource
    protected Map<String, FedoraObjectSerializer> serializers;

    @Override
    public Model createModelForResource(final FedoraResource resource,
            final UriInfo uriInfo, final GraphSubjects graphSubjects)
            throws RepositoryException {

        final Model model = createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType("mode:root")) {
            model.add(s, HAS_SEARCH_SERVICE, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraFieldSearch.class).build()
                    .toASCIIString()));
            model.add(s, HAS_SITEMAP, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraSitemap.class).build()
                    .toASCIIString()));
            model.add(s, HAS_TRANSACTION_SERVICE, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraTransactions.class).build()
                    .toASCIIString()));
            model.add(s, HAS_NAMESPACE_SERVICE, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraRepositoryNamespaces.class)
                    .build().toASCIIString()));

        } else {

            for (final String key : serializers.keySet()) {
                final Map<String, String> pathMap =
                        of("path", resource.getPath().substring(1), "format",
                                key);
                model.add(s, HAS_SERIALIZATION, model.createResource(uriInfo
                        .getBaseUriBuilder().path(FedoraExport.class)
                        .buildFromMap(pathMap).toASCIIString()));
            }

            final Map<String, String> pathMap =
                    of("path", resource.getPath().substring(1));
            model.add(s, HAS_VERSION_HISTORY, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraVersions.class)
                    .buildFromMap(pathMap).toASCIIString()));

        }

        if (resource.hasContent()) {
            final Map<String, String> pathMap =
                    of("path", resource.getPath().substring(1));
            model.add(s, HAS_FIXITY_SERVICE, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraFixity.class).buildFromMap(
                            pathMap).toASCIIString()));
        }

        return model;
    }
}
