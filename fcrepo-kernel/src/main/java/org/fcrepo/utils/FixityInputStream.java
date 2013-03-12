package org.fcrepo.utils;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.input.ProxyInputStream;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class FixityInputStream extends CountingInputStream {

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public FixityInputStream(InputStream in, MessageDigest digest) {
        super(new DigestInputStream(in, digest));
    }

    public MessageDigest getMessageDigest() {
        return ((DigestInputStream)in).getMessageDigest();
    }

}
