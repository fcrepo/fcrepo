/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.auth.webac;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

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
