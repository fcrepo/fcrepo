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
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.junit.runner.RunWith;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Unit tests for HttpRdfService
 * @author bseeger
 * @since 2019-11-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpRdfServiceTest {

    private static final Logger log = getLogger(HttpRdfService.class);

    @Mock
    HttpIdentifierConverter idTranslator;

    @Mock
    FedoraResource resource;

    @InjectMocks
    private HttpRdfService httpRdfService;

    private static final String FEDORA_URI = "http://www.example.com/fedora/rest/resource1";
    private static final String FEDORA_ID = "info:fedora/resource1";
    private static final String NON_FEDORA_URI = "http://www.otherdomain.org/resource5";
    private static final String RDF =
                "@prefix dc: <"  + DC.getURI() + "> ." +
                "@prefix dcterms: <"  + DCTerms.getURI() + "> ." +
                "<" + FEDORA_URI + "> dc:title 'fancy title' ;" +
                "    dcterms:isPartOf <" + NON_FEDORA_URI + "> ;" +
                "    dcterms:isPartOf <" + FEDORA_URI + "> .";

    private static final MediaType CONTENT_TYPE = new MediaType("text", "turtle");

    @Before
    public void setup(){
        when(idTranslator.toInternalId(FEDORA_URI)).thenReturn(FEDORA_ID);
        when(idTranslator.inExternalDomain(FEDORA_URI)).thenReturn(true);
        when(idTranslator.inExternalDomain(NON_FEDORA_URI)).thenReturn(false);
        when(resource.getId()).thenReturn(FEDORA_ID);

        log.debug("Rdf is: {}", RDF);
    }

    @Test
    public void testGetModelFromInputStream() {

        final InputStream requestBodyStream = new ByteArrayInputStream(RDF.getBytes());
        final Model model = httpRdfService.bodyToInternalModel(FEDORA_URI, requestBodyStream,
            CONTENT_TYPE);

        assertFalse(model.isEmpty());

        verifyTriples(model);
    }

    @Test
    public void testGetRdfStreamFromInputStream() {

        final InputStream requestBodyStream = new ByteArrayInputStream(RDF.getBytes());
        final RdfStream stream = httpRdfService.bodyToInternalStream(FEDORA_URI, requestBodyStream,
            CONTENT_TYPE);

        assertTrue(stream.toString().length() > 0);
        verifyTriples(stream);
    }

    private void verifyTriples(final Model model)  {

        // make sure it changed the fedora uri to id, but didn't touch the non fedora domain
        assertTrue(model.contains(model.createResource(FEDORA_ID),
            model.createProperty(DCTerms.isPartOf.toString()),
            model.createResource(NON_FEDORA_URI)));

        // make sure there are no triples that have the subject of the fedora uri
        assertFalse(model.containsResource(model.createResource(FEDORA_URI)));

        // make sure it translated the fedora uri to fedora id for this triple
        assertTrue(model.contains(model.createResource(FEDORA_ID),
            model.createProperty(DCTerms.isPartOf.toString()),
            model.createResource(FEDORA_ID)));
    }

    private void verifyTriples(final RdfStream rdfStream) {
        verifyTriples(rdfStream.collect(toModel()));
    }
}
