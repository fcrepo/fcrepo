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

package org.fcrepo.kernel.resource;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import javax.jcr.RepositoryException;

/**
 * @author cabeer
 * @since 9/13/14
 */
public interface RdfSource<T> extends Resource<T> {

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param graphSubjects
     * @param inputModel
     * @return RDFStream
     * @throws javax.jcr.RepositoryException
     */
    RdfStream replaceProperties(final IdentifierTranslator graphSubjects,
                                final Model inputModel) throws RepositoryException;
    /**
     * Update the properties Dataset with a SPARQL Update query. The updated
     * properties may be serialized to the JCR store.
     *
     * After applying the statement, clients SHOULD check the result
     * of #getDatasetProblems, which may include problems when attempting to
     * serialize the data to JCR.
     *
     * @param subjects
     * @param sparqlUpdateStatement
     * @throws RepositoryException
     */
    Dataset updatePropertiesDataset(final IdentifierTranslator subjects,
                                    final String sparqlUpdateStatement) throws RepositoryException;

    /**
     * Return the JCR properties of this object as a Jena {@link Dataset}
     *
     * @param graphSubjects
     * @param offset
     * @param limit
     * @return properties
     * @throws RepositoryException
     */
    Dataset getPropertiesDataset(final IdentifierTranslator graphSubjects,
                                 final int offset, final int limit);

    /**
     * Return the JCR properties of this object as a Jena {@link Dataset}
     * @param subjects
     * @return properties
     * @throws RepositoryException
     */
    Dataset getPropertiesDataset(final IdentifierTranslator subjects)
            throws RepositoryException;

    /**
     * Return the JCR properties of this object as an {@link RdfStream}
     * @param graphSubjects
     * @return triples
     * @throws RepositoryException
     */
    RdfStream getTriples(final IdentifierTranslator graphSubjects);

    /**
     * Return the JCR properties of this object as an {@link RdfStream}
     * @param graphSubjects
     * @return triples
     * @throws RepositoryException
     */
    RdfStream getHierarchyTriples(final IdentifierTranslator graphSubjects,
                                  final HierarchyRdfContextOptions serializationOptions);

    /**
     * Serialize the JCR versions information as an RDF dataset
     * @param graphSubjects
     * @return triples
     * @throws RepositoryException
     */
    RdfStream getVersionTriples(final IdentifierTranslator graphSubjects)
            throws RepositoryException;

    /**
     * Serialize inbound References to this object as an {@link RdfStream}
     * @param graphSubjects
     * @return triples
     * @throws RepositoryException
     */
    RdfStream getReferencesTriples(final IdentifierTranslator graphSubjects);

}
