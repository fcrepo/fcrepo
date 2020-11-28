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
package org.fcrepo.integration.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author bbpennel
 */
@Ignore // TODO FIX THESE TESTS
public class ServerManagedTriplesIT extends AbstractResourceIT {

    // BINARY DESCRIPTIONS
    public static final Property DESCRIBED_BY =
            createProperty("http://www.iana.org/assignments/relation/describedby");

    private final static String NON_EXISTENT_PREDICATE = "any_predicate_will_do";

    private final static String NON_EXISTENT_TYPE = "any_type_is_fine";

    private final static List<String> INDIVIDUAL_SM_PREDS = asList(
            PREMIS_NAMESPACE + "hasMessageDigest",
            PREMIS_NAMESPACE + "hasFixity");

    @Test
    public void testServerManagedPredicates() throws Exception {
        for (final String predicate : INDIVIDUAL_SM_PREDS) {
            verifyRejectLiteral(predicate);
            verifyRejectUpdateLiteral(predicate);
        }
    }

    @Test
    public void testLdpNamespace() throws Exception {
        // Verify that ldp:contains referencing another object is rejected
        final String refPid = getRandomUniqueId();
        final String refURI = serverAddress + refPid;
        createObject(refPid);

        verifyRejectUriRef(CONTAINS.getURI(), refURI);
        verifyRejectUpdateUriRef(CONTAINS.getURI(), refURI);

        // Verify that ldp:hasMemberRelation referencing an SMT is rejected
        verifyRejectUriRef(HAS_MEMBER_RELATION.getURI(), REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE);
        verifyRejectUpdateUriRef(HAS_MEMBER_RELATION.getURI(), REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE);
        verifyRejectUriRef(HAS_MEMBER_RELATION.getURI(), CONTAINS.getURI());
        verifyRejectUpdateUriRef(HAS_MEMBER_RELATION.getURI(), CONTAINS.getURI());

        // Verify that types in the ldp namespace are rejected
        verifyRejectRdfType(RESOURCE.getURI());
        verifyRejectUpdateRdfType(RESOURCE.getURI());
        verifyRejectRdfType(LDP_NAMESPACE + NON_EXISTENT_TYPE);
        verifyRejectUpdateRdfType(LDP_NAMESPACE + NON_EXISTENT_TYPE);
    }

    @Test
    public void testFedoraNamespace() throws Exception {
        // Verify rejection of known property
        verifyRejectLiteral(WRITABLE.getURI());
        verifyRejectUpdateLiteral(WRITABLE.getURI());
        // Verify rejection of non-existent property
        verifyRejectLiteral(REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE);
        verifyRejectUpdateLiteral(REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE);

        // Verify that types in this namespace are rejected
        verifyRejectRdfType(FEDORA_CONTAINER.getURI());
        verifyRejectUpdateRdfType(FEDORA_CONTAINER.getURI());
        verifyRejectRdfType(REPOSITORY_NAMESPACE + NON_EXISTENT_TYPE);
        verifyRejectUpdateRdfType(REPOSITORY_NAMESPACE + NON_EXISTENT_TYPE);
    }

    @Test
    public void testMementoNamespace() throws Exception {
        // Verify rejection of known property
        verifyRejectLiteral(MEMENTO_NAMESPACE + "mementoDatetime");
        verifyRejectUpdateLiteral(MEMENTO_NAMESPACE + "mementoDatetime");
        // Verify rejection of non-existent property
        verifyRejectLiteral(MEMENTO_NAMESPACE + NON_EXISTENT_PREDICATE);
        verifyRejectUpdateLiteral(MEMENTO_NAMESPACE + NON_EXISTENT_PREDICATE);

        // Verify rejection of known type
        verifyRejectRdfType(MEMENTO_TYPE);
        verifyRejectUpdateRdfType(MEMENTO_TYPE);
        // Verify rejection of non-existent type
        verifyRejectRdfType(MEMENTO_NAMESPACE + NON_EXISTENT_TYPE);
        verifyRejectUpdateRdfType(MEMENTO_NAMESPACE + NON_EXISTENT_TYPE);
    }

    private void verifyRejectRdfType(final String typeURI) throws Exception {
        verifyRejectUriRef(RDF_NAMESPACE + "type", typeURI);
    }

    private void verifyRejectUriRef(final String predicate, final String refURI) throws Exception {
        final String pid = getRandomUniqueId();
        final String content = "<> <" + predicate + "> <" + refURI + "> .";
        try (final CloseableHttpResponse response = execute(putObjMethod(pid, "text/turtle", content))) {
            assertEquals("Must reject server managed property <" + predicate + "> <" + refURI + ">",
                    409, response.getStatusLine().getStatusCode());
        }
    }

    private void verifyRejectLiteral(final String predicate) throws Exception {
        final String pid = getRandomUniqueId();
        final String content = "<> <" + predicate + "> \"value\" .";
        try (final CloseableHttpResponse response = execute(putObjMethod(pid, "text/turtle", content))) {
            assertEquals("Must reject server managed property <" + predicate + ">",
                    409, response.getStatusLine().getStatusCode());
        }
    }

    private void verifyRejectUpdateLiteral(final String predicate) throws Exception {
        final String updateString =
                "INSERT { <> <" + predicate + "> \"value\" } WHERE { }";

        final String pid = getRandomUniqueId();
        createObject(pid);
        try (final CloseableHttpResponse response = performUpdate(pid, updateString)) {
            assertEquals("Must reject update of server managed property <" + predicate + ">",
                    409, response.getStatusLine().getStatusCode());
        }
    }

    private void verifyRejectUpdateRdfType(final String typeURI) throws Exception {
        verifyRejectUpdateUriRef(RDF_NAMESPACE + "type", typeURI);
    }

    private void verifyRejectUpdateUriRef(final String predicate, final String refURI) throws Exception {
        final String updateString =
                "INSERT { <> <" + predicate + "> <" + refURI + "> } WHERE { }";

        final String pid = getRandomUniqueId();
        createObject(pid);
        try (final CloseableHttpResponse response = performUpdate(pid, updateString)) {
            assertEquals("Must reject update of server managed property <" + predicate + "> <" + refURI + ">",
                    409, response.getStatusLine().getStatusCode());
        }
    }

    private CloseableHttpResponse performUpdate(final String pid, final String updateString) throws Exception {
        final HttpPatch patchProp = patchObjMethod(pid);
        patchProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        patchProp.setEntity(new StringEntity(updateString));
        return execute(patchProp);
    }

    @Test
    public void testNonRdfSourceServerGeneratedTriples() throws Exception {
        final String pid = getRandomUniqueId();
        final String describedPid = pid + "/" + FCR_METADATA;
        final String location = serverAddress + pid;

        final String filename = "some-file.txt";
        final String content = "this is the content";
        createBinary(pid, filename, TEXT_PLAIN, content);

        final Model model = getModel(describedPid);

        // verify properties initially generated
        final Resource resc = model.getResource(location);
        assertEquals(content.length(), resc.getProperty(HAS_SIZE).getLong());
        assertEquals(serverAddress + describedPid, resc.getProperty(DESCRIBED_BY).getResource().getURI());
        assertEquals("text/plain", resc.getProperty(HAS_MIME_TYPE).getString());
        assertEquals(filename, resc.getProperty(HAS_ORIGINAL_NAME).getString());

        // verify properties can be deleted
        // iana:describedby cannot be totally removed since it is added in the response
        verifyDeleteExistingProperty(describedPid, location, HAS_SIZE,
                resc.getProperty(HAS_SIZE).getObject());
        verifyDeleteExistingProperty(describedPid, location, HAS_MIME_TYPE,
                resc.getProperty(HAS_MIME_TYPE).getObject());
        verifyDeleteExistingProperty(describedPid, location, HAS_ORIGINAL_NAME,
                resc.getProperty(HAS_ORIGINAL_NAME).getObject());

        // verify property can be added
        verifySetProperty(describedPid, location, DESCRIBED_BY, createResource("http://example.com"));
        verifySetProperty(describedPid, location, HAS_SIZE, model.createTypedLiteral(99L));
        verifySetProperty(describedPid, location, HAS_MIME_TYPE, model.createLiteral("text/special"));
        verifySetProperty(describedPid, location, HAS_ORIGINAL_NAME, model.createLiteral("different.txt"));

        // Verify deletion of added describedby
        verifyDeleteExistingProperty(describedPid, location, DESCRIBED_BY,
                createResource("http://example.com"));
    }

    private void createBinary(final String pid, final String filename, final String contentType, final String content)
            throws Exception {
        final HttpPost post = new HttpPost(serverAddress);
        post.setEntity(new StringEntity(content));
        post.setHeader("Slug", pid);
        post.setHeader(CONTENT_TYPE, contentType);
        post.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        post.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(post)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

    private void verifyDeleteExistingProperty(final String pid, final String subjectURI,
            final Property property, final RDFNode object) throws Exception {
        final String value = rdfNodeToString(object);
        final String deleteString =
                "DELETE { <> <" + property.getURI() + "> " + value + " } WHERE { }";

        performUpdate(pid, deleteString).close();

        final Model resultModel = getModel(pid);
        final Resource resultResc = resultModel.getResource(subjectURI);
        assertFalse("Must not contain deleted property " + property, resultResc.hasProperty(property, object));
    }

    private void verifySetProperty(final String pid, final String subjectURI, final Property property,
            final RDFNode object) throws Exception {
        final String value = rdfNodeToString(object);
        final String updateString =
                "INSERT { <> <" + property.getURI() + "> " + value + " } WHERE { }";

        performUpdate(pid, updateString).close();

        final Model model = getModel(pid);
        final Resource resc = model.getResource(subjectURI);
        assertTrue("Must contain updated property " + property, resc.hasProperty(property, object));
    }

    private String rdfNodeToString(final RDFNode object) {
        String value;
        if (object.isLiteral()) {
            final Literal literal = object.asLiteral();
            value = "\"" + literal.getValue().toString() + "\"";
            if (!literal.getDatatype().equals(XSDDatatype.XSDstring)) {
                value += "^^<" + literal.getDatatypeURI() + ">";
            }
        } else {
            value = "<" + object.toString() + ">";
        }
        return value;
    }
}
