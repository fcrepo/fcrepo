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
package org.fcrepo.kernel.rdf;

import javax.jcr.RepositoryException;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Translate internal JCR node identifiers to external Fedora identifiers
 * (and vice versa)
 * @author barmintor
 * @author ajs6f
 * @since May 15, 2013
 */
public interface IdentifierTranslator {

    /**
     * Translate an RDF resource into a JCR path
     * @param subject
     * @return path
     * @throws RepositoryException
     */
    String getPathFromSubject(final Resource subject) throws RepositoryException;

    /**
     * Predicate for determining whether this {@link Resource} is a Fedora object.
     * @param subject
     * @return boolean
     */
    boolean isFedoraGraphSubject(final Resource subject);

    /**
     * Get the RDF resource for an absolute path
     *
     * @param absPath the absolute path to the JCR node
     * @return an RDF URI resource
     * @throws RepositoryException
     */
    Resource getSubject(final String absPath) throws RepositoryException;

    /**
     * Get a context resource
     * @return Resource
     */
    Resource getContext();

    /**
     * Get the hierarchy levels for translation
     * @return
     */
    int getHierarchyLevels();

    /**
     * Reverse to get the transparent path
     * @return
     */
    String getSubjectPath(final Resource subject);
}
