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
package org.fcrepo.http.commons.exceptionhandlers;

import static org.junit.Assert.assertEquals;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;

import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * FedoraInvalidNamespaceExceptionMapperTest class.
 * </p>
 *
 * @author Daniel Bernstein
 * @since January 19, 2017
 */
public class FedoraInvalidNamespaceExceptionMapperTest {

    private FedoraInvalidNamespaceExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new FedoraInvalidNamespaceExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final FedoraInvalidNamespaceException input = new FedoraInvalidNamespaceException(
                "Invalid namespace", null);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
        assertEquals(TEXT_PLAIN_WITH_CHARSET, actual.getHeaderString(CONTENT_TYPE));

    }
}
