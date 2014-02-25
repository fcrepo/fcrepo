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
package org.fcrepo.kernel.rdf.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Translates JCR names into "fedora" subjects (by replacing jcr-specific names
 * with fedora names)
 * @author barmintor
 * @date May 15, 2013
 */
public class DefaultGraphSubjects implements GraphSubjects {


    /**
     * Default namespace to use for node URIs
     */
    public static final String RESOURCE_NAMESPACE = "info:fedora/";

    private final Resource context;
    private final Session session;

    private static final Logger LOGGER = getLogger(DefaultGraphSubjects.class);


    /**
     * Construct the graph with a placeholder context resource
     */
    public DefaultGraphSubjects(final Session session) {
        this.session = session;
        this.context = createResource();
    }

    @Override
    public Resource getGraphSubject(final String absPath) throws RepositoryException {
        if (absPath.endsWith(JCR_CONTENT)) {
            return createResource(RESOURCE_NAMESPACE
                    + absPath.replace(JCR_CONTENT, FCR_CONTENT).substring(1));
        }
        return createResource(RESOURCE_NAMESPACE + absPath.substring(1));
    }

    @Override
    public Resource getContext() {
        return context;
    }

    @Override
    public Resource getGraphSubject(final Node node) throws RepositoryException {
        LOGGER.trace("Returning RDF subject for: {}", node);
        return getGraphSubject(node.getPath());
    }

    @Override
    public Node getNodeFromGraphSubject(final Resource subject)
            throws RepositoryException {

        final String absPath = getPathFromGraphSubject(subject);

        if (absPath == null) {
            return null;
        }

        if (session.nodeExists(absPath)) {
            return session.getNode(absPath);
        }
        return null;
    }

    @Override
    public String getPathFromGraphSubject(final Resource subject) throws RepositoryException {
        if (!isFedoraGraphSubject(subject)) {
            return null;
        }

        final String absPath = subject.getURI()
                                   .substring(RESOURCE_NAMESPACE.length() - 1);

        if (absPath.endsWith(FCR_CONTENT)) {
            return absPath.replace(FCR_CONTENT, JCR_CONTENT);
        }
        return absPath;
    }

    @Override
    public boolean isFedoraGraphSubject(final Resource subject) {
        checkNotNull(subject, "null cannot be a Fedora object!");
        return subject.isURIResource() &&
            subject.getURI().startsWith(RESOURCE_NAMESPACE);
    }

}
