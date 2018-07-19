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
package org.fcrepo.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Link;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author bbpennel
 */
@RunWith(MockitoJUnitRunner.class)
public class ExternalContentHandlerFactoryTest {

    @Mock
    private ExternalContentPathValidator validator;

    private ExternalContentHandlerFactory factory;

    @Before
    public void init() {
        factory = new ExternalContentHandlerFactory();
        factory.setValidator(validator);
    }

    @Test
    public void testValidLinkHeader() throws Exception {
        final ExternalContentHandler handler = factory.createFromLinks(
                makeLinks("http://test.com"));

        assertEquals("http://test.com", handler.getURL());
        assertEquals("text/plain", handler.getContentType().toString());
        assertEquals("proxy", handler.getHandling());
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testValidationFailure() throws Exception {
        doThrow(new ExternalMessageBodyException("")).when(validator).validate(anyString());

        factory.createFromLinks(makeLinks("http://test.com"));
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testMultipleExtLinkHeaders() throws Exception {
        final List<String> links = makeLinks("http://test.com", "http://test2.com");

        factory.createFromLinks(links);
    }

    @Test(expected = ExternalMessageBodyException.class)
    public void testGetWithExternalMessageMissingURLBinary() throws Exception {
        final List<String> links = makeLinks("http://test.com");
        links.set(0, links.get(0).replaceAll("<.*>", "< >"));

        factory.createFromLinks(links);
    }

    private List<String> makeLinks(final String... uris) {
        return Arrays.stream(uris)
                .map(uri -> Link.fromUri(uri)
                        .rel(EXTERNAL_CONTENT.toString())
                        .param("handling", "proxy")
                        .type("text/plain")
                        .build()
                        .toString())
                .collect(Collectors.toList());
    }
}
