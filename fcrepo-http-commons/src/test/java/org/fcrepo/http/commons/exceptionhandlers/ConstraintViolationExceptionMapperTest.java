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

import java.net.URI;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.getServletContextImpl;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.junit.Assert.assertEquals;

import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * ConstraintViolationExceptionMapperTest
 *
 * @author whikloj
 * @since 2015-06-22
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConstraintViolationExceptionMapperTest {

    private UriInfo mockInfo;

    private ServletContext mockContext;

    @Before
    public void setUp() {
        this.mockInfo = getUriInfoImpl();
        this.mockContext = getServletContextImpl();
    }

    @Test
    public void testBuildConstraintLink() {
        final URI testLink = URI.create("http://localhost/fcrepo/static/constraints/ConstraintViolationException.rdf");

        final ConstraintViolationException ex = new ConstraintViolationException("This is an error.");

        final Link link = ConstraintViolationExceptionMapper.buildConstraintLink(ex, mockContext, mockInfo);

        assertEquals(link.getRel(), CONSTRAINED_BY.getURI());
        assertEquals(link.getUri(), testLink);
    }
}
