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
package org.fcrepo.integration.auth.webac;

import static java.util.Arrays.stream;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.auth.webac.WebACRolesProvider.GROUP_AGENT_BASE_URI_PROPERTY;
import static org.fcrepo.auth.webac.WebACRolesProvider.USER_AGENT_BASE_URI_PROPERTY;
import static org.fcrepo.http.api.FedoraAcl.ROOT_AUTHORIZATION_PROPERTY;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.fcrepo.integration.http.api.TestIsolationExecutionListener;
import org.glassfish.grizzly.utils.Charsets;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author Peter Eichman
 * @author whikloj
 * @since September 4, 2015
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class WebACRecipesIT extends AbstractResourceIT {

    private static final Logger logger = LoggerFactory.getLogger(WebACRecipesIT.class);

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private final ContentType turtleContentType = ContentType.create("text/turtle", "UTF-8");

    private final ContentType sparqlContentType = ContentType.create("application/sparql-update", "UTF-8");

    /**
     * Convenience method to create an ACL with 0 or more authorization resources in the repository.
     */
    private String ingestAcl(final String username,
            final String aclFilePath, final String aclResourcePath) throws IOException {

        // create the ACL
        final HttpResponse aclResponse = ingestTurtleResource(username, aclFilePath, aclResourcePath);

        // return the URI to the newly created resource
        return aclResponse.getFirstHeader("Location").getValue();
    }

    /**
     * Convenience method to POST the contents of a Turtle file to the repository to create a new resource. Returns
     * the HTTP response from that request. Throws an IOException if the server responds with anything other than a
     * 201 Created response code.
     */
    private HttpResponse ingestTurtleResource(final String username, final String path, final String requestURI)
            throws IOException {
        final HttpPut request = new HttpPut(requestURI);

        logger.debug("PUT to {} to create {}", requestURI, path);

        setAuth(request, username);

        final InputStream file = this.getClass().getResourceAsStream(path);
        final InputStreamEntity fileEntity = new InputStreamEntity(file);
        request.setEntity(fileEntity);
        request.setHeader("Content-Type", "text/turtle");

        try (final CloseableHttpResponse response = execute(request)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            return response;
        }

    }

    /**
     * Convenience method to set up a regular FedoraResource
     *
     * @param path Path to put the resource under
     * @return the Location of the newly created resource
     * @throws IOException on error
     */
    private String ingestObj(final String path) throws IOException {
        final HttpPut request = putObjMethod(path.replace(serverAddress, ""));
        setAuth(request, "fedoraAdmin");
        try (final CloseableHttpResponse response = execute(request)) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("Location").getValue();
        }
    }

    private String ingestBinary(final String path, final HttpEntity body) throws IOException {
        logger.info("Ingesting {} binary to {}", body.getContentType().getValue(), path);
        final HttpPut request = new HttpPut(serverAddress + path);
        setAuth(request, "fedoraAdmin");
        request.setEntity(body);
        request.setHeader(body.getContentType());
        final CloseableHttpResponse response = execute(request);
        assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        final String location = response.getFirstHeader("Location").getValue();
        logger.info("Created binary at {}", location);
        return location;

    }

    private String ingestDatastream(final String path, final String ds) throws IOException {
        final HttpPut request = putDSMethod(path, ds, "some not so random content");
        setAuth(request, "fedoraAdmin");
        try (final CloseableHttpResponse response = execute(request)) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("Location").getValue();
        }
    }

    /**
     * Convenience method for applying credentials to a request
     *
     * @param method the request to add the credentials to
     * @param username the username to add
     */
    private static void setAuth(final AbstractHttpMessage method, final String username) {
        final String creds = username + ":password";
        final String encCreds = new String(Base64.encodeBase64(creds.getBytes()));
        final String basic = "Basic " + encCreds;
        method.setHeader("Authorization", basic);
    }

    @Test
    public void scenario1() throws IOException {
        final String testObj = ingestObj("/rest/webacl_box1");

        final String acl1 = ingestAcl("fedoraAdmin", "/acls/01/acl.ttl",
                                      testObj + "/fcr:acl");
        final String aclLink = Link.fromUri(acl1).rel("acl").build().toString();

        final HttpGet request = getObjMethod(testObj.replace(serverAddress, ""));
        assertEquals("Anonymous can read " + testObj, HttpStatus.SC_FORBIDDEN, getStatus(request));

        setAuth(request, "user01");
        try (final CloseableHttpResponse response = execute(request)) {
            assertEquals("User 'user01' can't read" + testObj, HttpStatus.SC_OK, getStatus(response));
            // This gets the Link headers and filters for the correct one (aclLink::equals) defined above.
            final Optional<String> header = stream(response.getHeaders("Link")).map(Header::getValue)
                    .filter(aclLink::equals).findFirst();
            // So you either have the correct Link header or you get nothing.
            assertTrue("Missing Link header", header.isPresent());
        }

        final String childObj = ingestObj("/rest/webacl_box1/child");
        final HttpGet getReq = getObjMethod(childObj.replace(serverAddress, ""));
        setAuth(getReq, "user01");
        try (final CloseableHttpResponse response = execute(getReq)) {
            assertEquals("User 'user01' can't read child of " + testObj, HttpStatus.SC_OK, getStatus(response));
        }
    }

    @Test
    public void scenario2() throws IOException {
        final String id = "/rest/box/bag/collection";
        final String testObj = ingestObj(id);
        ingestAcl("fedoraAdmin", "/acls/02/acl.ttl", testObj + "/fcr:acl");

        logger.debug("Anonymous can not read " + testObj);
        final HttpGet requestGet = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet));

        logger.debug("GroupId 'Editors' can read " + testObj);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "jones");
        requestGet2.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));

        logger.debug("Anonymous cannot write " + testObj);
        final HttpPatch requestPatch = patchObjMethod(id);
        requestPatch.setEntity(new StringEntity("INSERT { <> <" + title.getURI() + "> \"Test title\" . } WHERE {}"));
        requestPatch.setHeader("Content-type", "application/sparql-update");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        logger.debug("Editors can write " + testObj);
        final HttpPatch requestPatch2 = patchObjMethod(id);
        setAuth(requestPatch2, "jones");
        requestPatch2.setHeader("some-header", "Editors");
        requestPatch2.setEntity(
                new StringEntity("INSERT { <> <" + title.getURI() + "> \"Different title\" . } WHERE {}"));
        requestPatch2.setHeader("Content-type", "application/sparql-update");
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch2));
    }

    @Test
    public void scenario3() throws IOException {
        final String idDark = "/rest/dark/archive";
        final String idLight = "/rest/dark/archive/sunshine";
        final String testObj = ingestObj(idDark);
        final String testObj2 = ingestObjWithACL(idLight, "/acls/03/acl.ttl");
        ingestAcl("fedoraAdmin", "/acls/03/acl.ttl", testObj + "/fcr:acl");

        logger.debug("Anonymous can't read " + testObj);
        final HttpGet requestGet = getObjMethod(idDark);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet));

        logger.debug("Restricted can read " + testObj);
        final HttpGet requestGet2 = getObjMethod(idDark);
        setAuth(requestGet2, "jones");
        requestGet2.setHeader("some-header", "Restricted");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));

        logger.debug("Anonymous can read " + testObj2);
        final HttpGet requestGet3 = getObjMethod(idLight);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet3));

        logger.debug("Restricted can read " + testObj2);
        final HttpGet requestGet4 = getObjMethod(idLight);
        setAuth(requestGet4, "jones");
        requestGet4.setHeader("some-header", "Restricted");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet4));
    }

    @Test
    public void scenario4() throws IOException {
        final String id = "/rest/public_collection";
        final String testObj = ingestObjWithACL(id, "/acls/04/acl.ttl");

        logger.debug("Anonymous can read " + testObj);
        final HttpGet requestGet = getObjMethod(id);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("Editors can read " + testObj);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "jones");
        requestGet2.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));

        logger.debug("Smith can access " + testObj);
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "smith");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet3));

        logger.debug("Anonymous can't write " + testObj);
        final HttpPatch requestPatch = patchObjMethod(id);
        requestPatch.setHeader("Content-type", "application/sparql-update");
        requestPatch.setEntity(new StringEntity("INSERT { <> <" + title.getURI() + "> \"Change title\" . } WHERE {}"));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        logger.debug("Editors can write " + testObj);
        final HttpPatch requestPatch2 = patchObjMethod(id);
        requestPatch2.setHeader("Content-type", "application/sparql-update");
        requestPatch2.setEntity(new StringEntity("INSERT { <> <" + title.getURI() + "> \"New title\" . } WHERE {}"));
        setAuth(requestPatch2, "jones");
        requestPatch2.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch2));

        logger.debug("Editors can create (PUT) child objects of " + testObj);
        final HttpPut requestPut1 = putObjMethod(id + "/child1");
        setAuth(requestPut1, "jones");
        requestPut1.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_CREATED, getStatus(requestPut1));

        final HttpGet requestGet4 = getObjMethod(id + "/child1");
        setAuth(requestGet4, "jones");
        requestGet4.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet4));

        logger.debug("Editors can create (POST) child objects of " + testObj);
        final HttpPost requestPost1 = postObjMethod(id);
        requestPost1.addHeader("Slug", "child2");
        setAuth(requestPost1, "jones");
        requestPost1.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_CREATED, getStatus(requestPost1));

        final HttpGet requestGet5 = getObjMethod(id + "/child2");
        setAuth(requestGet5, "jones");
        requestGet5.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet5));

        logger.debug("Editors can create nested child objects of " + testObj);
        final HttpPut requestPut2 = putObjMethod(id + "/a/b/c/child");
        setAuth(requestPut2, "jones");
        requestPut2.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_CREATED, getStatus(requestPut2));

        final HttpGet requestGet6 = getObjMethod(id + "/a/b/c/child");
        setAuth(requestGet6, "jones");
        requestGet6.setHeader("some-header", "Editors");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet6));

        logger.debug("Smith can't write " + testObj);
        final HttpPatch requestPatch3 = patchObjMethod(id);
        requestPatch3.setHeader("Content-type", "application/sparql-update");
        requestPatch3.setEntity(
                new StringEntity("INSERT { <> <" + title.getURI() + "> \"Different title\" . } WHERE {}"));
        setAuth(requestPatch3, "smith");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch3));
    }

    @Test
    public void scenario5() throws IOException {
        final String idPublic = "/rest/mixedCollection/publicObj";
        final String idPrivate = "/rest/mixedCollection/privateObj";
        ingestObjWithACL("/rest/mixedCollection", "/acls/05/acl.ttl");
        final String publicObj = ingestObj(idPublic);
        final String privateObj = ingestObj(idPrivate);
        final HttpPatch patch = patchObjMethod(idPublic);

        setAuth(patch, "fedoraAdmin");
        patch.setHeader("Content-type", "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <> a <http://example.com/terms#publicImage> . } WHERE {}"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patch));


        logger.debug("Anonymous can see eg:publicImage " + publicObj);
        final HttpGet requestGet = getObjMethod(idPublic);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("Anonymous can't see other resource " + privateObj);
        final HttpGet requestGet2 = getObjMethod(idPrivate);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet2));

        logger.debug("Admins can see eg:publicImage " + publicObj);
        final HttpGet requestGet3 = getObjMethod(idPublic);
        setAuth(requestGet3, "jones");
        requestGet3.setHeader("some-header", "Admins");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet3));

        logger.debug("Admins can see others" + privateObj);
        final HttpGet requestGet4 = getObjMethod(idPrivate);
        setAuth(requestGet4, "jones");
        requestGet4.setHeader("some-header", "Admins");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet4));
    }

    @Ignore("Content-type with charset causes it to be a binary - FCREPO-3312")
    @Test
    public void scenario9() throws IOException {
        final String idPublic = "/rest/anotherCollection/publicObj";
        final String groups = "/rest/group";
        final String fooGroup = groups + "/foo";
        final String testObj = ingestObj("/rest/anotherCollection");
        final String publicObj = ingestObj(idPublic);

        final HttpPut request = putObjMethod(fooGroup);
        setAuth(request, "fedoraAdmin");

        final InputStream file = this.getClass().getResourceAsStream("/acls/09/group.ttl");
        final InputStreamEntity fileEntity = new InputStreamEntity(file);
        request.setEntity(fileEntity);
        request.setHeader("Content-Type", "text/turtle;charset=UTF-8");

        assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(request));

        ingestAcl("fedoraAdmin", "/acls/09/acl.ttl", testObj + "/fcr:acl");

        logger.debug("Person1 can see object " + publicObj);
        final HttpGet requestGet1 = getObjMethod(idPublic);
        setAuth(requestGet1, "person1");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet1));

        logger.debug("Person2 can see object " + publicObj);
        final HttpGet requestGet2 = getObjMethod(idPublic);
        setAuth(requestGet2, "person2");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));

        logger.debug("Person3 user cannot see object " + publicObj);
        final HttpGet requestGet3 = getObjMethod(idPublic);
        setAuth(requestGet3, "person3");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet3));
    }

    /**
     * Test cases to verify authorization with only acl:Append mode configured
     * in the acl authorization of an resource.
     * Tests:
     *  1. Deny(403) on GET.
     *  2. Allow(204) on PATCH.
     *  3. Deny(403) on DELETE.
     *  4. Deny(403) on PATCH with SPARQL DELETE statements.
     *  5. Allow(400) on PATCH with empty SPARQL content.
     *  6. Deny(403) on PATCH with non-SPARQL content.
     *
     * @throws IOException thrown from injestObj() or *ObjMethod() calls
     */
    @Test
    public void scenario18Test1() throws IOException {
        final String testObj = ingestObj("/rest/append_only_resource");
        final String id = "/rest/append_only_resource/" + getRandomUniqueId();
        ingestObj(id);

        logger.debug("user18 can read (has ACL:READ): {}", id);
        final HttpGet requestGet = getObjMethod(id);
        setAuth(requestGet, "user18");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("user18 can't append (no ACL): {}", id);
        final HttpPatch requestPatch = patchObjMethod(id);
        setAuth(requestPatch, "user18");
        requestPatch.setHeader("Content-type", "application/sparql-update");
        requestPatch.setEntity(new StringEntity("INSERT { <> <" + title.getURI() + "> \"Test title\" . } WHERE {}"));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        logger.debug("user18 can't delete (no ACL): {}", id);
        final HttpDelete requestDelete = deleteObjMethod(id);
        setAuth(requestDelete, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestDelete));

        ingestAcl("fedoraAdmin", "/acls/18/append-only-acl.ttl", testObj + "/fcr:acl");

        logger.debug("user18 still can't read (ACL append): {}", id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet));

        logger.debug("user18 can patch - SPARQL INSERTs (ACL append): {}", id);
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch));

        // Alter the Content-type to include a character set, to ensure correct matching.
        requestPatch.setHeader("Content-type", "application/sparql-update; charset=UTF-8");
        logger.debug("user18 can patch - SPARQL INSERTs (ACL append with charset): {}", id);
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch));

        logger.debug("user18 still can't delete (ACL append): {}", id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestDelete));

        requestPatch.setEntity(new StringEntity("DELETE { <> <" + title.getURI() + "> \"Test title\" . } WHERE {}"));

        logger.debug("user18 can not patch - SPARQL DELETEs (ACL append): {}", id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        requestPatch.setEntity(null);

        logger.debug("user18 can patch (is authorized, but bad request) - Empty SPARQL (ACL append): {}", id);
        assertEquals(HttpStatus.SC_BAD_REQUEST, getStatus(requestPatch));

        requestPatch.setHeader("Content-type", null);

        logger.debug("user18 can not patch - Non SPARQL (ACL append): {}", id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

    }

    /**
     * Test cases to verify authorization with acl:Read and acl:Append modes
     * configured in the acl authorization of an resource.
     * Tests:
     *  1. Allow(200) on GET.
     *  2. Allow(204) on PATCH.
     *  3. Deny(403) on DELETE.
     *
     * @throws IOException thrown from called functions within this function
     */
    @Test
    public void scenario18Test2() throws IOException {
        final String testObj = ingestObj("/rest/read_append_resource");

        final String id = "/rest/read_append_resource/" + getRandomUniqueId();
        ingestObj(id);

        logger.debug("user18 can read (has ACL:READ): {}", id);
        final HttpGet requestGet = getObjMethod(id);
        setAuth(requestGet, "user18");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("user18 can't append (no ACL): {}", id);
        final HttpPatch requestPatch = patchObjMethod(id);
        setAuth(requestPatch, "user18");
        requestPatch.setHeader("Content-type", "application/sparql-update");
        requestPatch.setEntity(new StringEntity(
                "INSERT { <> <" + title.getURI() + "> \"some title\" . } WHERE {}"));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        ingestAcl("fedoraAdmin", "/acls/18/read-append-acl.ttl", testObj + "/fcr:acl");

        logger.debug("user18 can't delete (no ACL): {}", id);
        final HttpDelete requestDelete = deleteObjMethod(id);
        setAuth(requestDelete, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestDelete));

        logger.debug("user18 can read (ACL read, append): {}", id);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("user18 can append (ACL read, append): {}", id);
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch));

        logger.debug("user18 still can't delete (ACL read, append): {}", id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestDelete));
    }

    /**
     * Test cases to verify authorization with acl:Read, acl:Append and
     * acl:Write modes configured in the acl authorization of an resource.
     * Tests:
     *  1. Allow(200) on GET.
     *  2. Allow(204) on PATCH.
     *  3. Allow(204) on DELETE.
     *
     * @throws IOException from functions called from this function
     */
    @Test
    public void scenario18Test3() throws IOException {
        final String testObj = ingestObj("/rest/read_append_write_resource");

        final String id = "/rest/read_append_write_resource/" + getRandomUniqueId();
        ingestObj(id);

        logger.debug("user18 can read (has ACL:READ): {}", id);
        final HttpGet requestGet = getObjMethod(id);
        setAuth(requestGet, "user18");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("user18 can't append (no ACL): {}", id);
        final HttpPatch requestPatch = patchObjMethod(id);
        setAuth(requestPatch, "user18");
        requestPatch.setHeader("Content-type", "application/sparql-update");
        requestPatch.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"some title\" . } WHERE {}"));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        logger.debug("user18 can't delete (no ACL): {}", id);
        final HttpDelete requestDelete = deleteObjMethod(id);
        setAuth(requestDelete, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestDelete));

        ingestAcl("fedoraAdmin", "/acls/18/read-append-write-acl.ttl", testObj + "/fcr:acl");

        logger.debug("user18 can read (ACL read, append, write): {}", id);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet));

        logger.debug("user18 can append (ACL read, append, write): {}", id);
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch));

        logger.debug("user18 can delete (ACL read, append, write): {}", id);
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestDelete));
    }

    @Test
    public void testAccessToRoot() throws IOException {
        final String id = "/rest/" + getRandomUniqueId();
        final String testObj = ingestObj(id);

        logger.debug("Anonymous can read (has ACL:READ): {}", id);
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet1));

        logger.debug("Can username 'user06a' read {} (has ACL:READ)", id);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "user06a");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));

        logger.debug("Can username 'notuser06b' read {} (has ACL:READ)", id);
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "user06b");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet3));

        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/test-root-authorization2.ttl");
        logger.debug("Can username 'user06a' read {} (overridden system ACL)", id);
        final HttpGet requestGet4 = getObjMethod(id);
        setAuth(requestGet4, "user06a");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet4));
        System.clearProperty(ROOT_AUTHORIZATION_PROPERTY);

        // Add ACL to root
        final String rootURI = getObjMethod("/rest").getURI().toString();
        ingestAcl("fedoraAdmin", "/acls/06/acl.ttl", rootURI + "/fcr:acl");

        logger.debug("Anonymous still can't read (ACL present)");
        final HttpGet requestGet5 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet5));

        logger.debug("Can username 'user06a' read {} (ACL present)", testObj);
        final HttpGet requestGet6 = getObjMethod(id);
        setAuth(requestGet6, "user06a");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet6));

        logger.debug("Can username 'user06b' read {} (ACL present)", testObj);
        final HttpGet requestGet7 = getObjMethod(id);
        setAuth(requestGet7, "user06b");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet7));
    }

    @Test
    public void scenario21TestACLNotForInheritance() throws IOException {
        final String parentPath = "/rest/resource_acl_no_inheritance";
        // Ingest ACL with no acl:default statement to the parent resource
        ingestObjWithACL(parentPath, "/acls/21/acl.ttl");

        final String id = parentPath + "/" + getRandomUniqueId();
        final String testObj = ingestObj(id);


        // Test the parent ACL with no acl:default is applied for the parent resource authorization.
        final HttpGet requestGet1 = getObjMethod(parentPath);
        setAuth(requestGet1, "user21");
        assertEquals("Agent user21 can't read resource " + parentPath + " with its own ACL!",
                HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpGet requestGet2 = getObjMethod(id);
        assertEquals("Agent user21 inherits read permission from parent ACL to read resource " + testObj + "!",
                HttpStatus.SC_OK, getStatus(requestGet2));

        // Test the default root ACL is inherited for authorization while the parent ACL with no acl:default is ignored
        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/test-root-authorization2.ttl");
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "user06a");
        assertEquals("Agent user06a can't inherit read persmssion from root ACL to read resource " + testObj + "!",
                HttpStatus.SC_OK, getStatus(requestGet3));
    }

    @Test
    public void scenario22TestACLAuthorizationNotForInheritance() throws IOException {
        final String parentPath = "/rest/resource_mix_acl_default";
        final String parentObj = ingestObj(parentPath);

        final String id = parentPath + "/" + getRandomUniqueId();
        final String testObj = ingestObj(id);

        // Ingest ACL with mix acl:default authorization to the parent resource
        ingestAcl("fedoraAdmin", "/acls/22/acl.ttl", parentObj + "/fcr:acl");

        // Test the parent ACL is applied for the parent resource authorization.
        final HttpGet requestGet1 = getObjMethod(parentPath);
        setAuth(requestGet1, "user22a");
        assertEquals("Agent user22a can't read resource " + parentPath + " with its own ACL!",
                HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpGet requestGet2 = getObjMethod(parentPath);
        setAuth(requestGet2, "user22b");
        assertEquals("Agent user22b can't read resource " + parentPath + " with its own ACL!",
                HttpStatus.SC_OK, getStatus(requestGet1));

        // Test the parent ACL is applied for the parent resource authorization.
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "user22a");
        assertEquals("Agent user22a inherits read permission from parent ACL to read resource " + testObj + "!",
                HttpStatus.SC_FORBIDDEN, getStatus(requestGet3));

        final HttpGet requestGet4 = getObjMethod(id);
        setAuth(requestGet4, "user22b");
        assertEquals("Agent user22b can't inherits read permission from parent ACL to read resource " + testObj + "!",
                HttpStatus.SC_OK, getStatus(requestGet4));
    }

    @Test
    public void testAccessToBinary() throws IOException {
        // Block access to "book"
        final String idBook = "/rest/book";
        final String bookURI = ingestObj(idBook);

        // Open access datastream, "file"
        final String id = idBook + "/file";
        final String testObj = ingestDatastream(idBook, "file");
        ingestAcl("fedoraAdmin", "/acls/07/acl.ttl", bookURI + "/fcr:acl");

        logger.debug("Anonymous can't read");
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet1));

        logger.debug("Can username 'user07' read {}", testObj);
        final HttpGet requestGet2 = getObjMethod(id);

        setAuth(requestGet2, "user07");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
    public void testAccessToVersionedResources() throws IOException {
        final String idVersion = "/rest/versionResource";
        final String idVersionUri = ingestObj(idVersion);

        final HttpPatch requestPatch1 = patchObjMethod(idVersion);
        setAuth(requestPatch1, "fedoraAdmin");
        requestPatch1.addHeader("Content-type", "application/sparql-update");
        requestPatch1.setEntity(
                new StringEntity("PREFIX pcdm: <http://pcdm.org/models#> INSERT { <> a pcdm:Object } WHERE {}"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch1));

        ingestAcl("fedoraAdmin", "/acls/10/acl.ttl", idVersionUri + "/fcr:acl");

        final HttpGet requestGet1 = getObjMethod(idVersion);
        setAuth(requestGet1, "user10");
        assertEquals("user10 can't read object", HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpPost requestPost1 = postObjMethod(idVersion + "/fcr:versions");
        setAuth(requestPost1, "fedoraAdmin");
        assertEquals("Unable to create a new version", HttpStatus.SC_CREATED, getStatus(requestPost1));

        final HttpGet requestGet2 = getObjMethod(idVersion);
        setAuth(requestGet2, "user10");
        assertEquals("user10 can't read versioned object", HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
    public void testDelegatedUserAccess() throws IOException {
        logger.debug("testing delegated authentication");
        final String targetPath = "/rest/foo";
        final String targetResource = ingestObj(targetPath);

        ingestAcl("fedoraAdmin", "/acls/11/acl.ttl", targetResource + "/fcr:acl");

        final HttpGet adminGet = getObjMethod(targetPath);
        setAuth(adminGet, "fedoraAdmin");
        assertEquals("admin can read object", HttpStatus.SC_OK, getStatus(adminGet));

        final HttpGet adminDelegatedGet = getObjMethod(targetPath);
        setAuth(adminDelegatedGet, "fedoraAdmin");
        adminDelegatedGet.addHeader("On-Behalf-Of", "user11");
        assertEquals("delegated user can read object", HttpStatus.SC_OK, getStatus(adminDelegatedGet));

        final HttpGet adminUnauthorizedDelegatedGet = getObjMethod(targetPath);
        setAuth(adminUnauthorizedDelegatedGet, "fedoraAdmin");
        adminUnauthorizedDelegatedGet.addHeader("On-Behalf-Of", "fakeuser");
        assertEquals("delegated fakeuser cannot read object", HttpStatus.SC_FORBIDDEN,
                getStatus(adminUnauthorizedDelegatedGet));

        final HttpGet adminDelegatedGet2 = getObjMethod(targetPath);
        setAuth(adminDelegatedGet2, "fedoraAdmin");
        adminDelegatedGet2.addHeader("On-Behalf-Of", "info:user/user2");
        assertEquals("delegated user can read object", HttpStatus.SC_OK, getStatus(adminDelegatedGet2));

        final HttpGet adminUnauthorizedDelegatedGet2 = getObjMethod(targetPath);
        setAuth(adminUnauthorizedDelegatedGet2, "fedoraAdmin");
        adminUnauthorizedDelegatedGet2.addHeader("On-Behalf-Of", "info:user/fakeuser");
        assertEquals("delegated fakeuser cannot read object", HttpStatus.SC_FORBIDDEN,
                getStatus(adminUnauthorizedDelegatedGet2));

        // Now test with the system property in effect
        System.setProperty(USER_AGENT_BASE_URI_PROPERTY, "info:user/");
        System.setProperty(GROUP_AGENT_BASE_URI_PROPERTY, "info:group/");

        final HttpGet adminDelegatedGet3 = getObjMethod(targetPath);
        setAuth(adminDelegatedGet3, "fedoraAdmin");
        adminDelegatedGet3.addHeader("On-Behalf-Of", "info:user/user2");
        assertEquals("delegated user can read object", HttpStatus.SC_OK, getStatus(adminDelegatedGet3));

        final HttpGet adminUnauthorizedDelegatedGet3 = getObjMethod(targetPath);
        setAuth(adminUnauthorizedDelegatedGet3, "fedoraAdmin");
        adminUnauthorizedDelegatedGet3.addHeader("On-Behalf-Of", "info:user/fakeuser");
        assertEquals("delegated fakeuser cannot read object", HttpStatus.SC_FORBIDDEN,
                getStatus(adminUnauthorizedDelegatedGet3));

        System.clearProperty(USER_AGENT_BASE_URI_PROPERTY);
        System.clearProperty(GROUP_AGENT_BASE_URI_PROPERTY);
    }

    @Test
    public void testAccessByUriToVersionedResources() throws IOException {
        final String idVersionPath = "rest/versionResourceUri";
        final String idVersionResource = ingestObj(idVersionPath);

        ingestAcl("fedoraAdmin", "/acls/12/acl.ttl", idVersionResource + "/fcr:acl");

        final HttpGet requestGet1 = getObjMethod(idVersionPath);
        setAuth(requestGet1, "user12");
        assertEquals("testuser can't read object", HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpPost requestPost1 = postObjMethod(idVersionPath + "/fcr:versions");
        setAuth(requestPost1, "user12");
        final String mementoLocation;
        try (final CloseableHttpResponse response = execute(requestPost1)) {
            assertEquals("Unable to create a new version", HttpStatus.SC_CREATED, getStatus(response));
            mementoLocation = getLocation(response);
        }

        final HttpGet requestGet2 = new HttpGet(mementoLocation);
        setAuth(requestGet2, "user12");
        assertEquals("testuser can't read versioned object", HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
    public void testAgentAsUri() throws IOException {
        final String id = "/rest/" + getRandomUniqueId();
        final String testObj = ingestObj(id);

        logger.debug("Anonymous can read (has ACL:READ): {}", id);
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet1));

        logger.debug("Can username 'smith123' read {} (no ACL)", id);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "smith123");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));

        System.setProperty(USER_AGENT_BASE_URI_PROPERTY, "info:user/");
        System.setProperty(GROUP_AGENT_BASE_URI_PROPERTY, "info:group/");

        logger.debug("Can username 'smith123' read {} (overridden system ACL)", id);
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "smith123");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet3));

        logger.debug("Can username 'group123' read {} (overridden system ACL)", id);
        final HttpGet requestGet4 = getObjMethod(id);
        setAuth(requestGet4, "group123");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet4));

        System.clearProperty(USER_AGENT_BASE_URI_PROPERTY);
        System.clearProperty(GROUP_AGENT_BASE_URI_PROPERTY);

        // Add ACL to object
        ingestAcl("fedoraAdmin", "/acls/16/acl.ttl", testObj + "/fcr:acl");

        logger.debug("Anonymous still can't read (ACL present)");
        final HttpGet requestGet5 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet5));

        logger.debug("Can username 'smith123' read {} (ACL present, no system properties)", testObj);
        final HttpGet requestGet6 = getObjMethod(id);
        setAuth(requestGet6, "smith123");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet6));

        System.setProperty(USER_AGENT_BASE_URI_PROPERTY, "info:user/");
        System.setProperty(GROUP_AGENT_BASE_URI_PROPERTY, "info:group/");

        logger.debug("Can username 'smith123' read {} (ACL, system properties present)", id);
        final HttpGet requestGet7 = getObjMethod(id);
        setAuth(requestGet7, "smith123");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet7));

        logger.debug("Can groupname 'group123' read {} (ACL, system properties present)", id);
        final HttpGet requestGet8 = getObjMethod(id);
        setAuth(requestGet8, "group123");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet8));

        System.clearProperty(USER_AGENT_BASE_URI_PROPERTY);
        System.clearProperty(GROUP_AGENT_BASE_URI_PROPERTY);
    }

    @Test
    public void testRegisterNamespace() throws IOException {
        final String testObj = ingestObj("/rest/test_namespace");
        ingestAcl("fedoraAdmin", "/acls/13/acl.ttl", testObj + "/fcr:acl");

        final String id = "/rest/test_namespace/" + getRandomUniqueId();
        ingestObj(id);

        final HttpPatch patchReq = patchObjMethod(id);
        setAuth(patchReq, "user13");
        patchReq.addHeader("Content-type", "application/sparql-update");
        patchReq.setEntity(new StringEntity("PREFIX novel: <info://" + getRandomUniqueId() + ">\n"
                + "INSERT DATA { <> novel:value 'test' }"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));
    }

    @Test
    public void testRegisterNodeType() throws IOException {
        final String testObj = ingestObj("/rest/test_nodetype");
        ingestAcl("fedoraAdmin", "/acls/14/acl.ttl", testObj + "/fcr:acl");

        final String id = "/rest/test_nodetype/" + getRandomUniqueId();
        ingestObj(id);

        final HttpPatch patchReq = patchObjMethod(id);
        setAuth(patchReq, "user14");
        patchReq.addHeader("Content-type", "application/sparql-update");
        patchReq.setEntity(new StringEntity("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
                + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "INSERT DATA { <> rdf:type dc:type }"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));
    }


    @Test
    public void testDeletePropertyAsUser() throws IOException {
        final String testObj = ingestObj("/rest/test_delete");
        ingestAcl("fedoraAdmin", "/acls/15/acl.ttl", testObj + "/fcr:acl");

        final String id = "/rest/test_delete/" + getRandomUniqueId();
        ingestObj(id);

        HttpPatch patchReq = patchObjMethod(id);
        setAuth(patchReq, "user15");
        patchReq.addHeader("Content-type", "application/sparql-update");
        patchReq.setEntity(new StringEntity("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
                + "INSERT DATA { <> dc:title 'title' . " +
                "                <> dc:rights 'rights' . }"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));

        patchReq = patchObjMethod(id);
        setAuth(patchReq, "user15");
        patchReq.addHeader("Content-type", "application/sparql-update");
        patchReq.setEntity(new StringEntity("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
                + "DELETE { <> dc:title ?any . } WHERE { <> dc:title ?any . }"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));

        patchReq = patchObjMethod(id);
        setAuth(patchReq, "notUser15");
        patchReq.addHeader("Content-type", "application/sparql-update");
        patchReq.setEntity(new StringEntity("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
                + "DELETE { <> dc:rights ?any . } WHERE { <> dc:rights ?any . }"));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(patchReq));
    }

    @Test
    public void testHeadWithReadOnlyUser() throws IOException {
        final String testObj = ingestObj("/rest/test_head");
        ingestAcl("fedoraAdmin", "/acls/19/acl.ttl", testObj + "/fcr:acl");

        final HttpHead headReq = new HttpHead(testObj);
        setAuth(headReq, "user19");
        assertEquals(HttpStatus.SC_OK, getStatus(headReq));
    }

    @Test
    public void testOptionsWithReadOnlyUser() throws IOException {
        final String testObj = ingestObj("/rest/test_options");
        ingestAcl("fedoraAdmin", "/acls/20/acl.ttl", testObj + "/fcr:acl");

        final HttpOptions optionsReq = new HttpOptions(testObj);
        setAuth(optionsReq, "user20");
        assertEquals(HttpStatus.SC_OK, getStatus(optionsReq));
    }

    private static HttpResponse HEAD(final String requestURI) throws IOException {
        return HEAD(requestURI, "fedoraAdmin");
    }

    private static HttpResponse HEAD(final String requestURI, final String username) throws IOException {
        final HttpHead req = new HttpHead(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    private static HttpResponse PUT(final String requestURI) throws IOException {
        return PUT(requestURI, "fedoraAdmin");
    }

    private static HttpResponse PUT(final String requestURI, final String username) throws IOException {
        final HttpPut req = new HttpPut(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    private static HttpResponse DELETE(final String requestURI, final String username) throws IOException {
        final HttpDelete req = new HttpDelete(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    private static HttpResponse GET(final String requestURI, final String username) throws IOException {
        final HttpGet req = new HttpGet(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    private static HttpResponse PATCH(final String requestURI, final HttpEntity body, final String username)
            throws IOException {
        final HttpPatch req = new HttpPatch(requestURI);
        setAuth(req, username);
        if (body != null) {
            req.setEntity(body);
        }
        return execute(req);
    }

    private static String getLink(final HttpResponse res) {
        for (final Header h : res.getHeaders("Link")) {
            final HeaderElement link = h.getElements()[0];
            for (final NameValuePair param : link.getParameters()) {
                if (param.getName().equals("rel") && param.getValue().equals("acl")) {
                    return link.getName().replaceAll("^<|>$", "");
                }
            }
        }
        return null;
    }

    private String ingestObjWithACL(final String path, final String aclResourcePath) throws IOException {
        final String newURI = ingestObj(path);
        final HttpResponse res = HEAD(newURI);
        final String aclURI = getLink(res);

        logger.debug("Creating ACL at {}", aclURI);
        ingestAcl("fedoraAdmin", aclResourcePath, aclURI);

        return newURI;
    }

    @Test
    public void testControl() throws IOException {
        final String controlObj = ingestObjWithACL("/rest/control", "/acls/25/control.ttl");
        final String readwriteObj = ingestObjWithACL("/rest/readwrite", "/acls/25/readwrite.ttl");

        final String rwChildACL = getLink(PUT(readwriteObj + "/child"));
        assertEquals(SC_FORBIDDEN, getStatus(HEAD(rwChildACL, "testuser")));
        assertEquals(SC_FORBIDDEN, getStatus(GET(rwChildACL, "testuser")));
        assertEquals(SC_FORBIDDEN, getStatus(PUT(rwChildACL, "testuser")));
        assertEquals(SC_FORBIDDEN, getStatus(DELETE(rwChildACL, "testuser")));

        final String controlChildACL = getLink(PUT(controlObj + "/child"));
        assertEquals(SC_NOT_FOUND, getStatus(HEAD(controlChildACL, "testuser")));
        assertEquals(SC_NOT_FOUND, getStatus(GET(controlChildACL, "testuser")));

        ingestAcl("testuser", "/acls/25/child-control.ttl", controlChildACL);
        final StringEntity sparqlUpdate = new StringEntity(
                "PREFIX acl: <http://www.w3.org/ns/auth/acl#>  INSERT { <#restricted> acl:mode acl:Read } WHERE { }",
                ContentType.create("application/sparql-update"));
        assertEquals(SC_NO_CONTENT, getStatus(PATCH(controlChildACL, sparqlUpdate, "testuser")));

        assertEquals(SC_NO_CONTENT, getStatus(DELETE(controlChildACL, "testuser")));
    }

    @Test
    public void testAppendOnlyToContainer() throws IOException {
        final String testObj = ingestObj("/rest/test_append");
        ingestAcl("fedoraAdmin", "/acls/23/acl.ttl", testObj + "/fcr:acl");
        final String username = "user23";

        final HttpOptions optionsReq = new HttpOptions(testObj);
        setAuth(optionsReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(optionsReq));

        final HttpHead headReq = new HttpHead(testObj);
        setAuth(headReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(headReq));

        final HttpGet getReq = new HttpGet(testObj);
        setAuth(getReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(getReq));

        final HttpPut putReq = new HttpPut(testObj);
        setAuth(putReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(putReq));

        final HttpDelete deleteReq = new HttpDelete(testObj);
        setAuth(deleteReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteReq));

        final HttpPost postReq = new HttpPost(testObj);
        setAuth(postReq, username);
        assertEquals(HttpStatus.SC_CREATED, getStatus(postReq));

        final String[] legalSPARQLQueries = new String[] {
            "INSERT DATA { <> <http://purl.org/dc/terms/title> \"Test23\" . }",
            "INSERT { <> <http://purl.org/dc/terms/alternative> \"Test XXIII\" . } WHERE {}",
            "DELETE {} INSERT { <> <http://purl.org/dc/terms/description> \"Test append only\" . } WHERE {}"
        };
        for (final String query : legalSPARQLQueries) {
            final HttpPatch patchReq = new HttpPatch(testObj);
            setAuth(patchReq, username);
            patchReq.setEntity(new StringEntity(query));
            patchReq.setHeader("Content-Type", "application/sparql-update");
            logger.debug("Testing SPARQL update: {}", query);
            assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));
        }

        final String[] illegalSPARQLQueries = new String[] {
            "DELETE DATA { <> <http://purl.org/dc/terms/title> \"Test23\" . }",
            "DELETE { <> <http://purl.org/dc/terms/alternative> \"Test XXIII\" . } WHERE {}",
            "DELETE { <> <http://purl.org/dc/terms/description> \"Test append only\" . } INSERT {} WHERE {}"
        };
        for (final String query : illegalSPARQLQueries) {
            final HttpPatch patchReq = new HttpPatch(testObj);
            setAuth(patchReq, username);
            patchReq.setEntity(new StringEntity(query));
            patchReq.setHeader("Content-Type", "application/sparql-update");
            logger.debug("Testing SPARQL update: {}", query);
            assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(patchReq));
        }
        final String[] allowedDeleteSPARQLQueries = new String[] {
            "DELETE DATA {}",
            "DELETE { } WHERE {}",
            "DELETE { } INSERT {} WHERE {}"
        };
        for (final String query : allowedDeleteSPARQLQueries) {
            final HttpPatch patchReq = new HttpPatch(testObj);
            setAuth(patchReq, username);
            patchReq.setEntity(new StringEntity(query));
            patchReq.setHeader("Content-Type", "application/sparql-update");
            logger.debug("Testing SPARQL update: {}", query);
            assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));
        }

    }

    @Test
    public void testAppendOnlyToBinary() throws IOException {
        final String testObj = ingestBinary("/rest/test_append_binary", new StringEntity("foo"));
        ingestAcl("fedoraAdmin", "/acls/24/acl.ttl", testObj + "/fcr:acl");
        final String username = "user24";

        final HttpOptions optionsReq = new HttpOptions(testObj);
        setAuth(optionsReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(optionsReq));

        final HttpHead headReq = new HttpHead(testObj);
        setAuth(headReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(headReq));

        final HttpGet getReq = new HttpGet(testObj);
        setAuth(getReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(getReq));

        final HttpPut putReq = new HttpPut(testObj);
        setAuth(putReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(putReq));

        final HttpDelete deleteReq = new HttpDelete(testObj);
        setAuth(deleteReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteReq));

        final HttpPost postReq = new HttpPost(testObj);
        setAuth(postReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(postReq));
    }

    @Test
    public void testFoafAgent() throws IOException {
        final String path = ingestObj("/rest/foaf-agent");
        ingestAcl("fedoraAdmin", "/acls/26/foaf-agent.ttl", path + "/fcr:acl");
        final String username = "user1";

        final HttpGet req = new HttpGet(path);

        //NB: Actually no authentication headers should be set for this test
        //since the point of foaf:Agent is to allow unauthenticated access for everyone.
        //However at this time the test integration test server requires callers to
        //authenticate.
        setAuth(req, username);

        assertEquals(HttpStatus.SC_OK, getStatus(req));
    }

    @Test
    public void testAuthenticatedAgent() throws IOException {
        final String path = ingestObj("/rest/authenticated-agent");
        ingestAcl("fedoraAdmin", "/acls/26/authenticated-agent.ttl", path + "/fcr:acl");
        final String username = "user1";

        final HttpGet darkReq = new HttpGet(path);
        setAuth(darkReq, username);
        assertEquals(HttpStatus.SC_OK, getStatus(darkReq));
    }

    @Test
    public void testAgentGroupWithHashUris() throws Exception {
        ingestTurtleResource("fedoraAdmin", "/acls/agent-group-list.ttl",
                             serverAddress + "/rest/agent-group-list");
        //check that the authorized are authorized.
        final String authorized = ingestObj("/rest/agent-group-with-hash-uri-authorized");
        ingestAcl("fedoraAdmin", "/acls/agent-group-with-hash-uri-authorized.ttl", authorized + "/fcr:acl");

        final HttpGet getAuthorized = new HttpGet(authorized);
        setAuth(getAuthorized, "testuser");
        assertEquals(HttpStatus.SC_OK, getStatus(getAuthorized));

        //check that the unauthorized are unauthorized.
        final String unauthorized = ingestObj("/rest/agent-group-with-hash-uri-unauthorized");
        ingestAcl("fedoraAdmin", "/acls/agent-group-with-hash-uri-unauthorized.ttl", unauthorized + "/fcr:acl");

        final HttpGet getUnauthorized = new HttpGet(unauthorized);
        setAuth(getUnauthorized, "testuser");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(getUnauthorized));
    }

    @Test
    public void testAgentGroupWithMembersAsURIs() throws Exception {
        System.setProperty(USER_AGENT_BASE_URI_PROPERTY, "http://example.com/");
        ingestTurtleResource("fedoraAdmin", "/acls/agent-group-list-with-member-uris.ttl",
                             serverAddress + "/rest/agent-group-list-with-member-uris");
        final String authorized = ingestObj("/rest/agent-group-with-vcard-member-as-uri");
        ingestAcl("fedoraAdmin", "/acls/agent-group-with-vcard-member-as-uri.ttl", authorized + "/fcr:acl");
        //check that test user is authorized to write
        final HttpPut childPut = new HttpPut(authorized + "/child");
        setAuth(childPut, "testuser");
        assertEquals(HttpStatus.SC_CREATED, getStatus(childPut));
    }

    @Test
    public void testAgentGroup() throws Exception {
        ingestTurtleResource("fedoraAdmin", "/acls/agent-group-list-flat.ttl",
                             serverAddress + "/rest/agent-group-list-flat");
        //check that the authorized are authorized.
        final String flat = ingestObj("/rest/agent-group-flat");
        ingestAcl("fedoraAdmin", "/acls/agent-group-flat.ttl", flat + "/fcr:acl");

        final HttpGet getFlat = new HttpGet(flat);
        setAuth(getFlat, "testuser");
        assertEquals(HttpStatus.SC_OK, getStatus(getFlat));
    }

    @Test
    public void testAclAppendPermissions() throws Exception {
        final String testObj = ingestBinary("/rest/test-read-append", new StringEntity("foo"));
        ingestAcl("fedoraAdmin", "/acls/27/read-append.ttl", testObj + "/fcr:acl");
        final String username = "user27";

        final HttpOptions optionsReq = new HttpOptions(testObj);
        setAuth(optionsReq, username);
        assertEquals(HttpStatus.SC_OK, getStatus(optionsReq));

        final HttpHead headReq = new HttpHead(testObj);
        setAuth(headReq, username);
        assertEquals(HttpStatus.SC_OK, getStatus(headReq));

        final HttpGet getReq = new HttpGet(testObj);
        setAuth(getReq, username);
        final String descriptionUri;
        try (final CloseableHttpResponse response = execute(getReq)) {
            assertEquals(HttpStatus.SC_OK, getStatus(response));
            descriptionUri = Arrays.stream(response.getHeaders("Link"))
                    .flatMap(header -> Arrays.stream(header.getValue().split(","))).map(linkStr -> Link.valueOf(
                            linkStr))
                    .filter(link -> link.getRels().contains("describedby")).map(link -> link.getUri().toString())
                    .findFirst().orElse(null);
        }


        final HttpPut putReq = new HttpPut(testObj);
        setAuth(putReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(putReq));

        final HttpDelete deleteReq = new HttpDelete(testObj);
        setAuth(deleteReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteReq));

        final HttpPost postReq = new HttpPost(testObj);
        setAuth(postReq, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(postReq));

        if (descriptionUri != null) {
            final HttpOptions optionsDescReq = new HttpOptions(descriptionUri);
            setAuth(optionsDescReq, username);
            assertEquals(HttpStatus.SC_OK, getStatus(optionsDescReq));

            final HttpHead headDescReq = new HttpHead(descriptionUri);
            setAuth(headDescReq, username);
            assertEquals(HttpStatus.SC_OK, getStatus(headDescReq));

            final HttpGet getDescReq = new HttpGet(descriptionUri);
            setAuth(getDescReq, username);
            assertEquals(HttpStatus.SC_OK, getStatus(getDescReq));

            final HttpPut putDescReq = new HttpPut(descriptionUri);
            setAuth(putDescReq, username);
            assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(putDescReq));

            final HttpDelete deleteDescReq = new HttpDelete(descriptionUri);
            setAuth(deleteDescReq, username);
            assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteDescReq));

            final HttpPost postDescReq = new HttpPost(descriptionUri);
            setAuth(postDescReq, username);
            assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(postDescReq));
        }
    }

    @Test
    public void testCreateAclWithAccessToClassForBinary() throws Exception {
        final String id = getRandomUniqueId();
        final String subjectUri = serverAddress + id;
        ingestObj(subjectUri);
        ingestAcl("fedoraAdmin", "/acls/agent-access-to-class.ttl", subjectUri + "/fcr:acl");

        final String binaryUri = ingestBinary("/rest/" + id + "/binary", new StringEntity("foo"));

        final HttpHead headBinary = new HttpHead(binaryUri);
        setAuth(headBinary, "testuser");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(headBinary));

        final HttpHead headDesc = new HttpHead(binaryUri + "/fcr:metadata");
        setAuth(headDesc, "testuser");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(headDesc));

        // Add type to binary
        final HttpPatch requestPatch = patchObjMethod(id + "/binary/fcr:metadata");
        setAuth(requestPatch, "fedoraAdmin");
        final String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>  \n" +
                "INSERT { <> rdf:type foaf:Document } WHERE {}";
        requestPatch.setEntity(new StringEntity(sparql));
        requestPatch.setHeader("Content-type", "application/sparql-update");
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch));

        final HttpHead headBinary2 = new HttpHead(binaryUri);
        setAuth(headBinary2, "testuser");
        assertEquals(HttpStatus.SC_OK, getStatus(headBinary2));

        final HttpHead headDesc2 = new HttpHead(binaryUri + "/fcr:metadata");
        setAuth(headDesc2, "testuser");
        assertEquals(HttpStatus.SC_OK, getStatus(headDesc2));
    }

    @Ignore("Until FCREPO-3310 and FCREPO-3311 are resolved")
    @Test
    public void testIndirectRelationshipForbidden() throws IOException {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String writeableResource = "/rest/" + getRandomUniqueId();
        final String username = "user28";

        final String targetUri = ingestObj(targetResource);

        final String readonlyString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read ;\n" +
                "   acl:accessTo <" + targetResource + "> .";
        ingestAclString(targetUri, readonlyString, "fedoraAdmin");

        // User can read target resource.
        final HttpGet get1 = getObjMethod(targetResource);
        setAuth(get1, username);
        assertEquals(HttpStatus.SC_OK, getStatus(get1));

        // User can't patch target resource.
        final String patch = "INSERT DATA { <> <http://purl.org/dc/elements/1.1/title> \"Changed it\"}";
        final HttpEntity patchEntity = new StringEntity(patch, sparqlContentType);
        try (final CloseableHttpResponse resp = (CloseableHttpResponse) PATCH(targetUri, patchEntity,
                username)) {
            assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(resp));
        }

        // Make a user writable container.
        final String writeableUri = ingestObj(writeableResource);
        final String writeableAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#writeauth> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + writeableResource + "> ;\n" +
                "   acl:default <" + writeableResource + "> .";
        ingestAclString(writeableUri, writeableAcl, "fedoraAdmin");

        // Ensure we can still POST/PUT to writeable resource.
        testCanWrite(writeableResource, username);

        // Try to create indirect container referencing readonly resource with POST.
        final HttpPost userPost = postObjMethod(writeableResource);
        setAuth(userPost, username);
        userPost.addHeader("Link", "<" + INDIRECT_CONTAINER.toString() + ">; rel=type");
        final String indirect = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix example: <http://www.example.org/example1#> .\n" +
                "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                "<> ldp:insertedContentRelation <http://example.org/test#something> ;\n" +
                "ldp:membershipResource <" + targetResource + "> ;\n" +
                "ldp:hasMemberRelation <http://example.org/test#predicateToCreate> ;\n" +
                "dc:title \"The indirect container\" .";
        final HttpEntity indirectEntity = new StringEntity(indirect, turtleContentType);
        userPost.setEntity(indirectEntity);
        userPost.setHeader(CONTENT_TYPE, "text/turtle");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(userPost));

        // Try to create indirect container referencing readonly resource with PUT.
        final String indirectString = getRandomUniqueId();
        final HttpPut userPut = putObjMethod(writeableResource + "/" + indirectString);
        setAuth(userPut, username);
        userPut.addHeader("Link", "<" + INDIRECT_CONTAINER.toString() + ">; rel=type");
        userPut.setEntity(indirectEntity);
        userPut.setHeader(CONTENT_TYPE, "text/turtle");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(userPut));

        // Create an user writeable resource.
        final HttpPost targetPost = postObjMethod(writeableResource);
        setAuth(targetPost, username);
        final String tempTarget;
        try (final CloseableHttpResponse resp = execute(targetPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            tempTarget = getLocation(resp);
        }

        // Try to create indirect container referencing an available resource.
        final String indirect_ok = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix example: <http://www.example.org/example1#> .\n" +
                "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                "<> ldp:insertedContentRelation <http://example.org/test#something> ;\n" +
                "ldp:membershipResource <" + tempTarget + "> ;\n" +
                "ldp:hasMemberRelation <http://example.org/test#predicateToCreate> ;\n" +
                "dc:title \"The indirect container\" .";
        final HttpPost userPatchPost = postObjMethod(writeableResource);
        setAuth(userPatchPost, username);
        userPatchPost.addHeader("Link", "<" + INDIRECT_CONTAINER.toString() + ">; rel=type");
        final HttpEntity in_ok = new StringEntity(indirect_ok, turtleContentType);
        userPatchPost.setEntity(in_ok);
        userPatchPost.setHeader(CONTENT_TYPE, "text/turtle");
        final String indirectUri;
        try (final CloseableHttpResponse resp = execute(userPatchPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            indirectUri = getLocation(resp);
        }

        // Then PATCH to the readonly resource.
        final HttpPatch patchIndirect = new HttpPatch(indirectUri);
        setAuth(patchIndirect, username);
        final String patch_text = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE { <> ldp:membershipResource ?o } \n" +
                "INSERT { <> ldp:membershipResource <" + targetResource + "> } \n" +
                "WHERE { <> ldp:membershipResource ?o }";
        patchIndirect.setEntity(new StringEntity(patch_text, sparqlContentType));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(patchIndirect));

        // Delete the ldp:membershipRelation and add it with INSERT DATA {}
        final String patch_delete_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE DATA { <> ldp:membershipResource <" + tempTarget + "> }";
        final HttpPatch patchIndirect2 = new HttpPatch(indirectUri);
        setAuth(patchIndirect2, username);
        patchIndirect2.setEntity(new StringEntity(patch_delete_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchIndirect2));

        final String patch_insert_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "INSERT DATA { <> ldp:membershipResource <" + targetResource + "> }";
        final HttpPatch patchIndirect3 = new HttpPatch(indirectUri);
        setAuth(patchIndirect3, username);
        patchIndirect3.setEntity(new StringEntity(patch_insert_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(patchIndirect3));

        // Patch the indirect to the readonly target as admin
        final HttpPatch patchAsAdmin = new HttpPatch(indirectUri);
        setAuth(patchAsAdmin, "fedoraAdmin");
        patchAsAdmin.setEntity(new StringEntity(patch_text, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchAsAdmin));

        // Ensure the patching happened.
        final HttpGet verifyGet = new HttpGet(indirectUri);
        setAuth(verifyGet, "fedoraAdmin");
        try (final CloseableHttpResponse response = execute(verifyGet)) {
            final CloseableDataset dataset = getDataset(response);
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Can't find " + targetUri + " in graph",
                    graph.contains(
                            Node.ANY,
                            NodeFactory.createURI(indirectUri),
                            MEMBERSHIP_RESOURCE.asNode(),
                            NodeFactory.createURI(targetUri)
                    )
            );
        }

        // Try to POST a child as user
        final HttpPost postChild = new HttpPost(indirectUri);
        final String postTarget = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> test:something <" + tempTarget + "> .";
        final HttpEntity putPostChild = new StringEntity(postTarget, turtleContentType);
        setAuth(postChild, username);
        postChild.setEntity(putPostChild);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(postChild));

        // Try to PUT a child as user
        final String id = getRandomUniqueId();
        final HttpPut putChild = new HttpPut(indirectUri + "/" + id);
        setAuth(putChild, username);
        putChild.setEntity(putPostChild);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(putChild));

        // Put the child as Admin
        setAuth(putChild, "fedoraAdmin");
        assertEquals(HttpStatus.SC_CREATED, getStatus(putChild));

        // Try to delete the child as user
        final HttpDelete deleteChild = new HttpDelete(indirectUri + "/" + id);
        setAuth(deleteChild, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteChild));

        // Try to delete the indirect container
        final HttpDelete deleteIndirect = new HttpDelete(indirectUri);
        setAuth(deleteIndirect, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteIndirect));

        // Ensure we can still write to the writeable resource.
        testCanWrite(writeableResource, username);

    }

    @Test
    public void testIndirectRelationshipOK() throws IOException {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String writeableResource = "/rest/" + getRandomUniqueId();
        final String username = "user28";

        final String targetUri = ingestObj(targetResource);

        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + targetResource + "> .";
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");

        // User can read target resource.
        final HttpGet get1 = getObjMethod(targetResource);
        setAuth(get1, username);
        assertEquals(HttpStatus.SC_OK, getStatus(get1));

        // User can patch target resource.
        final String patch = "INSERT DATA { <> <http://purl.org/dc/elements/1.1/title> \"Changed it\"}";
        final HttpEntity patchEntity = new StringEntity(patch, sparqlContentType);
        try (final CloseableHttpResponse resp = (CloseableHttpResponse) PATCH(targetUri, patchEntity,
                username)) {
            assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(resp));
        }

        // Make a user writable container.
        final String writeableUri = ingestObj(writeableResource);
        final String writeableAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#writeauth> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + writeableResource + "> ;\n" +
                "   acl:default <" + writeableResource + "> .";
        ingestAclString(writeableUri, writeableAcl, "fedoraAdmin");

        // Ensure we can write to the writeable resource.
        testCanWrite(writeableResource, username);

        // Try to create indirect container referencing writeable resource with POST.
        final HttpPost userPost = postObjMethod(writeableResource);
        setAuth(userPost, username);
        userPost.addHeader("Link", "<" + INDIRECT_CONTAINER.toString() + ">; rel=type");
        final String indirect = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> ldp:insertedContentRelation test:something ;" +
                "ldp:membershipResource <" + targetResource + "> ;" +
                "ldp:hasMemberRelation test:predicateToCreate .";
        final HttpEntity indirectEntity = new StringEntity(indirect, turtleContentType);
        userPost.setEntity(new StringEntity(indirect, turtleContentType));
        userPost.setHeader("Content-type", "text/turtle");
        assertEquals(HttpStatus.SC_CREATED, getStatus(userPost));

        // Try to create indirect container referencing writeable resource with PUT.
        final String indirectString = getRandomUniqueId();
        final HttpPut userPut = putObjMethod(writeableResource + "/" + indirectString);
        setAuth(userPut, username);
        userPut.addHeader("Link", "<" + INDIRECT_CONTAINER.toString() + ">; rel=type");
        userPut.setEntity(indirectEntity);
        userPut.setHeader("Content-type", "text/turtle");
        assertEquals(HttpStatus.SC_CREATED, getStatus(userPut));

        // Create an user writeable resource.
        final HttpPost targetPost = postObjMethod(writeableResource);
        setAuth(targetPost, username);
        final String tempTarget;
        try (final CloseableHttpResponse resp = execute(targetPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            tempTarget = getLocation(resp);
        }

        // Try to create indirect container referencing an available resource.
        final String indirect_ok = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> ldp:insertedContentRelation test:something ;" +
                "ldp:membershipResource <" + tempTarget + "> ;" +
                "ldp:hasMemberRelation test:predicateToCreate .";
        final HttpPost userPatchPost = postObjMethod(writeableResource);
        setAuth(userPatchPost, username);
        userPatchPost.addHeader("Link", "<" + INDIRECT_CONTAINER.toString() + ">; rel=type");
        userPatchPost.setEntity(new StringEntity(indirect_ok, turtleContentType));
        userPatchPost.setHeader("Content-type", "text/turtle");
        final String indirectUri;
        try (final CloseableHttpResponse resp = execute(userPatchPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            indirectUri = getLocation(resp);
        }

        // Then PATCH to the writeable resource.
        final HttpPatch patchIndirect = new HttpPatch(indirectUri);
        setAuth(patchIndirect, username);
        final String patch_text = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE { <> ldp:membershipResource ?o } \n" +
                "INSERT { <> ldp:membershipResource <" + targetResource + "> } \n" +
                "WHERE { <> ldp:membershipResource ?o }";
        patchIndirect.setEntity(new StringEntity(patch_text, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchIndirect));

        // Delete the ldp:membershipRelation and add it back
        final String patch_delete_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE DATA { <> ldp:membershipResource <" + targetResource + "> }";
        final HttpPatch patchIndirect2 = new HttpPatch(indirectUri);
        setAuth(patchIndirect2, username);
        patchIndirect2.setEntity(new StringEntity(patch_delete_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchIndirect2));

        // Cannot insert membershipResource without deleting the default value
        final String patch_insert_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE { <> ldp:membershipResource ?o } \n" +
                "INSERT { <> ldp:membershipResource <" + targetResource + "> } \n" +
                "WHERE { <> ldp:membershipResource ?o }";
        final HttpPatch patchIndirect3 = new HttpPatch(indirectUri);
        setAuth(patchIndirect3, username);
        patchIndirect3.setEntity(new StringEntity(patch_insert_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchIndirect3));

        // Ensure we can still write to the writeable resource.
        testCanWrite(writeableResource, username);

    }

    @Ignore("Until FCREPO-3310 and FCREPO-3311 are resolved")
    @Test
    public void testDirectRelationshipForbidden() throws IOException {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String writeableResource = "/rest/" + getRandomUniqueId();
        final String username = "user28";

        final String targetUri = ingestObj(targetResource);

        final String readonlyString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read ;\n" +
                "   acl:accessTo <" + targetResource + "> .";
        ingestAclString(targetUri, readonlyString, "fedoraAdmin");

        // User can read target resource.
        final HttpGet get1 = getObjMethod(targetResource);
        setAuth(get1, username);
        assertEquals(HttpStatus.SC_OK, getStatus(get1));

        // User can't patch target resource.
        final String patch = "INSERT DATA { <> <http://purl.org/dc/elements/1.1/title> \"Changed it\"}";
        final HttpEntity patchEntity = new StringEntity(patch, sparqlContentType);
        try (final CloseableHttpResponse resp = (CloseableHttpResponse) PATCH(targetUri, patchEntity,
                username)) {
            assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(resp));
        }

        // Make a user writable container.
        final String writeableUri = ingestObj(writeableResource);
        final String writeableAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#writeauth> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + writeableResource + "> ;\n" +
                "   acl:default <" + writeableResource + "> .";
        ingestAclString(writeableUri, writeableAcl, "fedoraAdmin");

        // Ensure we can write to writeable resource.
        testCanWrite(writeableResource, username);

        // Try to create direct container referencing readonly resource with POST.
        final HttpPost userPost = postObjMethod(writeableResource);
        setAuth(userPost, username);
        userPost.addHeader("Link", "<" + DIRECT_CONTAINER.toString() + ">; rel=type");
        final String direct = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> ldp:membershipResource <" + targetResource + "> ;" +
                "ldp:hasMemberRelation test:predicateToCreate .";
        final HttpEntity directEntity = new StringEntity(direct, turtleContentType);
        userPost.setEntity(directEntity);
        userPost.setHeader("Content-type", "text/turtle");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(userPost));

        // Try to create direct container referencing readonly resource with PUT.
        final String indirectString = getRandomUniqueId();
        final HttpPut userPut = putObjMethod(writeableResource + "/" + indirectString);
        setAuth(userPut, username);
        userPut.addHeader("Link", "<" + DIRECT_CONTAINER.toString() + ">; rel=type");
        userPut.setEntity(directEntity);
        userPut.setHeader("Content-type", "text/turtle");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(userPut));

        // Create an user writeable resource.
        final HttpPost targetPost = postObjMethod(writeableResource);
        setAuth(targetPost, username);
        final String tempTarget;
        try (final CloseableHttpResponse resp = execute(targetPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            tempTarget = getLocation(resp);
        }

        // Try to create direct container referencing an available resource.
        final String direct_ok = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> ldp:membershipResource <" + tempTarget + "> ;\n" +
                "ldp:hasMemberRelation test:predicateToCreate .";
        final HttpPost userPatchPost = postObjMethod(writeableResource);
        setAuth(userPatchPost, username);
        userPatchPost.addHeader("Link", "<" + DIRECT_CONTAINER.toString() + ">; rel=type");
        userPatchPost.setEntity(new StringEntity(direct_ok, turtleContentType));
        userPatchPost.setHeader("Content-type", "text/turtle");
        final String directUri;
        try (final CloseableHttpResponse resp = execute(userPatchPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            directUri = getLocation(resp);
        }

        // Then PATCH to the readonly resource.
        final HttpPatch patchDirect = new HttpPatch(directUri);
        setAuth(patchDirect, username);
        final String patch_text = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE { <> ldp:membershipResource ?o } \n" +
                "INSERT { <> ldp:membershipResource <" + targetResource + "> } \n" +
                "WHERE { <> ldp:membershipResource ?o }";
        patchDirect.setEntity(new StringEntity(patch_text, sparqlContentType));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(patchDirect));

        // Delete the ldp:membershipRelation and add it with INSERT DATA {}
        final String patch_delete_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE DATA { <> ldp:membershipResource <" + tempTarget + "> }";
        final HttpPatch patchDirect2 = new HttpPatch(directUri);
        setAuth(patchDirect2, username);
        patchDirect2.setEntity(new StringEntity(patch_delete_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchDirect2));

        final String patch_insert_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "INSERT DATA { <> ldp:membershipResource <" + targetResource + "> }";
        final HttpPatch patchDirect3 = new HttpPatch(directUri);
        setAuth(patchDirect3, username);
        patchDirect3.setEntity(new StringEntity(patch_insert_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(patchDirect3));

        // Patch the indirect to the readonly target as admin
        final HttpPatch patchAsAdmin = new HttpPatch(directUri);
        setAuth(patchAsAdmin, "fedoraAdmin");
        patchAsAdmin.setEntity(new StringEntity(patch_text, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchAsAdmin));

        // Ensure the patching happened.
        final HttpGet verifyGet = new HttpGet(directUri);
        setAuth(verifyGet, "fedoraAdmin");
        try (final CloseableHttpResponse response = execute(verifyGet)) {
            final CloseableDataset dataset = getDataset(response);
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Can't find " + targetUri + " in graph",
                    graph.contains(
                        Node.ANY,
                        NodeFactory.createURI(directUri),
                        MEMBERSHIP_RESOURCE.asNode(),
                        NodeFactory.createURI(targetUri)
                    )
            );
        }

        // Try to POST a child as user
        final HttpPost postChild = new HttpPost(directUri);
        final String postTarget = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n" +
                "<> test:something <" + tempTarget + "> .";
        final HttpEntity putPostChild = new StringEntity(postTarget, turtleContentType);
        setAuth(postChild, username);
        postChild.setEntity(putPostChild);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(postChild));

        // Try to PUT a child as user
        final String id = getRandomUniqueId();
        final HttpPut putChild = new HttpPut(directUri + "/" + id);
        setAuth(putChild, username);
        putChild.setEntity(putPostChild);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(putChild));

        // Put the child as Admin
        setAuth(putChild, "fedoraAdmin");
        assertEquals(HttpStatus.SC_CREATED, getStatus(putChild));

        // Try to delete the child as user
        final HttpDelete deleteChild = new HttpDelete(directUri + "/" + id);
        setAuth(deleteChild, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteChild));

        // Try to delete the indirect container
        final HttpDelete deleteIndirect = new HttpDelete(directUri);
        setAuth(deleteIndirect, username);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(deleteIndirect));

        // Ensure we can still write to the writeable resource.
        testCanWrite(writeableResource, username);

    }

    @Test
    public void testDirectRelationshipsOk() throws IOException {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String writeableResource = "/rest/" + getRandomUniqueId();
        final String username = "user28";

        final String targetUri = ingestObj(targetResource);

        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + targetResource + "> .";
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");

        // User can read target resource.
        final HttpGet get1 = getObjMethod(targetResource);
        setAuth(get1, username);
        assertEquals(HttpStatus.SC_OK, getStatus(get1));

        // User can patch target resource.
        final String patch = "INSERT DATA { <> <http://purl.org/dc/elements/1.1/title> \"Changed it\"}";
        final HttpEntity patchEntity = new StringEntity(patch, sparqlContentType);
        try (final CloseableHttpResponse resp = (CloseableHttpResponse) PATCH(targetUri, patchEntity,
                username)) {
            assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(resp));
        }

        // Make a user writable container.
        final String writeableUri = ingestObj(writeableResource);
        final String writeableAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#writeauth> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + writeableResource + "> ;\n" +
                "   acl:default <" + writeableResource + "> .";
        ingestAclString(writeableUri, writeableAcl, "fedoraAdmin");

        // Ensure we can write to the writeable resource.
        testCanWrite(writeableResource, username);

        // Try to create direct container referencing writeable resource with POST.
        final HttpPost userPost = postObjMethod(writeableResource);
        setAuth(userPost, username);
        userPost.addHeader("Link", "<" + DIRECT_CONTAINER.toString() + ">; rel=type");
        final String indirect = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> ldp:membershipResource <" + targetResource + "> ;\n" +
                "ldp:hasMemberRelation test:predicateToCreate .";
        final HttpEntity directEntity = new StringEntity(indirect, turtleContentType);
        userPost.setEntity(new StringEntity(indirect, turtleContentType));
        userPost.setHeader("Content-type", "text/turtle");
        assertEquals(HttpStatus.SC_CREATED, getStatus(userPost));

        // Try to create direct container referencing writeable resource with PUT.
        final String directString = getRandomUniqueId();
        final HttpPut userPut = putObjMethod(writeableResource + "/" + directString);
        setAuth(userPut, username);
        userPut.addHeader("Link", "<" + DIRECT_CONTAINER.toString() + ">; rel=type");
        userPut.setEntity(directEntity);
        userPut.setHeader("Content-type", "text/turtle");
        assertEquals(HttpStatus.SC_CREATED, getStatus(userPut));

        // Create an user writeable resource.
        final HttpPost targetPost = postObjMethod(writeableResource);
        setAuth(targetPost, username);
        final String tempTarget;
        try (final CloseableHttpResponse resp = execute(targetPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            tempTarget = getLocation(resp);
        }

        // Try to create direct container referencing an available resource.
        final String direct_ok = "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                "@prefix test: <http://example.org/test#> .\n\n" +
                "<> ldp:membershipResource <" + tempTarget + "> ;\n" +
                "ldp:hasMemberRelation test:predicateToCreate .";
        final HttpPost userPatchPost = postObjMethod(writeableResource);
        setAuth(userPatchPost, username);
        userPatchPost.addHeader("Link", "<" + DIRECT_CONTAINER.toString() + ">; rel=type");
        userPatchPost.setEntity(new StringEntity(direct_ok, turtleContentType));
        userPatchPost.setHeader("Content-type", "text/turtle");
        final String directUri;
        try (final CloseableHttpResponse resp = execute(userPatchPost)) {
            assertEquals(HttpStatus.SC_CREATED, getStatus(resp));
            directUri = getLocation(resp);
        }

        // Then PATCH to the readonly resource.
        final HttpPatch patchDirect = new HttpPatch(directUri);
        setAuth(patchDirect, username);
        final String patch_text = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE { <> ldp:membershipResource ?o } \n" +
                "INSERT { <> ldp:membershipResource <" + targetResource + "> } \n" +
                "WHERE { <> ldp:membershipResource ?o }";
        patchDirect.setEntity(new StringEntity(patch_text, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchDirect));

        // Delete the ldp:membershipRelation and add it with INSERT
        final String patch_delete_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE DATA { <> ldp:membershipResource <" + targetResource + "> }";
        final HttpPatch patchDirect2 = new HttpPatch(directUri);
        setAuth(patchDirect2, username);
        patchDirect2.setEntity(new StringEntity(patch_delete_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchDirect2));

        // Cannot insert membershipResource without deleting the default value
        final String patch_insert_relation = "prefix ldp: <http://www.w3.org/ns/ldp#> \n" +
                "DELETE { <> ldp:membershipResource ?o } \n" +
                "INSERT { <> ldp:membershipResource <" + targetResource + "> } \n" +
                "WHERE { <> ldp:membershipResource ?o }";
        final HttpPatch patchDirect3 = new HttpPatch(directUri);
        setAuth(patchDirect3, username);
        patchDirect3.setEntity(new StringEntity(patch_insert_relation, sparqlContentType));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchDirect3));

        // Ensure we can write to the writeable resource.
        testCanWrite(writeableResource, username);
    }

    @Test
    public void testSameInTransaction() throws Exception {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String username = "user28";
        // Make a basic container.
        final String targetUri = ingestObj(targetResource);
        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + targetResource + "> .";
        // Allow user28 to read and write this object.
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");
        // Test that user28 can read target resource.
        final HttpGet getAllowed1 = getObjMethod(targetResource);
        setAuth(getAllowed1, username);
        assertEquals(HttpStatus.SC_OK, getStatus(getAllowed1));
        // Test that user28 can patch target resource.
        final HttpPatch patchAllowed1 = patchObjMethod(targetResource);
        final String patchString = "prefix dc: <http://purl.org/dc/elements/1.1/> INSERT { <> dc:title " +
            "\"new title\" } WHERE {}";
        final StringEntity patchEntity = new StringEntity(patchString, Charsets.UTF8_CHARSET);
        patchAllowed1.setEntity(patchEntity);
        patchAllowed1.setHeader(CONTENT_TYPE, "application/sparql-update");
        setAuth(patchAllowed1, username);
        assertEquals(SC_NO_CONTENT, getStatus(patchAllowed1));
        // Test that user28 can post to target resource.
        final HttpPost postAllowed1 = postObjMethod(targetResource);
        setAuth(postAllowed1, username);
        final String childResource;
        try (final CloseableHttpResponse response = execute(postAllowed1)) {
            assertEquals(SC_CREATED, getStatus(postAllowed1));
            childResource = getLocation(response);
        }
        // Test that user28 cannot patch the child resource (ACL is not acl:default).
        final HttpPatch patchDisallowed1 = new HttpPatch(childResource);
        patchDisallowed1.setEntity(patchEntity);
        patchDisallowed1.setHeader(CONTENT_TYPE, "application/sparql-update");
        setAuth(patchDisallowed1, username);
        assertEquals(SC_FORBIDDEN, getStatus(patchDisallowed1));
        // Test that user28 cannot post to a child resource.
        final HttpPost postDisallowed1 = new HttpPost(childResource);
        setAuth(postDisallowed1, username);
        assertEquals(SC_FORBIDDEN, getStatus(postDisallowed1));
        // Test another user cannot access the target resource.
        final HttpGet getDisallowed1 = getObjMethod(targetResource);
        setAuth(getDisallowed1, "user400");
        assertEquals(SC_FORBIDDEN, getStatus(getDisallowed1));
        // Get the transaction endpoint.
        final HttpGet getTransactionEndpoint = getObjMethod("/rest");
        setAuth(getTransactionEndpoint, "fedoraAdmin");
        final String transactionEndpoint;
        final Pattern linkHeaderMatcher = Pattern.compile("<([^>]+)>");
        try (final CloseableHttpResponse response = execute(getTransactionEndpoint)) {
            final var linkheaders = getLinkHeaders(response);
            transactionEndpoint = linkheaders.stream()
                    .filter(t -> t.contains("http://fedora.info/definitions/v4/transaction#endpoint"))
                    .map(t -> {
                        final var matches = linkHeaderMatcher.matcher(t);
                        matches.find();
                        return matches.group(1);
                    })
                    .findFirst()
                    .orElseThrow(Exception::new);
        }
        // Create a transaction.
        final HttpPost postTransaction = new HttpPost(transactionEndpoint);
        setAuth(postTransaction, "fedoraAdmin");
        final String transactionId;
        try (final CloseableHttpResponse response = execute(postTransaction)) {
            assertEquals(SC_CREATED, getStatus(response));
            transactionId = getLocation(response);
        }
        // Test user28 can post to  target resource in a transaction.
        final HttpPost postChildInTx = postObjMethod(targetResource);
        setAuth(postChildInTx, username);
        postChildInTx.setHeader(ATOMIC_ID_HEADER, transactionId);
        final String txChild;
        try (final CloseableHttpResponse response = execute(postChildInTx)) {
            assertEquals(SC_CREATED, getStatus(response));
            txChild = getLocation(response);
        }
        // Test user28 cannot post to the child in a transaction.
        final HttpPost postDisallowed2 = new HttpPost(txChild);
        setAuth(postDisallowed2, username);
        postDisallowed2.setHeader(ATOMIC_ID_HEADER, transactionId);
        assertEquals(SC_FORBIDDEN, getStatus(postDisallowed2));
    }

    @Test
    public void testBinaryAndDescriptionAllowed() throws Exception {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String username = "user88";
        // Make a basic container.
        final String targetUri = ingestObj(targetResource);
        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:default <" + targetResource + "> ;" +
                "   acl:accessTo <" + targetResource + "> .";
        // Allow user to read and write this object.
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");
        // user creates a binary
        final HttpPost newBinary = postObjMethod(targetResource);
        setAuth(newBinary, username);
        newBinary.setHeader(CONTENT_TYPE, "text/plain");
        final StringEntity stringData = new StringEntity("This is some data", Charsets.UTF8_CHARSET);
        newBinary.setEntity(stringData);
        final String binaryLocation;
        try (final CloseableHttpResponse response = execute(newBinary)) {
            assertEquals(SC_CREATED, getStatus(response));
            binaryLocation = getLocation(response);
        }
        // Try PUTting a new binary
        final HttpPut putAgain = new HttpPut(binaryLocation);
        setAuth(putAgain, username);
        putAgain.setHeader(CONTENT_TYPE, "text/plain");
        final StringEntity newStringData = new StringEntity("Some other data", Charsets.UTF8_CHARSET);
        putAgain.setEntity(newStringData);
        assertEquals(SC_NO_CONTENT, getStatus(putAgain));
        // Try PUTting to binary description
        final HttpPut putDesc = new HttpPut(binaryLocation + "/" + FCR_METADATA);
        setAuth(putDesc, username);
        putDesc.setHeader(CONTENT_TYPE, "text/turtle");
        final StringEntity putDescData = new StringEntity("<> <http://purl.org/dc/elements/1.1/title> \"Some title\".",
                Charsets.UTF8_CHARSET);
        putDesc.setEntity(putDescData);
        assertEquals(SC_NO_CONTENT, getStatus(putDesc));
        // Check the title
        assertPredicateValue(binaryLocation + "/" + FCR_METADATA, "http://purl.org/dc/elements/1.1/title",
                "Some title");
        // Try PATCHing to binary description
        final HttpPatch patchDesc = new HttpPatch(binaryLocation + "/" + FCR_METADATA);
        setAuth(patchDesc, username);
        patchDesc.setHeader(CONTENT_TYPE, "application/sparql-update");
        final StringEntity patchDescData = new StringEntity("PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
                "DELETE { <> dc:title ?o } INSERT { <> dc:title \"Some different title\" } WHERE { <> dc:title ?o }",
                Charsets.UTF8_CHARSET);
        patchDesc.setEntity(patchDescData);
        assertEquals(SC_NO_CONTENT, getStatus(patchDesc));
        // Check the title
        assertPredicateValue(binaryLocation + "/" + FCR_METADATA, "http://purl.org/dc/elements/1.1/title",
                "Some different title");

    }

    @Test
    public void testRequestWithEmptyPath() throws Exception {
        // Ensure HttpClient does not remove empty paths
        final RequestConfig config = RequestConfig.custom().setNormalizeUri(false).build();

        final String username = "testUser92";
        final String parent = getRandomUniqueId();
        final HttpPost postParent = postObjMethod();
        postParent.setHeader("Slug", parent);
        setAuth(postParent, "fedoraAdmin");
        final String parentUri;
        try (final CloseableHttpResponse response = execute(postParent)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            parentUri = getLocation(response);
        }
        // Make parent only accessible to fedoraAdmin
        final String parentAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"fedoraAdmin\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + parentUri + "> .";
        ingestAclString(parentUri, parentAcl, "fedoraAdmin");
        // Admin can see parent
        final HttpGet getAdminParent = getObjMethod(parent);
        setAuth(getAdminParent, "fedoraAdmin");
        assertEquals(OK.getStatusCode(), getStatus(getAdminParent));
        final HttpGet getParent = getObjMethod(parent);
        setAuth(getParent, username);
        // testUser92 cannot see parent.
        assertEquals(FORBIDDEN.getStatusCode(), getStatus(getParent));

        final String child = getRandomUniqueId();
        final HttpPost postChild = postObjMethod(parent);
        postChild.setHeader("Slug", child);
        setAuth(postChild, "fedoraAdmin");
        final String childUri;
        try (final CloseableHttpResponse response = execute(postChild)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            childUri = getLocation(response);
        }
        // Make child accessible to testUser92
        final String childAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + childUri + "> .";
        ingestAclString(childUri, childAcl, "fedoraAdmin");
        // Admin can see child.
        final HttpGet getAdminChild = getObjMethod(parent + "/" + child);
        setAuth(getAdminChild, "fedoraAdmin");
        assertEquals(OK.getStatusCode(), getStatus(getAdminChild));

        // testUser92 can see child.
        final HttpGet getChild = getObjMethod(parent + "/" + child);
        setAuth(getChild, username);
        assertEquals(OK.getStatusCode(), getStatus(getChild));

        // Admin bypasses ACL resolution gets 409.
        final HttpGet getAdminRequest = getObjMethod(parent + "//" + child);
        setAuth(getAdminRequest, "fedoraAdmin");
        getAdminRequest.setConfig(config);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(getAdminRequest));
        // User
        final HttpGet getUserRequest = getObjMethod(parent + "//" + child);
        setAuth(getUserRequest, username);
        getUserRequest.setConfig(config);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(getUserRequest));
    }

    @Test
    public void testGetWithEmbeddedResourcesOk() throws Exception {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String childResource = targetResource + "/" + getRandomUniqueId();
        final String username = "user88";
        // Make a basic container.
        final String targetUri = ingestObj(targetResource);
        ingestObj(childResource);
        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:default <" + targetResource + "> ;" +
                "   acl:accessTo <" + targetResource + "> .";
        // Allow user to read and write this object.
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");

        final HttpGet getAdminChild = new HttpGet(targetUri);
        setAuth(getAdminChild, username);
        getAdminChild.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINED + "\"");
        assertEquals(OK.getStatusCode(), getStatus(getAdminChild));
    }

    @Test
    public void testGetWithEmbeddedResourceDenied() throws Exception {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String childResource = targetResource + "/" + getRandomUniqueId();
        final String username = "user88";
        // Make a basic container.
        final String targetUri = ingestObj(targetResource);
        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + targetResource + "> .";
        // Allow user to read and write this object.
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");

        final String childUri = ingestObj(childResource);
        final String noAccessString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"fedoraAdmin\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + childResource + "> .";
        ingestAclString(childUri, noAccessString, "fedoraAdmin");

        // Can get the target.
        final HttpGet getTarget = new HttpGet(targetUri);
        setAuth(getTarget, username);
        assertEquals(OK.getStatusCode(), getStatus(getTarget));

        // Can't get the child.
        final HttpGet getChild = new HttpGet(childUri);
        setAuth(getChild, username);
        assertEquals(FORBIDDEN.getStatusCode(), getStatus(getChild));

        // So you can't get the target with embedded resources.
        final HttpGet getAdminChild = new HttpGet(targetUri);
        setAuth(getAdminChild, username);
        getAdminChild.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINED + "\"");
        assertEquals(FORBIDDEN.getStatusCode(), getStatus(getAdminChild));
    }

    @Test
    public void testDeepDeleteAllowed() throws Exception {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String username = "user88";
        // Make a basic container.
        final String targetUri = ingestObj(targetResource);
        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + targetResource + "> ;\n" +
                "   acl:default <" + targetResource + "> .";
        // Allow user to read and write this object.
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");

        final String child1 = targetResource + "/" + getRandomUniqueId();
        final String child2 = targetResource + "/" + getRandomUniqueId();
        final String child1_1 = child1 + "/" + getRandomUniqueId();
        final String child2_1 = child2 + "/" + getRandomUniqueId();
        ingestObj(child1);
        ingestObj(child2);
        ingestObj(child1_1);
        ingestObj(child2_1);

        assertGetRequest(targetUri, username, OK);
        assertGetRequest(serverAddress + child1, username, OK);
        assertGetRequest(serverAddress + child2, username, OK);
        assertGetRequest(serverAddress + child1_1, username, OK);
        assertGetRequest(serverAddress + child2_1, username, OK);

        final var delete = new HttpDelete(targetUri);
        setAuth(delete, username);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(delete));
    }

    @Test
    public void testDeepDeleteFailed() throws Exception {
        final String targetResource = "/rest/" + getRandomUniqueId();
        final String username = "user88";
        // Make a basic container.
        final String targetUri = ingestObj(targetResource);
        final String readwriteString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"" + username + "\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + targetResource + "> ;\n" +
                "   acl:default <" + targetResource + "> .";
        // Allow user to read and write this object.
        ingestAclString(targetUri, readwriteString, "fedoraAdmin");

        final String child1 = targetResource + "/" + getRandomUniqueId();
        final String child2 = targetResource + "/" + getRandomUniqueId();
        final String child1_1 = child1 + "/" + getRandomUniqueId();
        final String child2_1 = child2 + "/" + getRandomUniqueId();
        ingestObj(child1);
        ingestObj(child2);
        ingestObj(child1_1);
        final String child2_1_URI = ingestObj(child2_1);
        final String noAccessString = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n" +
                "<#readauthz> a acl:Authorization ;\n" +
                "   acl:agent \"fedoraAdmin\" ;\n" +
                "   acl:mode acl:Read, acl:Write ;\n" +
                "   acl:accessTo <" + child2_1 + "> .";
        ingestAclString(child2_1_URI, noAccessString, "fedoraAdmin");

        assertGetRequest(targetUri, username, OK);
        assertGetRequest(serverAddress + child1, username, OK);
        assertGetRequest(serverAddress + child2, username, OK);
        assertGetRequest(serverAddress + child1_1, username, OK);
        assertGetRequest(serverAddress + child2_1, username, FORBIDDEN);

        final var delete = new HttpDelete(targetUri);
        setAuth(delete, username);
        assertEquals(FORBIDDEN.getStatusCode(), getStatus(delete));
    }

    private void assertGetRequest(final String uri, final String username, final Response.Status expectedResponse) {
        final var getTarget = new HttpGet(uri);
        setAuth(getTarget, username);
        assertEquals(expectedResponse.getStatusCode(), getStatus(getTarget));
    }

    /**
     * Check the graph has the predicate with the value.
     * @param targetUri Full URI of the resource to check.
     * @param predicateUri Full URI of the predicate to check.
     * @param predicateValue Literal value to look for.
     * @throws Exception if problems performing the GET.
     */
    private void assertPredicateValue(final String targetUri, final String predicateUri, final String predicateValue)
            throws Exception {
        final HttpGet verifyGet = new HttpGet(targetUri);
        setAuth(verifyGet, "fedoraAdmin");
        try (final CloseableHttpResponse response = execute(verifyGet)) {
            final CloseableDataset dataset = getDataset(response);
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Can't find " + predicateValue + " for predicate " + predicateUri + " in graph",
                    graph.contains(
                            Node.ANY,
                            Node.ANY,
                            NodeFactory.createURI(predicateUri),
                            NodeFactory.createLiteral(predicateValue)
                    )
            );
        }
    }


    /**
     * Utility function to ingest a ACL from a string.
     *
     * @param resourcePath Path to the resource if doesn't end with "/fcr:acl" it is added.
     * @param acl the text/turtle ACL as a string
     * @param username user to ingest as
     * @return the response from the ACL ingest.
     * @throws IOException on StringEntity encoding or client execute
     */
    private HttpResponse ingestAclString(final String resourcePath, final String acl, final String username)
            throws IOException {
        final String aclPath = (resourcePath.endsWith("/fcr:acl") ? resourcePath : resourcePath + "/fcr:acl");
        final HttpPut putReq = new HttpPut(aclPath);
        setAuth(putReq, username);
        putReq.setHeader("Content-type", "text/turtle");
        putReq.setEntity(new StringEntity(acl, turtleContentType));
        return execute(putReq);
    }

    /**
     * Ensure that a writeable resource is still writeable
     *
     * @param writeableResource the URI of the writeable resource.
     * @param username the user will write access.
     * @throws UnsupportedEncodingException if default charset for String Entity is unsupported
     */
    private void testCanWrite(final String writeableResource, final String username)
            throws UnsupportedEncodingException {
        // Try to create a basic container inside the writeable resource with POST.
        final HttpPost okPost = postObjMethod(writeableResource);
        setAuth(okPost, username);
        assertEquals(HttpStatus.SC_CREATED, getStatus(okPost));

        // Try to PATCH the writeableResource
        final HttpPatch okPatch = patchObjMethod(writeableResource);
        final String patchString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE { <> dc:title ?o1 } " +
                "INSERT { <> dc:title \"Changed title\" }  WHERE { <> dc:title ?o1 }";
        final HttpEntity patchEntity = new StringEntity(patchString, sparqlContentType);
        setAuth(okPatch, username);
        okPatch.setHeader("Content-type", "application/sparql-update");
        okPatch.setEntity(patchEntity);
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(okPatch));
    }

    @Test
    public void testAuthenticatedUserCanCreateTransaction() {
        final HttpPost txnCreatePost = postObjMethod("rest/fcr:tx");
        setAuth(txnCreatePost, "testUser92");
        assertEquals(SC_CREATED, getStatus(txnCreatePost));
    }
}
