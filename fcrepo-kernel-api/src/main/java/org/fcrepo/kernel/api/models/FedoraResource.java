/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.api.models;

import java.net.URI;
import java.time.Instant;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.FedoraVersion;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;

import org.apache.jena.rdf.model.Model;

/**
 * @author ajs6f
 * @since Jan 10, 2014
 */
public interface FedoraResource {

    /**
     * Get the path to the resource
     * @return path
     */
    String getPath();

    /**
     * Get the children of this resource
     * @return a stream of Fedora resources
     */
    default Stream<FedoraResource> getChildren() {
        return getChildren(false);
    }

    /**
     * Get the children of this resource, possibly recursively
     * @param recursive whether to recursively fetch child resources
     * @return a stream of Fedora resources
     */
    Stream<FedoraResource> getChildren(Boolean recursive);

    /**
     * Get the container of this resource
     * @return the container of this resource
     */
    FedoraResource getContainer();

    /**
     * Get the Original Resource for which this resource is a memento. If this resource is not a memento,
     * then it is the original.
     *
     * @return the original resource for this
     */
    FedoraResource getOriginalResource();

    /**
     * Get the TimeMap/LDPCv of this resource
     *
     * @return the container for TimeMap/LDPCv of this resource
     */
    FedoraResource getTimeMap();

    /**
     * Create the TimeMap/LDPCv of this resource if it doesn't exist
     * @return the container for TimeMap/LDPCv of this resource
     */
    FedoraResource findOrCreateTimeMap();

    /**
     * Retrieve the mementoDatetime property and return it as an Instant
     *
     * @return the Instant for this resource
     */
    Instant getMementoDatetime();

    /**
     * Returns true if this resource is a Memento.
     *
     * @return true if the resource is a Memento.
     */
    boolean isMemento();

    /**
     * Retrieve the Memento with the closest datetime to the request.
     *
     * @param mementoDatetime The requested date time.
     * @return The closest Memento or null.
     */
    FedoraResource findMementoByDatetime(Instant mementoDatetime);

    /**
     * Get the ACL of this resource
     * @return the container for ACL of this resource
     */
    FedoraResource getAcl();

    /**
     * Create the ACL for this resource if it doesn't exist
     * @return the container for ACL of this resource
     */
    FedoraResource findOrCreateAcl();

    /**
     * Get the child of this resource at the given path
     *
     * @param relPath the given path
     * @return the child of this resource
     */
    FedoraResource getChild(String relPath);

    /**
     * Does this resource have a property
     * @param relPath the given path
     * @return the boolean value whether the resource has a property
     */
    boolean hasProperty(String relPath);

    /**
     * Delete this resource, and any inbound references to it
     */
    void delete();

    /**
     * Get the date this resource was created
     * @return created date
     */
    Instant getCreatedDate();

    /**
     * Get the date this resource was last modified
     * @return last modified date
     */
    Instant getLastModifiedDate();

    /**
     * Check if this object uses a given RDF type
     *
     * <p>Note: the type parameter should be in prefixed short form, so ldp:Container or ex:Image
     * are both acceptable types. This method does not assume any jcr to fedora prefix mappings are
     * managed by the implementation, so hasType("jcr:frozenNode") is a valid use of this method.</p>
     *
     * @param type the given type
     * @return whether the object has the given type
     */
    boolean hasType(final String type);

    /**
     * Get the RDF:type values for this resource
     * @return a list of types for this resource
     */
    List<URI> getTypes();

    /**
     * Add an RDF:type value to the resource
     *
     * <p>Note: the type parameter should be in prefixed short form, so ldp:Container or ex:Image
     * are both acceptable types. This method does not assume any jcr to fedora prefix mappings are
     * managed by the implementation, so hasType("jcr:frozenNode") is a valid use of this method.</p>
     *
     * @param type the type to add
     */
    void addType(final String type);

    /**
     * Update the provided properties with a SPARQL Update query. The updated
     * properties may be serialized to the persistence layer.
     *
     * @param idTranslator the property of idTranslator
     * @param sparqlUpdateStatement sparql update statement
     * @param originalTriples original triples
     * @throws MalformedRdfException if malformed rdf exception occurred
     * @throws AccessDeniedException if access denied in updating properties
     */
    void updateProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                          final String sparqlUpdateStatement,
                          final RdfStream originalTriples) throws MalformedRdfException, AccessDeniedException;

    /**
     * Return the RDF properties of this object using the provided context
     * @param idTranslator the property of idTranslator
     * @param context the context
     * @return the rdf properties of this object using the provided context
     */
    RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                         final TripleCategory context);

    /**
     * Return the RDF properties of this object using the provided contexts
     * @param idTranslator the property of idTranslator
     * @param contexts the provided contexts
     * @return the rdf properties of this object
     */
    RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                         final Set<? extends TripleCategory> contexts);

    /**
     * Get the base version for the node
     *
     * @return base version
     */
    FedoraResource getBaseVersion();

    /**
     * Get a stream of versions associated with this resource
     *
     * @return the versions associated with this resource
     */
    Stream<FedoraVersion> getVersions();

    /**
     * Check if a resource was created in this session
     * @return if resource created in this session
     */
    Boolean isNew();

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param idTranslator the given property of idTranslator
     * @param inputModel the input model
     * @param originalTriples the original triples
     * @throws MalformedRdfException if malformed rdf exception occurred
     */
    void replaceProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Model inputModel,
                                final RdfStream originalTriples) throws MalformedRdfException;

    /**
     * Construct an ETag value for the resource.
     *
     * @return constructed etag value
     */
    String getEtagValue();

    /**
     * Enable versioning
     */
    void enableVersioning();

    /**
     * Disable versioning
     */
    void disableVersioning();

    /**
     * Check if a resource is versioned
     * @return whether the resource is versioned
     */
    boolean isVersioned();

    /**
     * Check if a resource is frozen (a historic version).
     * @return whether the resource is frozen
     */
    boolean isFrozenResource();

    /**
     * When this is a frozen node, get the ancestor that was explicitly versioned
     * @return the ancestor that was explicity versioned
     */
    FedoraResource getVersionedAncestor();

    /**
     * Get the unfrozen equivalent of a frozen versioned node
     * @return the unfrozen equivalent of a frozen versioned node
     */
    FedoraResource getUnfrozenResource();

    /**
     * Get a resource version for this object with the provided label.
     * @param label the label
     * @return the node for this object at the version provided
     */
    FedoraResource getVersion(String label);

    /**
     * This method returns the version label of this frozen resource.
     * If this resource is not frozen, null is returned.
     * @return version label
     */
    String getVersionLabelOfFrozenResource();

    /**
     * Get the description for this resource
     * @return the description for this resource
     */
    FedoraResource getDescription();

    /**
     * Get the resource described by this resource
     * @return the resource being described
     */
    FedoraResource getDescribedResource();
}
