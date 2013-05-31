/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date to return, with
 */
public class FixityInputStreamTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void SimpleFixityInputStreamTest() throws NoSuchAlgorithmException {
        final FixityInputStream is =
                new FixityInputStream(new ByteArrayInputStream("0123456789"
                        .getBytes()), MessageDigest.getInstance("SHA-1"));

        try {
            while (is.read() != -1) {
                ;
            }
        } catch (final IOException e) {

        }

        assertEquals(10, is.getByteCount());
        assertEquals("87acec17cd9dcd20a716cc2cf67417b71c8a7016", Hex
                .encodeHexString(is.getMessageDigest().digest()));
    }
}
