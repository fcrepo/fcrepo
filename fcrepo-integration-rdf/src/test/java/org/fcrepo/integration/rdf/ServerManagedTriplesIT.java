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

import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Test;

/**
 * @author bbpennel
 */
public class ServerManagedTriplesIT extends AbstractResourceIT {

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
    public void testLdpContains() throws Exception {
        final String refPid = getRandomUniqueId();
        final String refURI = serverAddress + refPid;
        createObject(refPid);

        verifyRejectUriRef(LDP_NAMESPACE + "contains", refURI);
        verifyRejectUpdateUriRef(LDP_NAMESPACE + "contains", refURI);
    }

    @Test
    public void testLdpNamespace() throws Exception {
        // Verify that ldp:contains referencing another object is rejected
        final String refPid = getRandomUniqueId();
        final String refURI = serverAddress + refPid;
        createObject(refPid);

        verifyRejectUriRef(LDP_NAMESPACE + "contains", refURI);
        verifyRejectUpdateUriRef(LDP_NAMESPACE + "contains", refURI);

        // Verify that ldp:hasMemberRelation referencing an SMT is rejected
        verifyRejectUriRef(HAS_MEMBER_RELATION.getURI(), REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE);
        verifyRejectUpdateUriRef(HAS_MEMBER_RELATION.getURI(), REPOSITORY_NAMESPACE + NON_EXISTENT_PREDICATE);
        verifyRejectUriRef(HAS_MEMBER_RELATION.getURI(), LDP_NAMESPACE + "contains");
        verifyRejectUpdateUriRef(HAS_MEMBER_RELATION.getURI(), LDP_NAMESPACE + "contains");

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
        try (final CloseableHttpResponse response = performUpdate(pid, updateString)) {
            assertEquals("Must reject update of server managed property <" + predicate + "> <" + refURI + ">",
                    409, response.getStatusLine().getStatusCode());
        }
    }

    private CloseableHttpResponse performUpdate(final String pid, final String updateString) throws Exception {
        createObject(pid);

        final HttpPatch patchProp = patchObjMethod(pid);
        patchProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        patchProp.setEntity(new StringEntity(updateString));
        return execute(patchProp);
    }
}
