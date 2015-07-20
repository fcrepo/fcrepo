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
package org.fcrepo.kernel.modeshape.utils;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.apache.commons.io.input.CountingInputStream;

/**
 * An InputStream wrapper that calculates the size and digest
 * while reading from the stream.
 * @author Chris Beer
 * @since Mar 12, 2013
 */
public class FixityInputStream extends CountingInputStream {

    /**
     * Creates a <code>FilterInputStream</code> by assigning the
     * argument <code>in</code> to the field <code>this.in</code>
     * so as to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     * @param digest the given digest
     */
    public FixityInputStream(final InputStream in, final MessageDigest digest) {
        super(new DigestInputStream(in, digest));
    }

    /**
     * Retrieve the calculated digest for the input stream
     * @return digest for this input stream
     */
    public MessageDigest getMessageDigest() {
        return ((DigestInputStream) in).getMessageDigest();
    }

}
