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
package org.fcrepo.kernel;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.resources.Resource;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import java.util.Iterator;

/**
 * @author ajs6f
 * @since Jan 10, 2014
 */
public interface FedoraResource extends Resource<Node> {

    /**
     * Get the children of this resource
     * @return iterator
     */
    Iterator<FedoraResource> getChildren();

    /**
     * Does the resource have a jcr:content child node?
     * @return has content
     */
    boolean hasContent();

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
     */
    Dataset updatePropertiesDataset(final IdentifierTranslator subjects,
            final String sparqlUpdateStatement);

    /**
     * Return the JCR properties of this object as a Jena {@link Dataset}
     *
     * @param graphSubjects
     * @param offset
     * @param limit
     * @return properties
     */
    Dataset getPropertiesDataset(final IdentifierTranslator graphSubjects,
       final int offset, final int limit);

    /**
     * Return the JCR properties of this object as a Jena {@link Dataset}
     * @param subjects
     * @return properties
     */
    Dataset getPropertiesDataset(final IdentifierTranslator subjects);

    /**
     * Tag the current version of the Node with a version label that
     * can be retrieved by name later.
     *
     * @param label
     */
    void addVersionLabel(final String label);

    /**
     * Get the JCR Base version for the node
     * 
     * @return base version
     */
    public Version getBaseVersion();

    /**
     * Get JCR VersionHistory for the node.
     *
     * @return version history
     */
    public VersionHistory getVersionHistory();

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param graphSubjects
     * @param inputModel
     * @return RDFStream
     */
    RdfStream replaceProperties(final IdentifierTranslator graphSubjects,
                                final Model inputModel,
                                final RdfStream originalTriples);


}