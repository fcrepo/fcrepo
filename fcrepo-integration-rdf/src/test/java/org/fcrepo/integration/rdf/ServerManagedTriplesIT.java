/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
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
        verifyRejectUriRef(HAS_MEMBER_RELATION.getURI(),
                           REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE,
                           DIRECT_CONTAINER_LINK_HEADER);
        verifyRejectUpdateUriRef(HAS_MEMBER_RELATION.getURI(),
                                 REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE,
                                 DIRECT_CONTAINER_LINK_HEADER);
        verifyRejectUriRef(HAS_MEMBER_RELATION.getURI(), CONTAINS.getURI(), DIRECT_CONTAINER_LINK_HEADER);
        verifyRejectUpdateUriRef(HAS_MEMBER_RELATION.getURI(), CONTAINS.getURI(), DIRECT_CONTAINER_LINK_HEADER);

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
        verifyRejectUriRef(predicate, refURI, null);
    }

    private void verifyRejectUriRef(final String predicate, final String refURI, final String link) throws Exception {
        final String pid = getRandomUniqueId();
        final String content = "<> <" + predicate + "> <" + refURI + "> .";
        final var put = putObjMethod(pid, "text/turtle", content);
        if (link != null) {
            put.setHeader(LINK, link);
        }
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(409, response.getStatusLine().getStatusCode(),
                    "Must reject server managed property <" + predicate + "> <" + refURI + ">");
        }
    }

    private void verifyRejectLiteral(final String predicate) throws Exception {
        final String pid = getRandomUniqueId();
        final String content = "<> <" + predicate + "> \"value\" .";
        try (final CloseableHttpResponse response = execute(putObjMethod(pid, "text/turtle", content))) {
            assertEquals(409, response.getStatusLine().getStatusCode(),
                    "Must reject server managed property <" + predicate + ">");
        }
    }

    private void verifyRejectUpdateLiteral(final String predicate) throws Exception {
        final String updateString =
                "INSERT { <> <" + predicate + "> \"value\" } WHERE { }";

        final String pid = getRandomUniqueId();
        createObject(pid);
        try (final CloseableHttpResponse response = performUpdate(pid, updateString)) {
            assertEquals(409, response.getStatusLine().getStatusCode(),
                    "Must reject update of server managed property <" + predicate + ">");
        }
    }

    private void verifyRejectUpdateRdfType(final String typeURI) throws Exception {
        verifyRejectUpdateUriRef(RDF_NAMESPACE + "type", typeURI);
    }

    private void verifyRejectUpdateUriRef(final String predicate, final String refURI) throws Exception {
        verifyRejectUpdateUriRef(predicate, refURI, null);
    }

    private void verifyRejectUpdateUriRef(final String predicate,
                                          final String refURI,
                                          final String link) throws Exception {
        final String updateString =
            "INSERT { <> <" + predicate + "> <" + refURI + "> } WHERE { }";

        final String pid = getRandomUniqueId();
        if (link != null) {
            createObjectWithLinkHeader(pid, link);
        } else {
            createObject(pid);
        }
        try (final CloseableHttpResponse response = performUpdate(pid, updateString)) {
            assertEquals(409, response.getStatusLine().getStatusCode(),
                    "Must reject update of server managed property <" + predicate + "> <" + refURI + ">");
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
        assertEquals("text/plain", resc.getProperty(HAS_MIME_TYPE).getString());
        assertEquals(filename, resc.getProperty(HAS_ORIGINAL_NAME).getString());

        // verify properties can be deleted
        // iana:describedby, premis:hasSize, ebucore:hasMimeType and ebucore:filename cannot be totally removed since
        // they are added in to the response

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
        assertFalse(resultResc.hasProperty(property, object), "Must not contain deleted property " + property);
    }

    private void verifySetProperty(final String pid, final String subjectURI, final Property property,
            final RDFNode object) throws Exception {
        final String value = rdfNodeToString(object);
        final String updateString =
                "INSERT { <> <" + property.getURI() + "> " + value + " } WHERE { }";

        performUpdate(pid, updateString).close();

        final Model model = getModel(pid);
        final Resource resc = model.getResource(subjectURI);
        assertTrue(resc.hasProperty(property, object), "Must contain updated property " + property);
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
