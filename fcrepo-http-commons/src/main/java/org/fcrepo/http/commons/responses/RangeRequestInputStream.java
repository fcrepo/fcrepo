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
