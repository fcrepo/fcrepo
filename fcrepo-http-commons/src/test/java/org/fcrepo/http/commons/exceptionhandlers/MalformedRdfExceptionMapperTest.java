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
package org.fcrepo.http.commons.exceptionhandlers;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.junit.Assert.assertEquals;

/**
 * @author cabeer
 */
public class MalformedRdfExceptionMapperTest {

    private MalformedRdfExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new MalformedRdfExceptionMapper();

    }
    @Test
    public void testToResponse() throws IOException {
        final Response response = testObj.toResponse(new MalformedRdfException("xyz"));

        final Link link = response.getLink(CONSTRAINED_BY.getURI());
        assertEquals(CONSTRAINED_BY.getURI(), link.getRel());
        assertEquals("data", link.getUri().getScheme());
        final String[] split = link.getUri().toString().split(",", 2);
        assertEquals("Constraint data appears malformed", 2, split.length);
        assertEquals("xyz", IOUtils.toString(Base64.decodeBase64(split[1].getBytes()), "UTF-8"));
    }

    @Test
    public void testToResponseError() throws IOException {
        final String errorPrefix = "org.modeshape.jcr.value.ValueFormatException: ";
        final String errorSuffix = "Error converting ...";
        final Response response = testObj.toResponse(new MalformedRdfException(errorPrefix + errorSuffix));

        assertEquals(errorSuffix, response.getEntity().toString());
    }

    @Test
    public void testToResponseError2() throws IOException {
        final String errorPrefix = "org.modeshape.jcr.value.ValueFormat: ";
        final String errorSuffix = "Error converting ...";
        final Response response = testObj.toResponse(new MalformedRdfException(errorPrefix + errorSuffix));

        assertEquals(errorPrefix + errorSuffix, response.getEntity().toString());
    }
}
