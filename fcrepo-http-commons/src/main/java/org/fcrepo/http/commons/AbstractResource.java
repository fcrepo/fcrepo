/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
    //protected UniqueValueSupplier defaultPidMinter = new ConfigurableHierarchicalSupplier(2, 4);

}
