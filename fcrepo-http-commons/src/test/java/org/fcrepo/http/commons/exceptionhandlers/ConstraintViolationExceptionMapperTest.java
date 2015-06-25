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

import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.exception.ConstraintViolationException;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.RdfLexicon.CONSTRAINED_BY;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * ConstraintViolationExceptionMapperTest
 *
 * @author whikloj
 * @since 2015-06-22
 */
@RunWith(MockitoJUnitRunner.class)
public class ConstraintViolationExceptionMapperTest {

    private UriInfo mockInfo;

    @Mock
    private ConstraintViolationExceptionMapper mapper;

    @Before
    public void setUp() throws RepositoryException, URISyntaxException {
        this.mockInfo = getUriInfoImpl();
    }

    @Test
    public void testBuildConstraintLink() {
        final URI testLink = URI.create("http://localhost/static/constraints/ConstraintViolationException.rdf");

        final ConstraintViolationException ex = new ConstraintViolationException("This is an error.");

        final Link link = ConstraintViolationExceptionMapper.buildConstraintLink(ex, mockInfo);

        assertEquals(link.getRel(), CONSTRAINED_BY.getURI());
        assertEquals(link.getUri(), testLink);
    }
}
