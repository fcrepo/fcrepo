/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.BoundedInputStream;

/**
 * An {@link InputStream} that skips bytes and only returns the data up to a certain limit
 *
 * @author awoods
 * @author ajs6f
 */
public class RangeRequestInputStream extends BoundedInputStream {

    /**
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     * @param skip the number of bytes to skip at the beginning of the stream
     * @param length the number of bytes from the inputstream to read
     * @throws IOException if IO exception occurred
     */
    public RangeRequestInputStream(final InputStream in, final long skip, final long length) throws IOException {
        super(in, length);
        in.skip(skip);
    }
}
