/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.apache.commons.io.input.CountingInputStream;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 12, 2013
 */
public class FixityInputStream extends CountingInputStream {

    /**
     * Creates a <code>FilterInputStream</code> by assigning the
     * argument <code>in</code> to the field <code>this.in</code>
     * so as to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public FixityInputStream(final InputStream in, final MessageDigest digest) {
        super(new DigestInputStream(in, digest));
    }

    /**
     * @todo Add Documentation.
     */
    public MessageDigest getMessageDigest() {
        return ((DigestInputStream) in).getMessageDigest();
    }

}
