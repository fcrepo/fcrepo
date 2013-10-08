/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

public class FixityInputStreamTest {

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
