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
package org.fcrepo.http.commons;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.api.services.functions.ConfigurableHierarchicalSupplier;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;

import org.jvnet.hk2.annotations.Optional;

/**
 * Superclass for Fedora JAX-RS Resources, providing convenience fields and methods.
 *
 * @author ajs6f
 */
public class AbstractResource {

    @Inject
    protected FedoraPropsConfig fedoraPropsConfig;

    /**
     * Useful for constructing URLs
     */
    @Context
    protected UriInfo uriInfo;

    /**
     * For getting user agent
     */
    @Context
    protected HttpHeaders headers;

    @Inject
    protected ResourceFactory resourceFactory;

    /**
     * The version service
     */
    @Inject
    protected VersionService versionService;

    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Inject
    @Optional
    protected Supplier<String> pidMinter;

    // Mint non-hierarchical identifiers. To force pairtree creation as default, use
    //  ConfigurableHierarchicalSupplier(int length, count) instead.
    protected UniqueValueSupplier defaultPidMinter = new ConfigurableHierarchicalSupplier();

}
