/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.fcrepo.kernel.api.RdfStream;

/**
 * A simple type to collect an RdfStream and associated Namespace mappings
 *
 * @author acoburn
 * @since 2/13/16
 */
public class RdfNamespacedStream implements AutoCloseable {

    public final RdfStream stream;

    public final Map<String, String> namespaces;

    /**
     * Creates an object to hold an RdfStream and an associated namespace mapping.
     *
     * @param stream the RdfStream
     * @param namespaces the namespace mapping
     */
    public RdfNamespacedStream(final RdfStream stream, final Map<String, String> namespaces) {
        requireNonNull(stream);
        requireNonNull(namespaces);
        this.stream = stream;
        this.namespaces = namespaces;
    }

    @Override
    public void close() {
        stream.close();
    }
}
