/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

/**
 * An extension of HttpServletRequestWrapper that caches the InputStream as
 * byte array and overrides the getInputStream to return a new InputStream
 * object each time based on the cached byte array.
 * 
 * @author mohideen
 */
public class CachedHttpRequest extends HttpServletRequestWrapper {

    private byte[] cachedContent;

    private BufferedReader reader;

    /**
     * Create a new CachedSparqlRequest for the given servlet request.
     * @param request the original servlet request
     */
    public CachedHttpRequest(final ServletRequest request) {
        super((HttpServletRequest) request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (getRequest().getInputStream() != null) {
            if (this.cachedContent == null) {
                this.cachedContent = IOUtils.toByteArray(getRequest().getInputStream());
            }
            return new CustomServletInputStream(cachedContent);
        }
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.reader == null) {
            this.reader = new BufferedReader(new InputStreamReader(getInputStream(),
                    getCharacterEncoding()));
        }
        return this.reader;
    }

    private static class CustomServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        public CustomServletInputStream(final byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(final ReadListener listener) {
            throw new RuntimeException("Not implemented");
        }
    }
}
