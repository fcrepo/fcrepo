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
package org.fcrepo.http.commons;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.RepositoryService;
import org.fcrepo.kernel.services.VersionService;
import org.jvnet.hk2.annotations.Optional;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;

/**
 * Abstract superclass for Fedora JAX-RS Resources, providing convenience fields
 * and methods.
 *
 * @author ajs6f
 */
public abstract class AbstractResource {

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
    protected BinaryService binaryService;

    /**
     * The fcrepo version service
     */
    @Autowired
    protected VersionService versionService;

    /**
     * The fcrepo repository service
     */
    @Autowired
    protected RepositoryService repositoryService;

    @Inject
    @Optional
    protected EventBus eventBus;

    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Autowired
    protected PidMinter pidMinter;

    /**
     * Convert a JAX-RS list of PathSegments to a JCR path
     *
     * @param translator
     * @param originalPath
     * @return String jcr path
     */
    public static final String toPath(final IdentifierConverter<Resource, Node> translator,
                                      final String originalPath) {

        final Resource resource = translator.toDomain(originalPath);

        final String path = translator.asString(resource);

        if (path.isEmpty()) {
            return "/";
        } else {
            return path;
        }
    }

}
