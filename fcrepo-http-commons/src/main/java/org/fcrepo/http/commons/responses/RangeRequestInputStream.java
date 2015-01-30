/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.commons.responses;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.ProxyInputStream;

/**
 * An InputStream wrapper that skips N bytes and only returns
 * the data up to a certain length
 *
 * @author awoods
 */
public class RangeRequestInputStream extends FilterInputStream {

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     * @param skip the number of bytes to skip at the beginning of the stream
     * @param length the number of bytes from the inputstream to read
     */
    public RangeRequestInputStream(final InputStream in,
                                   final long skip,
                                   final long length) throws IOException {
        super(new BoundedInputStream(new SkipInputStream(in, skip), length));
    }


    /**
     * An InputStream wrapper that skips bytes
     * @param in
     * @param skip
     * @throws IOException
     */
    private static class SkipInputStream extends ProxyInputStream {

        /**
         * An InputStream wrapper that always skips the first N bytes
         * @param in
         * @param skip
         * @throws IOException
         */
        public SkipInputStream(final InputStream in,
                               final long skip) throws IOException {
            super(in);
            IOUtils.skip(in, skip);
        }
    }
}
