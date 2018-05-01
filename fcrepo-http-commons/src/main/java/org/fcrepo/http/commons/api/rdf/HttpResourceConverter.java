/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.replaceOnce;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.CONTAINER_WEBAC_ACL;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.LDPCV_TIME_MAP;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.validatePath;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.UriBuilder;

import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.IdentifierConversionException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraWebacAcl;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.modeshape.TombstoneImpl;
import org.fcrepo.kernel.modeshape.identifiers.HashConverter;
import org.fcrepo.kernel.modeshape.identifiers.NamespaceConverter;

import org.apache.jena.rdf.model.Resource;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;

/**
 * Convert between Jena Resources and JCR Nodes using a JAX-RS UriBuilder to mediate the
 * URI translation.
 *
 * @author cabeer
 * @since 10/5/14
 */
public class HttpResourceConverter extends IdentifierConverter<Resource,FedoraResource> {

    private static final Logger LOGGER = getLogger(HttpResourceConverter.class);

    // Regex pattern which decomposes a http resource uri into components
    // First group is the path of the resource or original resource,
    // second group determines if it is an fcr:metadata non-rdf source,
    // third group determines if the path is for a memento or timemap,
    // the fourth group allows for a memento identifier, and the fifth group for ACL
    private final static Pattern FORWARD_COMPONENT_PATTERN = Pattern.compile(
            "(.*?)(/" + FCR_METADATA + ")?(/" + FCR_VERSIONS + "(/\\d+)?)?(/" + FCR_ACL + ")?$");

    protected List<Converter<String, String>> translationChain;

    private final FedoraSession session;
    private final UriBuilder uriBuilder;

    protected Converter<String, String> forward = identity();
    protected Converter<String, String> reverse = identity();

    private final UriTemplate uriTemplate;
    private final boolean batch;

    /**
     * Create a new identifier converter within the given session with the given URI template
     * @param session the session
     * @param uriBuilder the uri builder
     */
    public HttpResourceConverter(final HttpSession session,
                                 final UriBuilder uriBuilder) {

        this.session = session.getFedoraSession();
        this.uriBuilder = uriBuilder;
        this.batch = session.isBatchSession();
        this.uriTemplate = new UriTemplate(uriBuilder.toTemplate());

        resetTranslationChain();
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uriBuilder.toTemplate());
    }

    @Override
    protected FedoraResource doForward(final Resource resource) {
        final Map<String, String> values = new HashMap<>();
        final String path = asString(resource, values);
        final Session jcrSession = getJcrSession(session);
        try {
            if (path != null) {
                final Node node = getNode(path);

                final boolean metadata = values.containsKey("path")
                        && values.get("path").contains("/" + FCR_METADATA);

                final FedoraResource fedoraResource = nodeConverter.convert(node);

                if (!metadata && fedoraResource instanceof NonRdfSourceDescription) {
                    return fedoraResource.getDescribedResource();
                }
                return fedoraResource;
            }
            throw new IdentifierConversionException("Asked to translate a resource " + resource
                    + " that doesn't match the URI template");
        } catch (final RepositoryException e) {
            validatePath(jcrSession, path);

            if ( e instanceof PathNotFoundException ) {
                try {
                    final Node preexistingNode = getClosestExistingAncestor(jcrSession, path);
                    if (TombstoneImpl.hasMixin(preexistingNode)) {
                        throw new TombstoneException(new TombstoneImpl(preexistingNode));
                    }
                } catch (final RepositoryException inner) {
                    LOGGER.debug("Error checking for parent tombstones", inner);
                }
            }
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    protected Resource doBackward(final FedoraResource resource) {
        return toDomain(doBackwardPathOnly(resource));
    }

    @Override
    public boolean inDomain(final Resource resource) {
        final Map<String, String> values = new HashMap<>();

        return uriTemplate.match(resource.getURI(), values) && values.containsKey("path") ||
            isRootWithoutTrailingSlash(resource);
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
        final Map<String, String> values = new HashMap<>();

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

            final Matcher matcher = FORWARD_COMPONENT_PATTERN.matcher(path);

            if (matcher.matches()) {
                final boolean metadata = matcher.group(2) != null;
                final boolean versioning = matcher.group(3) != null;
                final boolean webacAcl = matcher.group(5) != null;
                final String basePath = matcher.group(1);

                if (versioning) {
                    path = replaceOnce(path, "/" + FCR_VERSIONS, "/" + LDPCV_TIME_MAP);
                }

                if (metadata) {
                    path = replaceOnce(path, "/" + FCR_METADATA, "/" + FEDORA_DESCRIPTION);
                }

                if (webacAcl) {
                    path = replaceOnce(path, "/" + FCR_ACL, "/" + CONTAINER_WEBAC_ACL);
                }
            }

            path = forward.convert(path);

            if (path == null) {
                return null;
            }

            try {
                path = URLDecoder.decode(path, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                LOGGER.debug("Unable to URL-decode path " + e + " as UTF-8", e);
            }

            if (path.isEmpty()) {
                return "/";
            }

            // Validate path
            if (path.contains("//")) {
                throw new InvalidResourceIdentifierException("Path contains empty element! " + path);
            }
            return path;
        }

        if (isRootWithoutTrailingSlash(resource)) {
            return "/";
        }

        return null;
    }


    private Node getNode(final String path) throws RepositoryException {
        try {
            return getJcrSession(session).getNode(path);
        } catch (final IllegalArgumentException ex) {
            throw new InvalidResourceIdentifierException("Illegal path: " + path);
        }
    }

    /**
     * Get only the resource path to this resource, before embedding it in a full URI
     * @param resource
     * @return
     */
    private String doBackwardPathOnly(final FedoraResource resource) {

        String path = reverse.convert(resource.getPath());
        if (path == null) {
            throw new RepositoryRuntimeException("Unable to process reverse chain for resource " + resource);
        }

        if (resource instanceof FedoraWebacAcl) {
            // For ACL container, replace the name with fcr:acl path
            path = replaceOnce(path, "/" + CONTAINER_WEBAC_ACL, "/" + FCR_ACL);
        }

        path = replaceOnce(path, "/" + LDPCV_TIME_MAP, "/" + FCR_VERSIONS);

        path = replaceOnce(path, "/" + FEDORA_DESCRIPTION, "/" + FCR_METADATA);

        return path;
    }


    protected void resetTranslationChain() {
        if (translationChain == null) {
            translationChain = getTranslationChain();
            final List<Converter<String, String>> newChain =
                    new ArrayList<>(singleton(new TransactionIdentifierConverter(session, batch)));
            newChain.addAll(translationChain);
            setTranslationChain(newChain);
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


    private static final List<Converter<String, String>> minimalTranslationChain =
            of(new NamespaceConverter(), new HashConverter());

    protected List<Converter<String,String>> getTranslationChain() {
        final ApplicationContext context = getApplicationContext();
        if (context != null) {
            @SuppressWarnings("unchecked")
            final List<Converter<String,String>> tchain =
                    getApplicationContext().getBean("translationChain", List.class);
            return tchain;
        }
        return minimalTranslationChain;
    }

    protected ApplicationContext getApplicationContext() {
        return getCurrentWebApplicationContext();
    }

    /**
     * Translate the current transaction into the identifier
     */
    static class TransactionIdentifierConverter extends Converter<String, String> {
        public static final String TX_PREFIX = "tx:";

        private final FedoraSession session;
        private final boolean batch;

        public TransactionIdentifierConverter(final FedoraSession session, final boolean batch) {
            this.session = session;
            this.batch = batch;
        }

        @Override
        protected String doForward(final String path) {

            if (path.contains(TX_PREFIX) && !path.contains(txSegment())) {
                throw new RepositoryRuntimeException("Path " + path
                        + " is not in current transaction " +  session.getId());
            }

            return replaceOnce(path, txSegment(), EMPTY);
        }

        @Override
        protected String doBackward(final String path) {
            return txSegment() + path;
        }

        private String txSegment() {
            return batch ? "/" + TX_PREFIX + session.getId() : EMPTY;
        }
    }

    private boolean isRootWithoutTrailingSlash(final Resource resource) {
        final Map<String, String> values = new HashMap<>();

        return uriTemplate.match(resource.getURI() + "/", values) && values.containsKey("path") &&
            values.get("path").isEmpty();
    }
}
