/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.http.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream implementation for testing range queries.
**/
public class RangeTestInputStream extends InputStream {

    private final long skip;
    private final byte[] data;
    private long bytesRead = 0L;

    public RangeTestInputStream(long skip, byte[] data) {
        super();
        this.skip = skip;
        this.data = data;
    }

    @Override
    public int read() throws IOException {
        bytesRead++;
        final long pos = bytesRead - (skip + 1);
        if ( pos < 0 ) {
            return 47;
        } else if ( pos < data.length ) {
            return (int) data[ (int)pos ];
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        int i = 0;
        for ( ; i < b.length; ++i) {
            b[i] = (byte) read();
        }
        return i;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i = off;
        for ( ; i < len; ++i ) {
            b[i] = (byte) read();
        }
        return i;
    }
}
