package org.fcrepo.utils;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static junit.framework.Assert.assertEquals;

public class FixityInputStreamTest {

    @Test
    public void SimpleFixityInputStreamTest() throws NoSuchAlgorithmException {
        FixityInputStream is = new FixityInputStream(new ByteArrayInputStream("0123456789".getBytes()), MessageDigest.getInstance("SHA-1"));

        try {
            while(is.read() != -1);
        } catch (IOException e) {

        }

        assertEquals(10, is.getByteCount());
        assertEquals("87acec17cd9dcd20a716cc2cf67417b71c8a7016", Hex.encodeHexString(is.getMessageDigest().digest()));
    }
}
