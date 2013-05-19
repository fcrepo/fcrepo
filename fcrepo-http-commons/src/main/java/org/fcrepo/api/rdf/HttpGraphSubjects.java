
package org.fcrepo.api.rdf;

import static org.fcrepo.utils.FedoraJcrTypes.FCR_CONTENT;

import java.net.URI;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class HttpGraphSubjects implements GraphSubjects {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(HttpGraphSubjects.class);

    private final UriBuilder nodesBuilder;

    private final String basePath;

    private final int pathIx;

    public HttpGraphSubjects(final Class<?> relativeTo, final UriInfo uris) {
        this.nodesBuilder = uris.getBaseUriBuilder().path(relativeTo);
        String basePath = nodesBuilder.build("").toString();
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        this.basePath = basePath;
        this.pathIx = basePath.length() - 1;
        LOGGER.debug("Resolving graph subjects to a base URI of \"{}\"",
                basePath);
    }

    @Override
    public Resource getGraphSubject(final Node node) throws RepositoryException {
        final URI result = nodesBuilder.buildFromMap(getPathMap(node));
        LOGGER.debug("Translated node {} into RDF subject {}", node, result);
        return ResourceFactory.createResource(result.toString());
    }

    @Override
    public Node getNodeFromGraphSubject(final Session session,
            final Resource subject) throws RepositoryException {
        if (!isFedoraGraphSubject(subject)) {
            LOGGER.debug(
                    "RDF resource {} was not a URI resource with our expected basePath {}, aborting.",
                    subject, basePath);
            return null;
        }

        final String absPath = subject.getURI().substring(pathIx);

        final Node node;
        if (absPath.endsWith(FCR_CONTENT)) {
            node =
                    session.getNode(absPath.replace(FedoraJcrTypes.FCR_CONTENT,
                            JcrConstants.JCR_CONTENT));
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

    private static Map<String, String> getPathMap(final Node node)
            throws RepositoryException {
        // the path param value doesn't start with a slash
        String path = node.getPath().substring(1);
        if (path.endsWith(JcrConstants.JCR_CONTENT)) {
            path =
                    path.replace(JcrConstants.JCR_CONTENT,
                            FedoraJcrTypes.FCR_CONTENT);
        }
        return ImmutableBiMap.of("path", path);
    }
}
