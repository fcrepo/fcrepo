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

package org.fcrepo;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.WebContent;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.api.rdf.HttpTripleUtil;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;
import com.hp.hpl.jena.query.Dataset;

/**
 * Abstract superclass for Fedora JAX-RS Resources, providing convenience fields
 * and methods.
 *
 * @author ajs6f
 */
public abstract class AbstractResource {

    private static final Logger LOGGER = getLogger(AbstractResource.class);

    /**
     * Useful for constructing URLs
     */
    @Context
    protected UriInfo uriInfo;

    @Autowired
    protected SessionFactory sessions;

    /**
     * The fcrepo node service
     */
    @Autowired
    protected NodeService nodeService;

    /**
     * The fcrepo object service
     */
    @Autowired
    protected ObjectService objectService;

    /**
     * The fcrepo datastream service
     */
    @Autowired
    protected DatastreamService datastreamService;

    @Autowired(required = false)
    private HttpTripleUtil httpTripleUtil;

    @Autowired(required = false)
    protected EventBus eventBus;

    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Autowired
    protected PidMinter pidMinter;

    @Context
    private HttpServletRequest servletRequest;

    @Context
    private SecurityContext securityContext;

    /**
     * A convenience object provided by ModeShape for acting against the JCR
     * repository.
     */
    protected static final JcrTools jcrTools = new JcrTools(true);

    @PostConstruct
    public void initialize() throws RepositoryException {

    }

    /**
     * Convert a JAX-RS list of PathSegments to a JCR path
     * 
     * @param paths
     * @return
     */
    public static final String toPath(final List<PathSegment> paths) {
        final StringBuffer result = new StringBuffer();
        LOGGER.trace("converting URI path to JCR path: {}", paths);

        int i = 0;

        for (final PathSegment path : paths) {
            final String p = path.getPath();

            if (p.equals("")) {
                LOGGER.trace("Ignoring empty segment {}", p);
            } else if (i == 0 &&
                    (p.startsWith("tx:") || p.startsWith("workspace:"))) {
                LOGGER.trace("Ignoring internal segment {}", p);
                i++;
            } else {

                LOGGER.trace("Adding segment {}", p);

                if (!p.startsWith("[")) {
                    result.append('/');
                }
                result.append(p);
                i++;
            }
        }

        final String path = result.toString();

        if (path.isEmpty()) {
            return "/";
        } else {
            return path;
        }
    }

    protected FedoraResource createObjectOrDatastreamFromRequestContent(
            final Class<?> pathsRelativeTo, final Session session,
            final String path, final String mixin, final UriInfo uriInfo,
            final InputStream requestBodyStream,
            final MediaType requestContentType, final String checksumType,
            final String checksum) throws RepositoryException,
        InvalidChecksumException, IOException {

        final FedoraResource result;

        switch (mixin) {
            case FedoraJcrTypes.FEDORA_OBJECT:
                result = objectService.createObject(session, path);

                if (requestBodyStream != null &&
                        requestContentType != null &&
                        requestContentType.toString().equals(
                                WebContent.contentTypeSPARQLUpdate)) {
                    result.updatePropertiesDataset(new HttpGraphSubjects(
                            pathsRelativeTo, uriInfo), IOUtils
                            .toString(requestBodyStream));
                }

                break;
            case FedoraJcrTypes.FEDORA_DATASTREAM:
                final MediaType contentType =
                        requestContentType != null ? requestContentType
                                : APPLICATION_OCTET_STREAM_TYPE;

                final Node node =
                        datastreamService.createDatastreamNode(session, path,
                                contentType.toString(), requestBodyStream,
                                checksumType, checksum);
                result = new Datastream(node);
                break;
            default:
                result = null;
                break;
        }

        return result;
    }

    protected void addResponseInformationToDataset(
            final FedoraResource resource, final Dataset dataset,
            final UriInfo uriInfo, final GraphSubjects subjects)
        throws RepositoryException {
        if (httpTripleUtil != null) {
            httpTripleUtil.addHttpComponentModelsForResource(dataset, resource,
                    uriInfo, subjects);
        }
    }
}
