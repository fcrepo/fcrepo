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
package org.fcrepo.kernel.rdf.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.IdentifierTranslator;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A very simple {@link IdentifierTranslator} which translates JCR paths into
 * un-dereference-able Fedora subjects (by replacing JCR-specific names with
 * Fedora names). Should not be used except in "embedded" deployments in which
 * no publication of translated identifiers is expected!
 *
 * @author barmintor
 * @author ajs6f
 * @since May 15, 2013
 */
public class DefaultIdentifierTranslator implements IdentifierTranslator {

    /**
     * Default namespace to use for node URIs
     */
    public static final String RESOURCE_NAMESPACE = "info:fedora/";

    private final Resource context;

    /**
     * Construct the graph with a placeholder context resource
     */
    public DefaultIdentifierTranslator() {
        this.context = createResource();
    }

    @Override
    public Resource getSubject(final String absPath) throws RepositoryException {
        if (absPath.endsWith(JCR_CONTENT)) {
            return createResource(RESOURCE_NAMESPACE + absPath.replace(JCR_CONTENT, FCR_CONTENT).substring(1));
        }
        return createResource(RESOURCE_NAMESPACE + absPath.substring(1));
    }

    @Override
    public Resource getContext() {
        return context;
    }

    @Override
    public String getPathFromSubject(final Resource subject) throws RepositoryException {
        if (!isFedoraGraphSubject(subject)) {
            return null;
        }

        final String absPath = subject.getURI().substring(RESOURCE_NAMESPACE.length() - 1);

        if (absPath.endsWith(FCR_CONTENT)) {
            return absPath.replace(FCR_CONTENT, JCR_CONTENT);
        }
        return absPath;
    }

    @Override
    public boolean isFedoraGraphSubject(final Resource subject) {
        checkNotNull(subject, "null cannot be a Fedora object!");
        return subject.isURIResource() && subject.getURI().startsWith(RESOURCE_NAMESPACE);
    }

    @Override
    public int getHierarchyLevels() {
        return 0;
    }

    @Override
    public String getSubjectPath(final Resource subject) {
        return subject.getURI().substring(RESOURCE_NAMESPACE.length() - 1);
    }

}
