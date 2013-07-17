package org.fcrepo.responses;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class RangeRequestInputStreamTest {
    @Test
    public void shouldLimitTheInputStream() throws IOException {
        InputStream in = new ByteArrayInputStream("0123456789".getBytes());
        final RangeRequestInputStream out = new RangeRequestInputStream(in, 5L, 3L);
        final String s = IOUtils.toString(out);
        assertEquals("567", s);
    }


    @Test
    public void shouldAcceptUnboundedRanges() throws IOException {
        InputStream in = new ByteArrayInputStream("0123456789".getBytes());
        final RangeRequestInputStream out = new RangeRequestInputStream(in, 0L, -1L);
        final String s = IOUtils.toString(out);
        assertEquals("0123456789", s);
    }
}
