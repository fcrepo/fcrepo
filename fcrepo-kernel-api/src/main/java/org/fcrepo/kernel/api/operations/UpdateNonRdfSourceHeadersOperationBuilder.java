/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.apache.jena.rdf.model.Model;

/**
 * Builder for operations for updating non-RDF source resource headers
 *
 * @author bbpennel
 */
public interface UpdateNonRdfSourceHeadersOperationBuilder extends RelaxableResourceOperationBuilder,
        NonRdfSourceOperationBuilder {
    @Override
    UpdateNonRdfSourceHeadersOperationBuilder relaxedProperties(Model model);

    @Override
    UpdateNonRdfSourceHeadersOperationBuilder mimeType(String mimetype);

    @Override
    UpdateNonRdfSourceHeadersOperationBuilder filename(String filename);

    @Override
    UpdateNonRdfSourceHeadersOperation build();
}
