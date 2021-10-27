/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE_HEADERS;

/**
 * Operation for updating non-RDF source resource headers
 *
 * @author bbpennel
 */
public interface UpdateNonRdfSourceHeadersOperation extends RelaxableResourceOperation, NonRdfSourceOperation {
    @Override
    public default ResourceOperationType getType() {
        return UPDATE_HEADERS;
    }
}
