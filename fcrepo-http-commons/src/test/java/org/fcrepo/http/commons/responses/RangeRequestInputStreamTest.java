/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.http.commons.responses;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

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
            final String s = IOUtils.toString(out);
            assertEquals("567", s);
        }
    }


    @Test
    public void shouldAcceptUnboundedRanges() throws IOException {
        final InputStream in = new ByteArrayInputStream("0123456789".getBytes());
        try (final RangeRequestInputStream out = new RangeRequestInputStream(in, 0L, -1L)) {
            final String s = IOUtils.toString(out);
            assertEquals("0123456789", s);
        }
    }
}
