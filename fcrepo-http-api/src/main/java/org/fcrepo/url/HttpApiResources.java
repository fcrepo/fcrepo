package org.fcrepo.url;

import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
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

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import java.util.Map;

@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @javax.annotation.Resource
    protected Map<String, FedoraObjectSerializer> serializers;

    @Override
    public Model createModelForResource(FedoraResource resource, UriInfo uriInfo, GraphSubjects graphSubjects) throws RepositoryException {

        final Model model = ModelFactory.createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType("mode:root")) {
            model.add(s, model.createProperty("http://www.whatwg.org/specs/web-apps/current-work/multipage/links.html#link-type-search"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraFieldSearch.class).build().toASCIIString()));
            model.add(s, model.createProperty("http://microformats.org/wiki/rel-sitemap"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraSitemap.class).build().toASCIIString()));
            model.add(s, model.createProperty("info:fedora/hasTransactionProvider"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraTransactions.class).build().toASCIIString()));
            model.add(s, model.createProperty("info:fedora/hasNamespaces"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraRepositoryNamespaces.class).build().toASCIIString()));

        } else {

            for (String key : serializers.keySet()) {
                final Map<String, String> pathMap = ImmutableBiMap.of("path", resource.getPath().substring(1), "format", key);
                model.add(s, model.createProperty("info:fedora/exportsAs"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraExport.class).buildFromMap(pathMap).toASCIIString()));
            }

            final Map<String, String> pathMap = ImmutableBiMap.of("path", resource.getPath().substring(1));
            model.add(s, model.createProperty("info:fedora/hasVersions"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraVersions.class).buildFromMap(pathMap).toASCIIString()));

        }

        if (resource.hasContent()) {
            final Map<String, String> pathMap = ImmutableBiMap.of("path", resource.getPath().substring(1));
            model.add(s, model.createProperty("info:fedora/runFixityCheck"), model.createResource(uriInfo.getBaseUriBuilder().path(FedoraFixity.class).buildFromMap(pathMap).toASCIIString()));
        }

        return model;
    }
}
