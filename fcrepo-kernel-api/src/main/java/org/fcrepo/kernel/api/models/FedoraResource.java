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
import java.util.stream.Stream;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * A resource in a Fedora repository.
 *
 * @author ajs6f
 * @since Jan 10, 2014
 */
public interface FedoraResource {

    /**
     * Get the fedora identifier for this resource
     *
     * @return the fedora identifier
     */
    String getId();

    /**
     * Get the FedoraId for this resource.
     * @return the FedoraId identifier.
     */
    FedoraId getFedoraId();

    /**
     * Get the resource which contains this resource.
     *
     * @return the parent resource
     * @throws PathNotFoundException thrown if the parent cannot be found
     */
    FedoraResource getParent() throws PathNotFoundException;

    /**
     * Get the path to the resource
     *
     * @return path
     * @deprecated use getId()
     */
    @Deprecated
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
     * Get the Original Resource for which this resource is a memento or timemap for. If this resource is not a
     * memento or timemap, then it is the original.
     *
     * @return the original resource for this
     */
    FedoraResource getOriginalResource();

    /**
     * Get the TimeMap/LDPCv of this resource
     *
     * @return the container for TimeMap/LDPCv of this resource
     */
    TimeMap getTimeMap();

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
     * Returns true if this resource is an ACL.
     *
     * @return true if the resource is an ACL.
     */
    boolean isAcl();

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
     * Does this resource have a property
     * @param relPath the given path
     * @return the boolean value whether the resource has a property
     */
    boolean hasProperty(String relPath);

    /**
     * Get the date this resource was created
     * @return created date
     */
    Instant getCreatedDate();

    /**
     * Get the created by value
     *
     * @return created by
     */
    String getCreatedBy();

    /**
     * Get the date this resource was last modified
     * @return last modified date
     */
    Instant getLastModifiedDate();

    /**
     * Get the last modified by value
     * @return last modified by
     */
    String getLastModifiedBy();

    /**
     * Check if this object uses a given RDF type
     *
     * @param type the given type
     * @return whether the object has the given type
     */
    boolean hasType(final String type);

    /**
     * Get only the user provided types from their RDF.
     * @return a list of types from the user provided RDF.
     */
    List<URI> getUserTypes();

    /**
     * Get only the system defined types from their RDF.
     * @param forRdf whether we only want types for displaying in a RDF body.
     * @return a list of types from the user provided RDF.
     */
    List<URI> getSystemTypes(final boolean forRdf);

    /**
     * Get the RDF:type values for this resource, this is usually the combination of getUserTypes and
     * getSystemTypes(false) to get ALL the types.
     * @return a list of types for this resource
     */
    List<URI> getTypes();

    /**
     * Return the RDF properties for this resource.
     *
     * @return the RDF properties of this object.
     */
    RdfStream getTriples();

    /**
     * Check if a resource was created in this session
     *
     * @return if resource created in this session
     */
    @Deprecated
    Boolean isNew();

    /**
     * Construct an ETag value for the resource.
     *
     * @return constructed etag value
     */
    String getEtagValue();

    /**
     * Construct a State Token value for the resource.
     *
     * @return constructed state-token value
     */
    String getStateToken();

    /**
     * Check if a resource is an original resource
     * (ie versionable, as opposed to non-versionable resources
     * like mementos, timemaps, and acls).
     * @return whether the resource is an original resource.
     */
    boolean isOriginalResource();

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

    /**
     * Get the resource's interaction model.
     * @return the interaction model.
     */
    String getInteractionModel();
}
