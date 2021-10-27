/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.apache.jena.rdf.model.Model;

/**
 * Builder for operations involving resource with relaxable server managed properties
 *
 * @author bbpennel
 */
public interface RelaxableResourceOperationBuilder extends ResourceOperationBuilder {
    /**
     * Set the relaxed managed properties for this resource if the server
     * is in relaxed mode.
     *
     * @param model rdf of the resource
     * @return this builder
     */
    RelaxableResourceOperationBuilder relaxedProperties(Model model);
}
