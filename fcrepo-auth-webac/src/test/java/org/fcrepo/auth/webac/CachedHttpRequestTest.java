/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link CachedHttpRequest}.
 *
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
public class CachedHttpRequestTest {

    private CachedHttpRequest cachedHttpRequest;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() throws IOException {
        final var sampleText = "This is some test data";
        final var stream =  new TestServletInputStream(sampleText.getBytes(StandardCharsets.UTF_8));
        when(request.getInputStream()).thenReturn(stream);
        cachedHttpRequest = new CachedHttpRequest(request);
    }

    @AfterEach
    public void tearDown() throws IOException {
        cachedHttpRequest.getInputStream().close();
    }

    /**
     * Always returns true.
     */
    @Test
    public void testIsReady() throws IOException {
        assertTrue(cachedHttpRequest.getInputStream().isReady());
    }

    /**
     * Always returns the not implemented exception.
     */
    @Test
    public void testSetReadListener() {
        assertThrows(RuntimeException.class,
                () -> cachedHttpRequest.getInputStream().setReadListener(new TestReadListener()));
    }

    @Test
    public void testIsFinished() throws IOException {
        final var stream = cachedHttpRequest.getInputStream();
        assertFalse(stream.isFinished());
        while (!stream.isFinished()) {
            stream.read();
        }
        assertTrue(stream.isFinished());
    }

    static class TestReadListener implements ReadListener {

        @Override
        public void onDataAvailable() throws IOException {
            // No implementation needed for this test
        }

        @Override
        public void onAllDataRead() throws IOException {
            // No implementation needed for this test
        }

        @Override
        public void onError(final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static class TestServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        public TestServletInputStream(final byte[] contents) {
            buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            // no-op
        }

        @Override
        public int read() throws IOException {
            return buffer.read();
        }
    }
}
