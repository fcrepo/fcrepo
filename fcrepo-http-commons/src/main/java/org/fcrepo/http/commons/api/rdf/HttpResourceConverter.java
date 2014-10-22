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
package org.fcrepo.http.commons.api.rdf;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.IdentifierConversionException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.identifiers.HashConverter;
import org.fcrepo.kernel.impl.identifiers.NamespaceConverter;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.ws.rs.core.UriBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.replaceOnce;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.services.TransactionServiceImpl.getCurrentTransactionId;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFrozenNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext;

/**
 * Convert between Jena Resources and JCR Nodes using a JAX-RS UriBuilder to mediate the
 * URI translation.
 *
 * @author cabeer
 * @since 10/5/14
 */
public class HttpResourceConverter extends IdentifierConverter<Resource,FedoraResource> {

    private static final Logger LOGGER = getLogger(HttpResourceConverter.class);

    protected List<Converter<String, String>> translationChain;

    private final Session session;
    private final UriBuilder uriBuilder;

    protected Converter<String, String> forward = identity();
    protected Converter<String, String> reverse = identity();

    private final UriTemplate uriTemplate;

    /**
     * Create a new identifier converter within the given session with the given URI template
     * @param session
     * @param uriBuilder
     */
    public HttpResourceConverter(final Session session,
                                 final UriBuilder uriBuilder) {

        this.session = session;
        this.uriBuilder = uriBuilder;
        this.uriTemplate = new UriTemplate(uriBuilder.toTemplate());

        resetTranslationChain();
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uriBuilder.toTemplate());
    }

    @Override
    protected FedoraResource doForward(final Resource resource) {
        try {
            final HashMap<String, String> values = new HashMap<>();

            final String path = asString(resource, values);

            if (path != null) {
                final Node node = getNode(path);

                final boolean metadata = values.containsKey("path")
                        && values.get("path").endsWith("/" + FCR_METADATA);

                final FedoraResource fedoraResource = nodeConverter.convert(node);

                if (!metadata && fedoraResource instanceof Datastream) {
                    return ((Datastream)fedoraResource).getBinary();
                } else {
                    return fedoraResource;
                }
            } else {
                throw new IdentifierConversionException("Asked to translate a resource " + resource
                        + " that doesn't match the URI template");
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    protected Resource doBackward(final FedoraResource resource) {
        try {
            return toDomain(doBackwardPathOnly(resource));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean inDomain(final Resource resource) {
        final HashMap<String, String> values = new HashMap<>();
        return uriTemplate.match(resource.getURI(), values) && values.containsKey("path");
    }

    @Override
    public Resource toDomain(final String path) {

        final String realPath;
        if (path == null) {
            realPath = "";
        } else if (path.startsWith("/")) {
            realPath = path.substring(1);
        } else {
            realPath = path;
        }

        final UriBuilder uri = uriBuilder();

        if (realPath.contains("#")) {

            final String[] split = realPath.split("#", 2);

            uri.resolveTemplate("path", split[0], false);
            uri.fragment(split[1]);
        } else {
            uri.resolveTemplate("path", realPath, false);

        }
        return createResource(uri.build().toString());
    }

    @Override
    public String asString(final Resource resource) {
        final HashMap<String, String> values = new HashMap<>();

        return asString(resource, values);
    }

    /**
     * Convert the incoming Resource to a JCR path (but don't attempt to load the node).
     *
     * @param resource Jena Resource to convert
     * @param values a map that will receive the matching URI template variables for future use.
     * @return
     */
    private String asString(final Resource resource, final Map<String, String> values) {
        if (uriTemplate.match(resource.getURI(), values) && values.containsKey("path")) {
            String path = "/" + values.get("path");

            final boolean metadata = path.endsWith("/" + FCR_METADATA);

            if (metadata) {
                path = replaceOnce(path, "/" + FCR_METADATA, EMPTY);
            }

            path = forward.convert(path);

            if (path == null) {
                return null;
            }

            try {
                path = URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOGGER.debug("Unable to URL-decode path " + e + " as UTF-8", e);
            }

            if (path.isEmpty()) {
                return "/";
            } else {
                return path;
            }
        } else {
            return null;
        }
    }


    private Node getNode(final String path) throws RepositoryException {
        if (path.contains(FCR_VERSIONS)) {
            final String[] split = path.split("/" + FCR_VERSIONS + "/", 2);
            final String versionedPath = split[0];
            final String versionAndPathIntoVersioned = split[1];
            final String[] split1 = versionAndPathIntoVersioned.split("/", 2);
            final String version = split1[0];

            final String pathIntoVersioned;
            if (split1.length > 1) {
                pathIntoVersioned = split1[1];
            } else {
                pathIntoVersioned = "";
            }

            final Node node = getFrozenNodeByLabel(versionedPath, version);

            if (pathIntoVersioned.isEmpty()) {
                return node;
            } else if (node != null) {
                return node.getNode(pathIntoVersioned);
            } else {
                throw new PathNotFoundException("Unable to find versioned resource at " + path);
            }
        } else {
            return session.getNode(path);
        }
    }

    /**
     * A private helper method that tries to look up frozen node for the given subject
     * by a label.  That label may either be one that was assigned at creation time
     * (and is a version label in the JCR sense) or a system assigned identifier that
     * was used for versions created without a label.  The current implementation
     * uses the JCR UUID for the frozen node as the system-assigned label.
     */
    private Node getFrozenNodeByLabel(final String baseResourcePath, final String label) {
        try {
            try {
                final Node frozenNode = session.getNodeByIdentifier(label);

            /*
             * We found a node whose identifier is the "label" for the version.  Now
             * we must do due dilligence to make sure it's a frozen node representing
             * a version of the subject node.
             */
                final Property p = frozenNode.getProperty("jcr:frozenUuid");
                if (p != null) {
                    final Node subjectNode = session.getNode(baseResourcePath);
                    if (p.getString().equals(subjectNode.getIdentifier())) {
                        return frozenNode;
                    }
                }
            /*
             * Though a node with an id of the label was found, it wasn't the
             * node we were looking for, so fall through and look for a labeled
             * node.
             */
            } catch (final ItemNotFoundException ex) {
            /*
             * the label wasn't a uuid of a frozen node but
             * instead possibly a version label.
             */
            }

            final VersionHistory hist =
                    session.getWorkspace().getVersionManager().getVersionHistory(baseResourcePath);
            if (hist.hasVersionLabel(label)) {
                LOGGER.debug("Found version for {} by label {}.", baseResourcePath, label);
                return hist.getVersionByLabel(label).getFrozenNode();
            }
            LOGGER.warn("Unknown version {} with label or uuid {}!", baseResourcePath, label);
            throw new PathNotFoundException("Unknown version " + baseResourcePath
                    + " with label or uuid " + label);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private String getPath(final FedoraResource resource) throws RepositoryException {
        if (isFrozenNode.apply(resource)) {
            try {
                Node versionableFrozenNode = resource.getNode();
                Node versionableNode = session.getNodeByIdentifier(
                        versionableFrozenNode.getProperty("jcr:frozenUuid").getString());
                String pathWithinVersionable = "";
                while (!versionableNode.isNodeType("mix:versionable")) {
                    LOGGER.debug("node {} is not versionable, checking parent...", versionableNode.getIdentifier());
                    pathWithinVersionable = "/" + getRelativePath(versionableNode,
                            versionableNode.getParent()) + pathWithinVersionable;
                    versionableFrozenNode = versionableFrozenNode.getParent();
                    versionableNode = session.getNodeByIdentifier(
                            versionableFrozenNode.getProperty("jcr:frozenUuid").getString());
                }

                pathWithinVersionable = versionableFrozenNode.getIdentifier()
                        + (pathWithinVersionable.length() > 0 ? pathWithinVersionable : "");
                final String pathToVersionable = versionableNode.getPath();
                LOGGER.debug("frozen node found with id {}.", versionableFrozenNode.getIdentifier());
                final String path = pathToVersionable + "/" + FCR_VERSIONS + "/" + pathWithinVersionable;
                return path.startsWith("/") ? path : "/" + path;
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        } else {
            return resource.getPath();
        }
    }

    private static String getRelativePath(final Node node, final Node ancestor) {
        try {
            return node.getPath().substring(ancestor.getPath().length() + 1);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get only the resource path to this resource, before embedding it in a full URI
     * @param resource
     * @return
     * @throws RepositoryException
     */
    private String doBackwardPathOnly(final FedoraResource resource) throws RepositoryException {
        String path = reverse.convert(getPath(resource));
        if (path != null) {

            if (resource instanceof Datastream) {
                path = path + "/" + FCR_METADATA;
            }

            if (path.endsWith(JCR_CONTENT)) {
                path = replaceOnce(path, "/" + JCR_CONTENT, EMPTY);
            }
            return path;
        } else {
            throw new RuntimeException("Unable to process reverse chain for resource " + resource);
        }
    }


    protected void resetTranslationChain() {
        if (translationChain == null) {
            translationChain = getTranslationChain();

            final Converter<String,String> transactionIdentifierConverter = new TransactionIdentifierConverter(session);

            setTranslationChain(ImmutableList.copyOf(
                    Iterables.concat(Lists.newArrayList(
                                    transactionIdentifierConverter),
                            translationChain)));
        }
    }

    private void setTranslationChain(final List<Converter<String, String>> chained) {

        translationChain = chained;

        for (final Converter<String, String> t : translationChain) {
            forward = forward.andThen(t);
        }
        for (final Converter<String, String> t : Lists.reverse(translationChain)) {
            reverse = reverse.andThen(t.reverse());
        }
    }


    private static final List<Converter<String,String>> minimalTranslationChain =
            Lists.newArrayList(
                    (Converter<String, String>) new NamespaceConverter(),
                    (Converter<String, String>) new HashConverter()
            );

    protected List<Converter<String,String>> getTranslationChain() {
        final ApplicationContext context = getApplicationContext();
        if (context != null) {
            final List<Converter<String,String>> tchain =
                    getApplicationContext().getBean("translationChain", List.class);
            return tchain;
        } else {
            return minimalTranslationChain;
        }
    }

    protected ApplicationContext getApplicationContext() {
        return getCurrentWebApplicationContext();
    }

    /**
     * Translate the current transaction into the identifier
     */
    class TransactionIdentifierConverter extends Converter<String, String> {
        public static final String TX_PREFIX = "tx:";

        private final Session session;

        public TransactionIdentifierConverter(final Session session) {
            this.session = session;
        }

        @Override
        protected String doForward(final String path) {

            if (path.contains(TX_PREFIX) && !path.contains(txSegment())) {
                throw new RepositoryRuntimeException("Path " + path
                        + " is not in current transaction " +  getCurrentTransactionId(session));
            }

            return replaceOnce(path, txSegment(), EMPTY);
        }

        @Override
        protected String doBackward(final String path) {
            return txSegment() + path;
        }

        private String txSegment() {

            final String txId = getCurrentTransactionId(session);

            if (txId != null) {
                return "/" + TX_PREFIX + txId;
            } else {
                return EMPTY;
            }
        }
    }
}
