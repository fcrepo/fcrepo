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

import static com.google.common.base.Preconditions.checkArgument;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrConstants;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Translates JCR names into "fedora" subjects (by replacing jcr-specific names
 * with fedora names)
 * @author barmintor
 * @date May 15, 2013
 */
public class DefaultGraphSubjects implements GraphSubjects {

    private final Resource context;
    private final Session session;

    /**
     * Construct the graph with a placeholder context resource
     */
    public DefaultGraphSubjects(final Session session) {
        this.session = session;
        this.context = ResourceFactory.createResource();
    }

    @Override
    public Resource getGraphSubject(final String absPath) throws RepositoryException {
        if (absPath.endsWith(JcrConstants.JCR_CONTENT)) {
            return ResourceFactory
                           .createResource("info:fedora" +
                                                   absPath.replace(JcrConstants.JCR_CONTENT,
                                                                          FedoraJcrTypes.FCR_CONTENT));
        } else {
            return ResourceFactory.createResource("info:fedora" + absPath);
        }
    }

    @Override
    public Resource getContext() {
        return context;
    }

    @Override
    public Resource getGraphSubject(final Node node) throws RepositoryException {
        return getGraphSubject(node.getPath());
    }

    @Override
    public Node getNodeFromGraphSubject(final Resource subject)
        throws RepositoryException {
        if (!isFedoraGraphSubject(subject)) {
            return null;
        }

        final String absPath =
            subject.getURI().substring("info:fedora".length());

        if (absPath.endsWith(FCR_CONTENT)) {
            return session.getNode(absPath.replace(FedoraJcrTypes.FCR_CONTENT,
                                                   JcrConstants.JCR_CONTENT));
        } else if (session.nodeExists(absPath)) {
            return session.getNode(absPath);
        } else {
            return null;
        }
    }

    @Override
    public boolean isFedoraGraphSubject(final Resource subject) {
        checkArgument(subject != null, "null cannot be a Fedora object!");
        assert(subject != null);

        return subject.isURIResource() &&
            subject.getURI().startsWith("info:fedora/");
    }

}
