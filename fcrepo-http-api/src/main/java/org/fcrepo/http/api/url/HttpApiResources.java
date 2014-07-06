/**
 * Copyright 2014 DuraSpace, Inc.
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

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.singletonMap;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCK;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SEARCH_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_SITEMAP;
import static org.fcrepo.kernel.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_HISTORY;
import static org.fcrepo.kernel.RdfLexicon.HAS_WORKSPACE_SERVICE;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.api.FedoraExport;
import org.fcrepo.http.api.FedoraFieldSearch;
import org.fcrepo.http.api.FedoraFixity;
import org.fcrepo.http.api.FedoraLocks;
import org.fcrepo.http.api.repository.FedoraRepositorySitemap;
import org.fcrepo.http.api.FedoraVersions;
import org.fcrepo.http.api.repository.FedoraRepositoryExport;
import org.fcrepo.http.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.http.api.repository.FedoraRepositoryTransactions;
import org.fcrepo.http.api.repository.FedoraRepositoryWorkspaces;
import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.serialization.SerializerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import java.util.Map;

/**
 * Inject our HTTP API methods into the object graphs
 *
 * @author awoods
 */
@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @Autowired
    protected SerializerUtil serializers;

    @Override
    public Model createModelForResource(final FedoraResource resource,
        final UriInfo uriInfo, final IdentifierTranslator graphSubjects)
        throws RepositoryException {

        final Model model = createDefaultModel();

        final Resource s = graphSubjects.getSubject(resource.getNode().getPath());

        if (resource.getNode().getPrimaryNodeType().isNodeType(ROOT)) {
            addRepositoryStatements(uriInfo, model, s);
        } else {
            addNodeStatements(resource, uriInfo, model, s, graphSubjects);
        }

        if (resource.hasContent()) {
            addContentStatements(resource, uriInfo, model, s, graphSubjects);
        }

        return model;
    }

    private static void addContentStatements(final FedoraResource resource, final UriInfo uriInfo,
        final Model model, final Resource s, final IdentifierTranslator graphSubjects) throws RepositoryException {
        // fcr:fixity
        final Map<String, String> pathMap =
            singletonMap("path", resource.getPath(graphSubjects).substring(1));
        model.add(s, HAS_FIXITY_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraFixity.class).buildFromMap(
                        pathMap).toASCIIString()));
    }

    private void addNodeStatements(final FedoraResource resource, final UriInfo uriInfo,
        final Model model, final Resource s, final IdentifierTranslator graphSubjects) throws RepositoryException {

        final Map<String, String> pathMap =
                singletonMap("path", resource.getPath(graphSubjects).substring(1));

        // hasLock
        if (resource.getNode().isLocked()) {
            final String path = resource.getNode().getPath();
            final Node lockHoldingNode
                = resource.getNode().getSession().getWorkspace()
                    .getLockManager().getLock(path).getNode();
            final Map<String, String> lockedNodePathMap =
                    singletonMap("path", lockHoldingNode.getPath().substring(1));
            model.add(s, HAS_LOCK, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraLocks.class).buildFromMap(
                        lockedNodePathMap).toASCIIString()));
        }

        // fcr:versions
        if (resource.getNode().isNodeType(NodeType.MIX_VERSIONABLE)) {
            model.add(s, HAS_VERSION_HISTORY, createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraVersions.class).buildFromMap(
                            pathMap).toASCIIString()));
        }

        // fcr:exports?format=xyz
        for (final String key : serializers.keySet()) {
            final Resource format =
                    createResource(uriInfo.getBaseUriBuilder().path(
                            FedoraExport.class).queryParam("format", key)
                            .buildFromMap(pathMap).toASCIIString());
            model.add(s, HAS_SERIALIZATION, format);
        }
    }

    private void addRepositoryStatements(final UriInfo uriInfo, final Model model,
        final Resource s) {
        // fcr:search
        model.add(s, HAS_SEARCH_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraFieldSearch.class).build()
                .toASCIIString()));

        // sitemap
        model.add(s, HAS_SITEMAP, createResource(uriInfo.getBaseUriBuilder()
                .path(FedoraRepositorySitemap.class).build().toASCIIString()));

        // fcr:tx
        model.add(s, HAS_TRANSACTION_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryTransactions.class)
                .build().toASCIIString()));

        // fcr:namespaces
        model.add(s, HAS_NAMESPACE_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryNamespaces.class)
                .build().toASCIIString()));

        // fcr:workspaces
        model.add(s, HAS_WORKSPACE_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryWorkspaces.class)
                .build().toASCIIString()));

        // fcr:export?format=xyz
        for (final String key : serializers.keySet()) {
            model.add(s, HAS_SERIALIZATION, createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraRepositoryExport.class)
                    .queryParam("format", key).build().toASCIIString()));
        }
    }

}
