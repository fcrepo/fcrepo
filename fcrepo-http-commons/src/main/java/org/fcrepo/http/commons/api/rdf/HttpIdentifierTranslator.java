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

import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.HierarchyConverter;
import org.fcrepo.kernel.identifiers.InternalIdentifierConverter;
import org.fcrepo.kernel.identifiers.NamespaceConverter;
import org.fcrepo.kernel.services.functions.GetDefaultWorkspace;
import org.slf4j.Logger;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static javax.jcr.PropertyType.PATH;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.replaceOnce;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.fcrepo.kernel.services.TransactionServiceImpl.getCurrentTransactionId;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * Translate JCR paths to URLs.  There are a few types of translations
 * that occur as part of this implementation:
 * <ul>
 *     <li>jcr:content is replaced with fcr:content</li>
 *     <li>information about the transaction is added</li>
 * </ul>
 *
 */
/**
 * @author barmintor
 * @author ajs6f
 * @since Apr 2, 2014
 */
public class HttpIdentifierTranslator extends SpringContextAwareIdentifierTranslator {

    private static final Logger LOGGER = getLogger(HttpIdentifierTranslator.class);
    public static final String WORKSPACE_PREFIX = "workspace:";
    public static final String TX_PREFIX = "tx:";

    protected final UriBuilder uriBuilder;

    private final String basePath;

    private final int pathIx;

    private final URI context;

    private final Session session;

    private final String defaultWorkspace;

    private final Function<Repository, String> getDefaultWorkspace = new GetDefaultWorkspace();
    private final Class<?> relativeTo;
    private final UriInfo uris;
    private final boolean canonical;

    /**
     * Build HTTP graph subjects relative to the given JAX-RS resource, using the UriInfo provided.
     *
     * The session may provide additional information (e.g. workspaces) that need to be
     * taken into account.
     *
     * @param session
     * @param relativeTo
     * @param uris
     */
    public HttpIdentifierTranslator(final Session session, final Class<?> relativeTo, final UriInfo uris) {
        this(session, relativeTo, uris, false);
    }

    /**
     * Build HTTP graph subjects relative to the given JAX-RS resource, using the UriInfo provided.
     *
     * The session may provide additional information (e.g. workspaces) that need to be
     * taken into account.
     *
     * @param session
     * @param relativeTo
     * @param uris
     * @param canonical generate canonical URIs for resources
     */
    public HttpIdentifierTranslator(final Session session,
                                    final Class<?> relativeTo,
                                    final UriInfo uris,
                                    final boolean canonical) {
        this.session = session;
        this.relativeTo = relativeTo;
        this.uris = uris;
        this.canonical = canonical;
        this.context = uris.getRequestUri();
        this.uriBuilder = uris.getBaseUriBuilder().clone().path(relativeTo);
        String normalizedBasePath = uriBuilder.build("").toString();
        if (!normalizedBasePath.endsWith("/")) {
            normalizedBasePath = normalizedBasePath + "/";
        }
        this.basePath = normalizedBasePath;
        this.pathIx = normalizedBasePath.length() - 1;
        this.defaultWorkspace = getDefaultWorkspace.apply(session.getRepository());
        LOGGER.debug("Resolving graph subjects to a base URI of \"{}\"",
                normalizedBasePath);
        resetTranslationChain();
        for (final InternalIdentifierConverter converter : translationChain) {
            if (converter instanceof HierarchyConverter) {
                hierarchyLevels = converter.getLevels();
                break;
            }
        }
    }

    /**
     * Get the canonical IdentifierTranslator (e.g. one that ignores transactions and other transient states)
     * @param canonical
     * @return the canonical IdentifierTranslator
     */
    public HttpIdentifierTranslator getCanonical(final boolean canonical) {
        return new HttpIdentifierTranslator(session, relativeTo, uris, canonical);
    }

    /**
     * Is the current IdentifierTranslator canonical?
     * @return true if the current IdentifierTranslator is canonical
     */
    public boolean isCanonical() {
        return canonical || getCurrentTransactionId(session) == null;
    }

    @Override
    public Resource getSubject(final String absPath) throws RepositoryException {
        resetTranslationChain();
        LOGGER.debug("Creating RDF subject from identifier: {}", absPath);
        return doForward(absPath);
    }

    @Override
    public Resource getContext() {
        return createResource(context.toString());
    }

    private static String getResourceURI(final Resource subject) {
        if (!subject.isURIResource()) {
            LOGGER.debug("RDF resource {} was not a URI resource: returning null.", subject);
            return null;
        }
        return subject.getURI();
    }

    @Override
    public String getPathFromSubject(final Resource subject) throws RepositoryException {
        resetTranslationChain();
        return doBackward(subject);
    }

    /**
     * Gets a path from the graph subject's URI.  This method does the heavy
     * lifting for getNodeFromGraphSubject and getPathFromGraphSubject in a way
     * that's tied to a more generic URI rather than a rdf Resource.
     */
    protected String getPathFromGraphSubject(@NotNull final String subjectUri) throws RepositoryException {

        if (!isFedoraGraphSubject(subjectUri)) {
            LOGGER.debug("RDF resource {} was not a URI resource with our expected basePath {}, returning null.",
                    subjectUri, basePath);
            return null;
        }

        final StringBuilder pathBuilder = new StringBuilder();
        final String absPath;
        final String[] pathSegments =
                subjectUri.substring(pathIx).split("/");

        for (final String segment : pathSegments) {
            if (segment.startsWith(TX_PREFIX)) {
                if (!canonical) {
                    final String tx = segment.substring(TX_PREFIX.length());
                    final String currentTxId = getCurrentTransactionId(session);

                    if (currentTxId == null || !tx.equals(currentTxId)) {
                        throw new RepositoryException("Subject is not in this transaction");
                    }
                }
            } else if (segment.startsWith(WORKSPACE_PREFIX)) {
                final String workspace = segment.substring(WORKSPACE_PREFIX.length());
                if (!session.getWorkspace().getName().equals(workspace)) {
                    throw new RepositoryException("Subject is not in this workspace");
                }
            } else if (segment.equals(FCR_CONTENT)) {
                pathBuilder.append("/");
                pathBuilder.append(JCR_CONTENT);
            } else {
                if (!segment.isEmpty()) {
                    pathBuilder.append("/");
                    pathBuilder.append(segment);
                }
            }
        }

        if (pathBuilder.length() == 0) {
            absPath = "/";
        } else {
            absPath = pathBuilder.toString();
        }

        if (isValidJcrPath(absPath)) {
            return absPath;
        }
        return null;
    }

    private boolean isValidJcrPath(final String absPath) {
        try {
            String pathToValidate = absPath;
            final String txId = getCurrentTransactionId(session);
            if (!canonical && txId != null) {
                final String txIdWithSlash = "/" + TX_PREFIX + txId;
                /* replace the first occurrence of tx within the path */
                pathToValidate = replaceOnce(absPath, txIdWithSlash, EMPTY);
                LOGGER.debug("removed {} from URI {}. Path for JCR validation is now: {}", txIdWithSlash, absPath,
                        pathToValidate);
            }
            session.getValueFactory().createValue(pathToValidate, PATH);
            return true;
        } catch (final ValueFormatException e) {
            LOGGER.trace("Unable to validate JCR path: ", e);
            return false;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException("Unable to validate JCR path: ", e);
        }
    }

    @Override
    public boolean isFedoraGraphSubject(final Resource subject) {
        return subject.isURIResource() && isFedoraGraphSubject(subject.getURI());
    }

    private boolean isFedoraGraphSubject(final String subjectUri) {
        return subjectUri.startsWith(basePath) && isValidJcrPath(subjectUri.substring(pathIx));
    }

    private Map<String, String> getPathMap(final String absPath) {
        // the path param value doesn't start with a slash
        String path = absPath.startsWith("/") ? absPath.substring(1) : absPath;
        if (session != null) {
            final Workspace workspace = session.getWorkspace();

            final String txId = getCurrentTransactionId(session);

            if (!canonical && txId != null) {
                path = TX_PREFIX + txId + "/" + path;
            } else if (workspace != null && !workspace.getName().equals(defaultWorkspace)) {
                path = WORKSPACE_PREFIX + workspace.getName() + "/" + path;
            }
        }

        return singletonMap("path", path);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.identifiers.ExternalIdentifierConverter#doRdfForward(java.lang.String)
     */
    @Override
    protected Resource doRdfForward(final String inputId) {
        final URI result = uriBuilder.buildFromMap(getPathMap(inputId));
        return createResource(result.toString());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.identifiers.ExternalIdentifierConverter#doRdfBackward(com.hp.hpl.jena.rdf.model.Resource)
     */
    @Override
    protected String doRdfBackward(final Resource subject) {
        final String subjectUri = getResourceURI(subject);
        if (subjectUri == null) {
            return null;
        }
        try {
            return getPathFromGraphSubject(subjectUri);
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }


    protected void resetTranslationChain() {
        if (translationChain == null) {
            if (getTranslationChain() != null) {
                setTranslationChain(getTranslationChain());
            } else {
                setTranslationChain(minimalTranslationChain);
            }
        }
    }

    private static final List<InternalIdentifierConverter> minimalTranslationChain =
        singletonList((InternalIdentifierConverter) new NamespaceConverter());

    /**
     * Hierarchy levels. Default 0 for converters other than the HierarchyConverter.
     * @return
     */
    @Override
    public int getHierarchyLevels() {
        return hierarchyLevels;
    }

    @Override
    public String getSubjectPath(final Resource subject) {
        return subject.getURI().substring(pathIx);
    }
}
