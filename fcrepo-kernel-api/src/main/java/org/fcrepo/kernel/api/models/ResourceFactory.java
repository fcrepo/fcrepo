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

/**
 * Interface to a factory to locate or create new resources.
 *
 * @author whikloj
 * @since 2019-09-23
 */
public interface ResourceFactory {

    /**
     * Find or create a new container.
     *
     * @param identifier The path or identifier for the resource.
     * @param interactionModel The interaction model for the new/expected container.
     * @return The container object.
     */
    public Container findOrInitContainer(final String identifier, final String interactionModel);

    /**
     * Find or create a new binary. Newly created binaries will have nothing backing them until they are populated by a
     * service.
     *
     * @param identifier The path or identifier for the resource.
     * @return The binary object.
     */
    public FedoraBinary findOrInitBinary(final String identifier);

    /**
     * Find or create a binary description.
     *
     * @param identifier The path or identifier for the resource.
     * @return The description object.
     */
    public NonRdfSourceDescription findOrInitBinaryDescription(final String identifier);

    /**
     * Find or create a timemap.
     *
     * @param identifier The path or identifier for the resource.
     * @return The timemap object.
     */
    public FedoraTimeMap findOrInitTimemap(final String identifier);

    /**
     * Find or create a Webac ACL.
     *
     * @param identifier The path or identifier of the resource this ACL is linked to.
     * @return The ACL object.
     */
    public FedoraWebacAcl findOrInitAcl(final String identifier);

    /*
     * TODO: Do we need a model for an archival group?
     * public ArchivalGroup findOrInitArchivalGroup(final String identifier);
     */

}
