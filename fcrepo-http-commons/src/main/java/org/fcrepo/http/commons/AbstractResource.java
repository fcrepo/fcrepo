/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.http.commons;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ContainerService;
import org.fcrepo.kernel.services.VersionService;

import org.jvnet.hk2.annotations.Optional;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.eventbus.EventBus;

/**
 * Superclass for Fedora JAX-RS Resources, providing convenience fields and methods.
 *
 * @author ajs6f
 */
public class AbstractResource {

    static {
        // the SLF4J to JUL bridge normally adds its attachments
        // we want them to _replace_ the JUL loggers, to avoid logging outputs except those controlled by SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /**
     * Useful for constructing URLs
     */
    @Context
    protected UriInfo uriInfo;

    /**
     * For getting user agent
     */
    @Context
    protected HttpHeaders headers;

    @Inject
    protected SessionFactory sessions;

    /**
     * The JCR node service
     */
    @Inject
    protected NodeService nodeService;

    /**
     * The repository object service
     */
    @Inject
    protected ContainerService containerService;

    /**
     * The bitstream service
     */
    @Inject
    protected BinaryService binaryService;

    /**
     * The version service
     */
    @Inject
    protected VersionService versionService;

    @Inject
    @Optional
    protected EventBus eventBus;

    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Inject
    protected Supplier<String> pidMinter;

    /**
     * Convert a JAX-RS list of PathSegments to a JCR path
     *
     * @param idTranslator the id translator
     * @param originalPath the original path
     * @return String jcr path
     */
    public static final String toPath(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                      final String originalPath) {

        final Resource resource = idTranslator.toDomain(originalPath);

        final String path = idTranslator.asString(resource);

        return path.isEmpty() ? "/" : path;
    }

}
