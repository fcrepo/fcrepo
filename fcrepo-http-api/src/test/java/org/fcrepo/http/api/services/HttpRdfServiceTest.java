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
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;

import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.graph.Triple;
import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.runner.RunWith;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
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

    private final String fedoraUri = "http://www.example.com/fedora/rest/resource1";
    private final String fedoraId = "info:fedora/resource1";
    private final String externalUri = "http://www.otherdomain.org/resource5";
    private final String rdfString = String.format("@prefix dc: <http://purl.org/dc/elements/1.1/> . " +
                "@prefix dcterms: <http://purl.org/dc/terms/> ." +
                "<%s>  dc:title 'fancy title' ;" +
                "  dcterms:isPartOf <%s>, <%s> . ", fedoraUri, externalUri, fedoraUri);

    private final MediaType contentType = new MediaType("text", "turtle");
    private final InputStream requestBodyStream = new ByteArrayInputStream(rdfString.getBytes());

    @Test
    public void testGetModelFromInputStream() {
        when(idTranslator.toInternalId(fedoraUri)).thenReturn(fedoraId);
        when(idTranslator.inExternalDomain(fedoraUri)).thenReturn(true);
        when(idTranslator.inExternalDomain(externalUri)).thenReturn(false);

        when(resource.getId()).thenReturn(fedoraId);

        final Model model = httpRdfService.bodyToInternalModel(fedoraUri, requestBodyStream,
            contentType);

        assertTrue(model.listStatements().toList().size() > 0);

        verifyTriples(model);
    }

    @Test
    public void testGetRdfStreamFromInputStream() {
        when(idTranslator.toInternalId(fedoraUri)).thenReturn("info:fedora/resource1");
        when(idTranslator.inExternalDomain(fedoraUri)).thenReturn(true);
        when(idTranslator.inExternalDomain(externalUri)).thenReturn(false);

        final RdfStream stream = httpRdfService.bodyToInternalStream(fedoraUri, requestBodyStream,
            contentType);

        assertTrue(stream.toString().length() > 0);
        verifyTriples(stream);
    }

    private void verifyTriples(final Model model)  {
        verifyTriples(fromModel(model.getResource(fedoraId).asNode(),model));
    }

    private void verifyTriples(final RdfStream rdfStream) {

        final List<Triple> triples = rdfStream.map(triple -> {
            assertFalse(triple.getSubject().getURI().equals(fedoraUri));
            assertFalse(triple.getObject().isURI() && triple.getObject().toString().equals(fedoraUri));
            return triple;
        })
        .filter(triple -> triple.getObject().isURI() && triple.getObject().getURI().equals(externalUri))
        .collect(Collectors.toList());

        assertTrue(triples.size() == 1);
    }
}
