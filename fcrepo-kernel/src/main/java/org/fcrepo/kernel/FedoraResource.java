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

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author ajs6f
 * @since Jan 10, 2014
 */
public interface FedoraResource {

    /**
     * Does the resource have a jcr:content child node?
     * @return has content
     */
    boolean hasContent();

    /**
     * @return The JCR node that backs this object.
     */
    Node getNode();

    /**
     * Get the path to the JCR node
     * @return path
     */
    String getPath();

    /**
     * Get the date this datastream was created
     * @return created date
     */
    Date getCreatedDate();

    /**
     * Get the date this datastream was last modified
     * @return last modified date
     */
    Date getLastModifiedDate();

    /**
     * Check if this object uses a given mixin
     * @return a collection of mixin names
     */
    boolean hasType(final String type);
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
     * Return the JCR properties of this object as an {@link RdfStream}
     * @param graphSubjects
     * @return triples
     */
    RdfStream getPropertiesTriples(final IdentifierTranslator graphSubjects);

    /**
     * Return the JCR properties of this object as an {@link RdfStream}
     * @param graphSubjects
     * @return triples
     */
    RdfStream getHierarchyTriples(final IdentifierTranslator graphSubjects,
                                  final HierarchyRdfContextOptions serializationOptions);

    /**
     * Serialize the JCR versions information as an RDF dataset
     * @param graphSubjects
     * @return triples
     */
    RdfStream getVersionTriples(final IdentifierTranslator graphSubjects);

    /**
     * Serialize inbound References to this object as an {@link RdfStream}
     * @param graphSubjects
     * @return triples
     */
    RdfStream getReferencesTriples(final IdentifierTranslator graphSubjects);

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
     * Check if a resource was created in this session
     * @return if resource created in this session
     */
    Boolean isNew();

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param graphSubjects
     * @param inputModel
     * @return RDFStream
     */
    RdfStream replaceProperties(final IdentifierTranslator graphSubjects, final Model inputModel);

    /**
     * Construct an ETag value from the last modified date and path. JCR has a
     * mix:etag type, but it only takes into account binary properties. We
     * actually want whole-object etag data. TODO : construct and store an ETag
     * value on object modify
     *
     * @return constructed etag value
     */
    String getEtagValue();

}