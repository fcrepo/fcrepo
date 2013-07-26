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

package org.fcrepo.api.rdf;

import static org.fcrepo.utils.FedoraJcrTypes.FCR_CONTENT;

import java.net.URI;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.TransactionService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Translate JCR paths to URLs to the given class
 */
public class HttpGraphSubjects implements GraphSubjects {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(HttpGraphSubjects.class);

    private final UriBuilder nodesBuilder;

    private final String basePath;

    private final int pathIx;
    private final URI context;
    private final Session session;

    /**
     * Build HTTP graph subjects relative to the given JAX-RS resource, using the UriInfo provided.
     *
     * The session may provide additional information (e.g. workspaces) that need to be
     * taken into account.
     *
     * @param relativeTo
     * @param uris
     */
    public HttpGraphSubjects(final Session session, final Class<?> relativeTo, final UriInfo uris) {
        this.context = uris.getRequestUri();
        this.nodesBuilder = uris.getBaseUriBuilder().path(relativeTo);
        String basePath = nodesBuilder.build("").toString();
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        this.basePath = basePath;
        this.pathIx = basePath.length() - 1;
        this.session = session;
        LOGGER.debug("Resolving graph subjects to a base URI of \"{}\"",
                basePath);
    }

    @Override
    public Resource getGraphSubject(final String absPath)
        throws RepositoryException {
        final URI result =
                nodesBuilder.buildFromMap(getPathMap(absPath));
        LOGGER.debug("Translated path {} into RDF subject {}", absPath, result);
        return ResourceFactory.createResource(result.toString());
    }

    @Override
    public Resource getContext() {
        return ResourceFactory.createResource(context.toString());
    }

    @Override
    public Resource getGraphSubject(final Node node) throws RepositoryException {
        final URI result = nodesBuilder.buildFromMap(getPathMap(node));
        LOGGER.debug("Translated node {} into RDF subject {}", node, result);
        return ResourceFactory.createResource(result.toString());
    }

    @Override
    public Node getNodeFromGraphSubject(final Resource subject) throws RepositoryException {
        if (!isFedoraGraphSubject(subject)) {
            LOGGER.debug(
                    "RDF resource {} was not a URI resource with our expected basePath {}, aborting.",
                    subject, basePath);
            return null;
        }

        final StringBuilder pathBuilder = new StringBuilder();
        final String absPath;
        final String[] pathSegments =
                subject.getURI().substring(pathIx).split("/");

        for (String segment : pathSegments) {
            if (segment.startsWith("tx:")) {
                String tx = segment.substring("tx:".length());

                final String currentTxId = TransactionService.getCurrentTransactionId(session);

                if (currentTxId != null && tx.equals(currentTxId)) {
                    // no-op
                } else {
                    throw new RepositoryException(
                            "Subject is not in this transaction");
                }

            } else if (segment.startsWith("workspace:")) {
                String workspace = segment.substring("workspace:".length());
                if (!session.getWorkspace().getName().equals(workspace)) {
                    throw new RepositoryException(
                            "Subject is not in this workspace");
                }
            } else {
                if (!segment.isEmpty()) {
                    pathBuilder.append("/");
                    pathBuilder.append(segment);
                }
            }
        }

        absPath = pathBuilder.toString();

        final Node node;

        if (absPath.endsWith(FCR_CONTENT)) {
            final String contentPath =
                absPath.replace(FedoraJcrTypes.FCR_CONTENT,
                                   JcrConstants.JCR_CONTENT);
            node = session.getNode(contentPath);
            LOGGER.trace(
                    "RDF resource {} is a fcr:content node, retrieving the corresponding JCR content node {}",
                    subject, node);
        } else if (session.nodeExists(absPath)) {
            node = session.getNode(absPath);
            LOGGER.trace("RDF resource {} maps to JCR node {}", subject, node);
        } else {
            node = null;
            LOGGER.debug(
                    "RDF resource {} looks like a Fedora node, but when we checked was not in the repository",
                    subject);
        }

        return node;

    }

    @Override
    public boolean isFedoraGraphSubject(final Resource subject) {
        return subject.isURIResource() && subject.getURI().startsWith(basePath);
    }

    private Map<String, String> getPathMap(final Node node)
        throws RepositoryException {
        return getPathMap(node.getPath());
    }

    private Map<String, String> getPathMap(final String absPath) throws RepositoryException {
        // the path param value doesn't start with a slash
        String path = absPath.substring(1);
        if (path.endsWith(JcrConstants.JCR_CONTENT)) {
            path =
                    path.replace(JcrConstants.JCR_CONTENT,
                            FedoraJcrTypes.FCR_CONTENT);
        }

        if (session != null) {
            final Workspace workspace = session.getWorkspace();

            final String txId = TransactionService.getCurrentTransactionId(session);

            if (txId != null) {
                path = "tx:" + txId + "/" + path;
            } else if (workspace != null &&
                    !workspace.getName().equals("default")) {
                path = "workspace:" + workspace.getName() + "/" + path;
            }
        }

        return ImmutableBiMap.of("path", path);
    }
}
