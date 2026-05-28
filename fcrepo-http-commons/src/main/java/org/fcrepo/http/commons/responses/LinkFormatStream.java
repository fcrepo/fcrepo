/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;

import jakarta.ws.rs.core.Link;

/**
 * Stream of links for Memento TimeMaps
 *
 * @author whikloj
 * @since 2017-10-24
 */
public class LinkFormatStream implements AutoCloseable {

    private final Stream<Link> stream;

    /**
     * Constructor
     *
     * @param stream the stream of Links
     */
    public LinkFormatStream(final Stream<Link> stream) {
        requireNonNull(stream);
        this.stream = stream;
    }

    /**
     * Generic getter
     * 
     * @return the Stream of Links
     */
    public Stream<Link> getStream() {
        return stream;
    }

    @Override
    public void close() {
        stream.close();
    }

}
