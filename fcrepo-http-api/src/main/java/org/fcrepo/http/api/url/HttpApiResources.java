/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.http.api.url;

import static com.google.common.collect.ImmutableBiMap.of;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_WORKSPACE_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SEARCH_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_SITEMAP;
import static org.fcrepo.kernel.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_HISTORY;
import static org.fcrepo.kernel.RdfLexicon.RDFS_LABEL;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.http.api.FedoraExport;
import org.fcrepo.http.api.FedoraFieldSearch;
import org.fcrepo.http.api.FedoraFixity;
import org.fcrepo.http.api.FedoraSitemap;
import org.fcrepo.http.api.FedoraVersions;
import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.http.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.http.api.repository.FedoraRepositoryTransactions;
import org.fcrepo.http.api.repository.FedoraRepositoryWorkspaces;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Inject our HTTP API methods into the object graphs
 */
@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @Autowired
    protected SerializerUtil serializers;

    @Override
    public Model createModelForResource(final FedoraResource resource,
            final UriInfo uriInfo, final GraphSubjects graphSubjects)
        throws RepositoryException {

        final Model model = createDefaultModel();

        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType(
                FedoraJcrTypes.ROOT)) {
            addRepositoryStatements(uriInfo, model, s);
        } else {
            addNodeStatements(resource, uriInfo, model, s);
        }

        if (resource.hasContent()) {
            addContentStatements(resource, uriInfo, model, s);
        }

        // fcr:export?format=xyz
        for (final String key : serializers.keySet()) {
            final Map<String, String> pathMap =
                    of("path", resource.getPath().substring(1));
            final Resource format =
                    model.createResource(uriInfo.getBaseUriBuilder().path(
                            FedoraExport.class).queryParam("format", key)
                            .buildFromMap(pathMap).toASCIIString());
            model.add(s, HAS_SERIALIZATION, format);
            model.add(format, RDFS_LABEL, key);
        }

        return model;
    }

    private void addContentStatements(FedoraResource resource, UriInfo uriInfo,
            Model model, Resource s) throws RepositoryException {
        // fcr:fixity
        final Map<String, String> pathMap =
                of("path", resource.getPath().substring(1));
        model.add(s, HAS_FIXITY_SERVICE, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraFixity.class).buildFromMap(
                        pathMap).toASCIIString()));
    }

    private void addNodeStatements(FedoraResource resource, UriInfo uriInfo,
            Model model, Resource s) throws RepositoryException {

        // fcr:versions
        final Map<String, String> pathMap =
                of("path", resource.getPath().substring(1));
        model.add(s, HAS_VERSION_HISTORY, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraVersions.class).buildFromMap(
                        pathMap).toASCIIString()));
    }

    private void addRepositoryStatements(UriInfo uriInfo, Model model,
            Resource s) {
        // fcr:search
        model.add(s, HAS_SEARCH_SERVICE, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraFieldSearch.class).build()
                .toASCIIString()));

        // sitemap
        model.add(s, HAS_SITEMAP, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraSitemap.class).build()
                .toASCIIString()));

        // fcr:tx
        model.add(s, HAS_TRANSACTION_SERVICE, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryTransactions.class)
                .build().toASCIIString()));

        // fcr:namespaces
        model.add(s, HAS_NAMESPACE_SERVICE, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryNamespaces.class)
                .build().toASCIIString()));

        // fcr:workspaces
        model.add(s, HAS_WORKSPACE_SERVICE, model.createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryWorkspaces.class)
                .build().toASCIIString()));
    }

}
