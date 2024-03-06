/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * <p>RangeRequestInputStreamTest class.</p>
 *
 * @author awoods
 */
public class RangeRequestInputStreamTest {
    @Test
    public void shouldLimitTheInputStream() throws IOException {
        final InputStream in = new ByteArrayInputStream("0123456789".getBytes());
        try (final RangeRequestInputStream out = new RangeRequestInputStream(in, 5L, 3L)) {
            final String s = IOUtils.toString(out, UTF_8);
            assertEquals("567", s);
        }
    }


    @Test
    public void shouldAcceptUnboundedRanges() throws IOException {
        final InputStream in = new ByteArrayInputStream("0123456789".getBytes());
        try (final RangeRequestInputStream out = new RangeRequestInputStream(in, 0L, -1L)) {
            final String s = IOUtils.toString(out, UTF_8);
            assertEquals("0123456789", s);
        }
    }

    @Test
    public void getGetLongRange() throws IOException {
        final StringBuilder buf = new StringBuilder();
        while ( buf.length() < 9000 ) {
            buf.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        }
        final InputStream in = new ByteArrayInputStream(buf.toString().getBytes());
        try (final RangeRequestInputStream out = new RangeRequestInputStream(in, 0L, 9000)) {
            assertEquals(9000, IOUtils.toString(out, UTF_8).length());
        }
    }
}
