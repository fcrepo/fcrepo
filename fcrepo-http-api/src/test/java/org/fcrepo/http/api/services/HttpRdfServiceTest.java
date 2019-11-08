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
package org.fcrepo.http.api.services;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;
import org.junit.Ignore;
import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for HttpRdfService
 * @author bseeger
 * @since 2019-11-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpRdfServiceTest {


    @Mock
    HttpIdentifierConverter idTranslator;

    @Mock
    FedoraResource resource;

    @InjectMocks
    private HttpRdfService httpRdfService;

    private final String resourceUri = "www.example.com/fedora/rest/resource1";
    private final String systemId = "info:fedora/resource1";
    private final String rdfString = "@prefix dc: <http://purl.org/dc/elements/1.1/> . \n" +
                "<www.example.com/resource1>\n" +
                "dc:title 'fancy title'\n";
    private final MediaType contentType = new MediaType("text", "turtle");
    private final InputStream requestBodyStream = new ByteArrayInputStream(rdfString.getBytes());

    @Test
    @Ignore
    public void testGetModelFromInputStream() {
        when(idTranslator.convert(resourceUri)).thenReturn("info:fedora/resource1");
        when(resource.getId()).thenReturn("info:fedora/resource1");

        final Model model = httpRdfService.bodyToInternalModel(resource, requestBodyStream,
            contentType, idTranslator);

        assertTrue(model.listStatements().toList().size() > 0);

        // TODO - look at triples and ensure that the URI has been changed to an internal Fedora ID
    }

    @Test
    @Ignore
    public void testGetRdfStreamFromInputStream() {
        when(idTranslator.convert(resourceUri)).thenReturn("info:fedora/resource1");

        final RdfStream stream = httpRdfService.bodyToInternalStream(resource, requestBodyStream,
            contentType, idTranslator);

        assertTrue(stream.toString().length() > 0);

        // TODO - look at triples and ensure that the URI has been changed to an internal Fedora ID
    }



}
