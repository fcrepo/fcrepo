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
package org.fcrepo.kernel.impl.services;


import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.junit.Assert.assertEquals;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.XSD;
import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AbstractServiceTest {

    private final List<String> stringTypesNotValid = Stream.of(MEMENTO_TYPE, RESOURCE.toString(),
            FEDORA_RESOURCE.toString()).collect(Collectors.toList());

    private final List<String> stringTypesValid = Stream.of(MEMENTO_TYPE, CONTAINER.toString(),
            BASIC_CONTAINER.toString()).collect(Collectors.toList());

    private final String defaultInteractionModel = DEFAULT_INTERACTION_MODEL.toString();

    private final AbstractService service = new AbstractService();

    @Test
    public void testSendingValidInteractionModel() {
        // If you provide a valid interaction model, you should always get it back.
        final String expected = BASIC_CONTAINER.toString();
        final String model1 = service.determineInteractionModel(stringTypesValid, false, false,
                false);
        assertEquals(expected, model1);
        final String model2 = service.determineInteractionModel(stringTypesValid, false, false,
                true);
        assertEquals(expected, model2);
        final String model3 = service.determineInteractionModel(stringTypesValid, false, true,
                false);
        assertEquals(expected, model3);
        final String model4 = service.determineInteractionModel(stringTypesValid, false, true,
                true);
        assertEquals(expected, model4);
        final String model5 = service.determineInteractionModel(stringTypesValid, true, false,
                false);
        assertEquals(expected, model5);
        final String model6 = service.determineInteractionModel(stringTypesValid, true, false,
                true);
        assertEquals(expected, model6);
        final String model7 = service.determineInteractionModel(stringTypesValid, true, true,
                false);
        assertEquals(expected, model7);
        final String model8 = service.determineInteractionModel(stringTypesValid, true, true,
                true);
        assertEquals(expected, model8);
    }

    @Test
    public void testSendingInvalidInteractionModelIsNotRdf() {
        final String model = service.determineInteractionModel(stringTypesNotValid, false, false,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testNotRdfNoContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = service.determineInteractionModel(stringTypesNotValid, false, false,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testNotRdfContentPresentNotExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = service.determineInteractionModel(stringTypesNotValid, false, true,
                false);
        assertEquals(expected, model);
    }

    @Test
    public void testNotRdfContentPresentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = service.determineInteractionModel(stringTypesNotValid, false, true,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfNoContentNotExternal() {
        final String model = service.determineInteractionModel(stringTypesNotValid, true, false,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfNoContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = service.determineInteractionModel(stringTypesNotValid, true, false,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfHasContentNotExternal() {
        final String model = service.determineInteractionModel(stringTypesNotValid, true, true,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfHasContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = service.determineInteractionModel(stringTypesNotValid, true, true,
                true);
        assertEquals(expected, model);
    }

    // TODO: Move to operation builder
    // @Test(expected = MalformedRdfException.class)
    public void testCheckServerManagedLdpType() throws Exception {
        final InputStream graph = IOUtils.toInputStream("@prefix ldp: <" + LDP_NAMESPACE + "> .\n@prefix dc: <" + DC_11.getURI() + "> .\n" +
                "@prefix example: <http://example.org/stuff#> .\n" +
                "<> a example:Thing, ldp:BasicContainer ; dc:title \"The thing\" .", "UTF-8");
        final Model model = ModelFactory.createDefaultModel();
        model.read(graph, "http://localhost:8080/rest/test1", "TURTLE");
        //service.checkForServerManagedTriples(model);
    }

    // TODO: Move to operation builder
    // @Test(expected = MalformedRdfException.class)
    public void testCheckServerManagedPredicate() throws Exception {
        final InputStream graph = IOUtils.toInputStream("@prefix fr: <" + REPOSITORY_NAMESPACE + "> .\n@prefix dc: <" + DC_11.getURI() + "> .\n" +
                "@prefix example: <http://example.org/stuff#> .\n@prefix xsd: <" + XSD.getURI() + ">.\n" +
                "<> a example:Thing; dc:title \"The thing\"; fr:lastModified \"2000-01-01T00:00:00Z\"^^xsd:datetime .", "UTF-8");
        final Model model = ModelFactory.createDefaultModel();
        model.read(graph, "http://localhost:8080/rest/test1", "TURTLE");
        //service.checkForServerManagedTriples(model);
    }

    // TODO: Move to operation builder
    // @Test
    public void testCheckServerManagedSuccess() throws Exception {
        final InputStream graph = IOUtils.toInputStream("@prefix dc: <" + DC_11.getURI() + "> .\n" +
                "@prefix example: <http://example.org/stuff#> .\n@prefix xsd: <" + XSD.getURI() + ">.\n" +
                "<> a example:Thing; dc:title \"The thing\"; example:lastModified \"2000-01-01T00:00:00Z\"^^xsd:datetime .", "UTF-8");
        final Model model = ModelFactory.createDefaultModel();
        model.read(graph, "http://localhost:8080/rest/test1", "TURTLE");
        //service.checkForServerManagedTriples(model);
    }

    @Test(expected = ServerManagedTypeException.class)
    public void testHasRestrictedPathFail() throws Exception {
        final String path = UUID.randomUUID().toString() + "/fedora:stuff/" + UUID.randomUUID().toString();
        service.hasRestrictedPath(path);
    }

    @Test
    public void testHasRestrictedPathPass() throws Exception {
        final String path = UUID.randomUUID().toString() + "/dora:stuff/" + UUID.randomUUID().toString();
        service.hasRestrictedPath(path);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailDblQ() throws Exception {
        final List<String> links = Stream.of("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                "handling=\"proxy\"; type=\"image/tiff\"", "<http://example.org/some/otherlocation>; rel=\"acl\"")
                .collect(Collectors.toList());
        service.checkAclLinkHeader(links);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailSingleQ() throws Exception {
        final List<String> links = Stream.of("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                        "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                        "handling=\"proxy\"; type=\"image/tiff\"", "<http://example.org/some/otherlocation>; rel='acl'")
                .collect(Collectors.toList());
        service.checkAclLinkHeader(links);
    }

    @Test(expected = RequestWithAclLinkHeaderException.class)
    public void testCheckAclLinkHeaderFailNoQ() throws Exception {
        final List<String> links = Stream.of("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                        "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                        "handling=\"proxy\"; type=\"image/tiff\"", "<http://example.org/some/otherlocation>; rel=acl")
                .collect(Collectors.toList());
        service.checkAclLinkHeader(links);
    }

    @Test
    public void testCheckAclLinkHeaderSuccess() throws Exception {
        final List<String> links = Stream.of("<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"",
                "<http://example.org/some/location/image.tiff>; " +
                "rel=\"http://fedora.info/definitions/fcrepo#ExternalContent\"; " +
                "handling=\"proxy\"; type=\"image/tiff\"")
                .collect(Collectors.toList());
        service.checkAclLinkHeader(links);
    }
}
