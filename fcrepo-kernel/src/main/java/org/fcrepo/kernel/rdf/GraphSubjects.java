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
package org.fcrepo.kernel.rdf;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Translate internal JCR node identifiers to external Fedora identifiers
 * (and vice versa)
 * @author barmintor
 * @date May 15, 2013
 */
public interface GraphSubjects {
    /**
     * Translate a JCR node into an RDF Resource
     * @param node
     * @return an RDF URI resource
     * @throws RepositoryException
     */
    Resource getGraphSubject(final Node node) throws RepositoryException;

    /**
     * Translate an RDF resource into a JCR node
     * @param subject an RDF URI resource
     * @return a JCR node, or null if one couldn't be found
     * @throws RepositoryException
     */
    Node getNodeFromGraphSubject(final Resource subject)
        throws RepositoryException;

    /**
     * Translate an RDF resource into a JCR path
     * @param subject
     * @return
     * @throws RepositoryException
     */
    String getPathFromGraphSubject(final Resource subject) throws RepositoryException;

    /**
     * Predicate for determining whether this {@link Resource} is a Fedora object.
     * @param subject
     * @return
     */
    boolean isFedoraGraphSubject(final Resource subject);

    /**
     * Get the RDF resource for an absolute path
     *
     * @param absPath the absolute path to the JCR node
     * @return an RDF URI resource
     * @throws RepositoryException
     */
    Resource getGraphSubject(final String absPath) throws RepositoryException;

    /**
     * Get a context resource
     * @return
     */
    Resource getContext();
}
