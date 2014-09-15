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
package org.fcrepo.kernel.services;

import java.io.File;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.Problems;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface RepositoryService extends Service {

    /**
     * Calculate the total size of all the binary properties in the repository
     *
     * @return size in bytes
     */
    Long getRepositorySize();

    /**
     * Calculate the number of objects in the repository
     *
     * @return number of objects in the repository
     */
    Long getRepositoryObjectCount();

    /**
     * Get a map of JCR prefixes to their URI namespaces
     *
     * @param session
     * @return map of JCR prefixes to their URI namespaces
     * @throws RepositoryException
     */
    Map<String, String> getRepositoryNamespaces(final Session session) throws RepositoryException;

    /**
     * Serialize the JCR namespace information as an RDF Dataset
     *
     * @param session
     * @return JCR namespace information as an RDF Dataset
     * @throws RepositoryException
     */
    Dataset getNamespaceRegistryDataset(final Session session, final IdentifierTranslator idTranslator);

    /**
     * Serialize the JCR namespace information as an {@link RdfStream}
     *
     * @param session
     * @return JCR namespace information as an {@link RdfStream}
     * @throws RepositoryException
     */
    RdfStream getNamespaceRegistryStream(final Session session, final IdentifierTranslator idTranslator);

    /**
     * Perform a full-text search on the whole repository and return the
     * information as an RDF Dataset
     *
     * @param subjectFactory
     * @param searchSubject RDF resource to use as the subject of the search
     * @param session
     * @param terms
     * @param limit
     * @param offset
     * @return full text search results as an RDF Dataset
     * @throws RepositoryException
     */
    Dataset searchRepository(IdentifierTranslator subjectFactory, Resource searchSubject, Session session, String terms,
            int limit, long offset);

    /**
     * This method backups up a running repository
     *
     * @param session
     * @param backupDirectory
     * @return problems
     * @throws RepositoryException
     */
    Problems backupRepository(Session session, File backupDirectory);

    /**
     * This methods restores the repository from a backup
     *
     * @param session
     * @param backupDirectory
     * @return problems
     * @throws RepositoryException
     */
    Problems restoreRepository(Session session, File backupDirectory);

}