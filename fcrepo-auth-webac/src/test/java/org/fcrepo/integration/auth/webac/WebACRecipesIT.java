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
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.auth.webac.WebACRolesProvider.GROUP_AGENT_BASE_URI_PROPERTY;
import static org.fcrepo.http.api.FedoraAcl.ROOT_AUTHORIZATION_PROPERTY;
import static org.fcrepo.kernel.modeshape.utils.FedoraSessionUserUtil.USER_AGENT_BASE_URI_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Optional;
import javax.ws.rs.core.Link;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
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
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Eichman
 * @author whikloj
 * @since September 4, 2015
 */
public class WebACRecipesIT extends AbstractResourceIT {

    private static final Logger logger = LoggerFactory.getLogger(WebACRecipesIT.class);

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    /**
     * Convenience method to create an ACL with 0 or more authorization resources in the respository.
     */
    private String ingestAcl(final String username,
            final String aclFilePath, final String aclResourcePath) throws IOException {

        // create the ACL
        final HttpResponse aclResponse = ingestTurtleResource(username, aclFilePath, aclResourcePath);

        // get the URI to the newly created resource
        final String aclURI = aclResponse.getFirstHeader("Location").getValue();

        return aclURI;
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
            assertEquals(
                "Didn't get a CREATED response!: " + IOUtils.toString(response.getEntity().getContent(), "UTF-8"),
                CREATED.getStatusCode(), getStatus(response));
            return response;
        }

    }

    /**
     * Convenience method to set up a regular FedoraResource
     *
     * @param path Path to put the resource under
     * @return the Location of the newly created resource
     * @throws IOException
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
        final String acl2 = ingestAcl("fedoraAdmin", "/acls/02/acl.ttl", testObj + "/fcr:acl");

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
        final String aclDark =
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
        final String testObj = ingestObjWithACL("/rest/mixedCollection", "/acls/05/acl.ttl");
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

        final String acl9 = ingestAcl("fedoraAdmin", "/acls/09/acl.ttl", testObj + "/fcr:acl");

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
     */
    @Test
    public void scenario18Test1() throws IOException, UnsupportedEncodingException {
        final String testObj = ingestObj("/rest/append_only_resource");
        final String id = "/rest/append_only_resource/" + getRandomUniqueId();
        ingestObj(id);

        logger.debug("user18 can't read (no ACL): {}", id);
        final HttpGet requestGet = getObjMethod(id);
        setAuth(requestGet, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet));

        logger.debug("user18 can't append (no ACL): {}", id);
        final HttpPatch requestPatch = patchObjMethod(id);
        setAuth(requestPatch, "user18");
        requestPatch.setHeader("Content-type", "application/sparql-update");
        requestPatch.setEntity(new StringEntity("INSERT { <> <" + title.getURI() + "> \"Test title\" . } WHERE {}"));

        logger.debug("user18 can't delete (no ACL): {}", id);
        final HttpDelete requestDelete = deleteObjMethod(id);
        setAuth(requestDelete, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestDelete));

        final String acl = ingestAcl("fedoraAdmin", "/acls/18/append-only-acl.ttl", testObj + "/fcr:acl");

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
     */
    @Test
    public void scenario18Test2() throws IOException, UnsupportedEncodingException {
        final String testObj = ingestObj("/rest/read_append_resource");

        final String id = "/rest/read_append_resource/" + getRandomUniqueId();
        ingestObj(id);

        logger.debug("user18 can't read (no ACL): {}", id);
        final HttpGet requestGet = getObjMethod(id);
        setAuth(requestGet, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet));

        logger.debug("user18 can't append (no ACL): {}", id);
        final HttpPatch requestPatch = patchObjMethod(id);
        setAuth(requestPatch, "user18");
        requestPatch.setHeader("Content-type", "application/sparql-update");
        requestPatch.setEntity(new StringEntity(
                "INSERT { <> <" + title.getURI() + "> \"some title\" . } WHERE {}"));
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestPatch));

        final String acl = ingestAcl("fedoraAdmin", "/acls/18/read-append-acl.ttl", testObj + "/fcr:acl");

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
     */
    @Test
    public void scenario18Test3() throws IOException, UnsupportedEncodingException {
        final String testObj = ingestObj("/rest/read_append_write_resource");

        final String id = "/rest/read_append_write_resource/" + getRandomUniqueId();
        ingestObj(id);

        logger.debug("user18 can't read (no ACL): {}", id);
        final HttpGet requestGet = getObjMethod(id);
        setAuth(requestGet, "user18");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet));

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

        final String acl = ingestAcl("fedoraAdmin", "/acls/18/read-append-write-acl.ttl", testObj + "/fcr:acl");

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

        logger.debug("Anonymous can't read (no ACL): {}", id);
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet1));

        logger.debug("Can username 'user06a' read {} (no ACL)", id);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "user06a");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet2));

        logger.debug("Can username 'notuser06b' read {} (no ACL)", id);
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "user06b");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet3));

        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/test-root-authorization2.ttl");
        logger.debug("Can username 'user06a' read {} (overridden system ACL)", id);
        final HttpGet requestGet4 = getObjMethod(id);
        setAuth(requestGet4, "user06a");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet4));
        System.clearProperty(ROOT_AUTHORIZATION_PROPERTY);

        // Add ACL to root
        final String rootURI = getObjMethod("/rest").getURI().toString();
        final String acl = ingestAcl("fedoraAdmin", "/acls/06/acl.ttl",
                                     rootURI + "/fcr:acl");

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
        final String parentObj = ingestObjWithACL(parentPath, "/acls/21/acl.ttl");

        final String id = parentPath + "/" + getRandomUniqueId();
        final String testObj = ingestObj(id);


        // Test the parent ACL with no acl:default is applied for the parent resource authorization.
        final HttpGet requestGet1 = getObjMethod(parentPath);
        setAuth(requestGet1, "user21");
        assertEquals("Agent user21 can't read resource " + parentPath + " with its own ACL!",
                HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpGet requestGet2 = getObjMethod(id);
        assertEquals("Agent user21 inherits read permission from parent ACL to read resource " + testObj + "!",
                HttpStatus.SC_FORBIDDEN, getStatus(requestGet2));

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
        final String acl = ingestAcl("fedoraAdmin",
                "/acls/07/acl.ttl", bookURI + "/fcr:acl");

        logger.debug("Anonymous can't read");
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet1));

        logger.debug("Can username 'user07' read {}", testObj);
        final HttpGet requestGet2 = getObjMethod(id);

        setAuth(requestGet2, "user07");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
    @Ignore("FAILING")
    public void testAccessToHashResource() throws IOException {
        final String id = "/rest/some/parent#hash-resource";
        final String testObj = ingestObj(id);
        final String acl = ingestAcl("fedoraAdmin", "/acls/08/acl.ttl", testObj + "/fcr:acl");

        logger.debug("Anonymous can't read");
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet1));

        logger.debug("Can username 'user08' read {}", testObj);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "user08");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
    @Ignore ("Until implemented with Memento")
    public void testAccessToVersionedResources() throws IOException {
        final String idVersion = "/rest/versionResource";
        ingestObj(idVersion);

        final HttpPatch requestPatch1 = patchObjMethod(idVersion);
        setAuth(requestPatch1, "fedoraAdmin");
        requestPatch1.addHeader("Content-type", "application/sparql-update");
        requestPatch1.setEntity(
                new StringEntity("PREFIX pcdm: <http://pcdm.org/models#> INSERT { <> a pcdm:Object } WHERE {}"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(requestPatch1));

        final String acl = ingestAcl("fedoraAdmin",
                "/acls/10/acl.ttl", idVersion + "/fcr:acl");

        final HttpGet requestGet1 = getObjMethod(idVersion);
        setAuth(requestGet1, "user10");
        assertEquals("user10 can't read object", HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpPost requestPost1 = postObjMethod(idVersion + "/fcr:versions");
        requestPost1.addHeader("Slug", "v0");
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

        final String acl = ingestAcl("fedoraAdmin", "/acls/11/acl.ttl", targetResource + "/fcr:acl");

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
    @Ignore ("Until implemented with Memento")

    public void testAccessByUriToVersionedResources() throws IOException {
        final String idVersion = "/rest/versionResourceUri";
        ingestObj(idVersion);

        final String acl = ingestAcl("fedoraAdmin",
                "/acls/12/acl.ttl", idVersion + "/fcr:acl");

        final HttpGet requestGet1 = getObjMethod(idVersion);
        setAuth(requestGet1, "user12");
        assertEquals("testuser can't read object", HttpStatus.SC_OK, getStatus(requestGet1));

        final HttpPost requestPost1 = postObjMethod(idVersion + "/fcr:versions");
        requestPost1.addHeader("Slug", "v0");
        setAuth(requestPost1, "user12");
        assertEquals("Unable to create a new version", HttpStatus.SC_CREATED, getStatus(requestPost1));

        final HttpGet requestGet2 = getObjMethod(idVersion);
        setAuth(requestGet2, "user12");
        assertEquals("testuser can't read versioned object", HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
    public void testAgentAsUri() throws IOException {
        final String id = "/rest/" + getRandomUniqueId();
        final String testObj = ingestObj(id);

        logger.debug("Anonymous can't read (no ACL): {}", id);
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet1));

        logger.debug("Can username 'smith123' read {} (no ACL)", id);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "smith123");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet2));

        System.setProperty(USER_AGENT_BASE_URI_PROPERTY, "info:user/");
        System.setProperty(GROUP_AGENT_BASE_URI_PROPERTY, "info:group/");

        logger.debug("Can username 'smith123' read {} (overridden system ACL)", id);
        final HttpGet requestGet3 = getObjMethod(id);
        setAuth(requestGet3, "smith123");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet3));

        logger.debug("Can username 'group123' read {} (overridden system ACL)", id);
        final HttpGet requestGet4 = getObjMethod(id);
        setAuth(requestGet4, "group123");
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet4));

        System.clearProperty(USER_AGENT_BASE_URI_PROPERTY);
        System.clearProperty(GROUP_AGENT_BASE_URI_PROPERTY);

        // Add ACL to object
        final String acl = ingestAcl("fedoraAdmin", "/acls/16/acl.ttl", testObj + "/fcr:acl");

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
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/13/acl.ttl", testObj + "/fcr:acl");

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
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/14/acl.ttl", testObj + "/fcr:acl");

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
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/15/acl.ttl", testObj + "/fcr:acl");

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
        final String acl = ingestAcl("fedoraAdmin", "/acls/19/acl.ttl", testObj + "/fcr:acl");

        final HttpHead headReq = new HttpHead(testObj);
        setAuth(headReq, "user19");
        assertEquals(HttpStatus.SC_OK, getStatus(headReq));
    }

    @Test
    public void testOptionsWithReadOnlyUser() throws IOException {
        final String testObj = ingestObj("/rest/test_options");
        final String acl = ingestAcl("fedoraAdmin", "/acls/20/acl.ttl", testObj + "/fcr:acl");

        final HttpOptions optionsReq = new HttpOptions(testObj);
        setAuth(optionsReq, "user20");
        assertEquals(HttpStatus.SC_OK, getStatus(optionsReq));
    }

    protected static HttpResponse HEAD(final String requestURI) throws IOException {
        return HEAD(requestURI, "fedoraAdmin");
    }

    protected static HttpResponse HEAD(final String requestURI, final String username) throws IOException {
        final HttpHead req = new HttpHead(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    protected static HttpResponse PUT(final String requestURI) throws IOException {
        return PUT(requestURI, "fedoraAdmin");
    }

    protected static HttpResponse PUT(final String requestURI, final String username) throws IOException {
        final HttpPut req = new HttpPut(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    protected static HttpResponse DELETE(final String requestURI) throws IOException {
        return DELETE(requestURI, "fedoraAdmin");
    }

    protected static HttpResponse DELETE(final String requestURI, final String username) throws IOException {
        final HttpDelete req = new HttpDelete(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    protected static HttpResponse GET(final String requestURI) throws IOException {
        return GET(requestURI, "fedoraAdmin");
    }

    protected static HttpResponse GET(final String requestURI, final String username) throws IOException {
        final HttpGet req = new HttpGet(requestURI);
        setAuth(req, username);
        return execute(req);
    }

    protected static HttpResponse PATCH(final String requestURI, final HttpEntity body) throws IOException {
        return PATCH(requestURI, body, "fedoraAdmin");
    }

    protected static HttpResponse PATCH(final String requestURI, final HttpEntity body, final String username)
            throws IOException {
        final HttpPatch req = new HttpPatch(requestURI);
        setAuth(req, username);
        if (body != null) {
            req.setEntity(body);
        }
        return execute(req);
    }

    protected static String getLink(final HttpResponse res, final String rel) {
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

    protected String ingestObjWithACL(final String path, final String aclResourcePath) throws IOException {
        final String newURI = ingestObj(path);
        final HttpResponse res = HEAD(newURI);
        final String aclURI = getLink(res, "acl");

        logger.debug("Creating ACL at {}", aclURI);
        ingestAcl("fedoraAdmin", aclResourcePath, aclURI);

        return newURI;
    }

    @Test
    public void testControl() throws IOException {
        final String controlObj = ingestObjWithACL("/rest/control", "/acls/25/control.ttl");
        final String readwriteObj = ingestObjWithACL("/rest/readwrite", "/acls/25/readwrite.ttl");

        final String rwChildACL = getLink(PUT(readwriteObj + "/child"), "acl");
        assertEquals(SC_FORBIDDEN, getStatus(HEAD(rwChildACL, "testuser")));
        assertEquals(SC_FORBIDDEN, getStatus(GET(rwChildACL, "testuser")));
        assertEquals(SC_FORBIDDEN, getStatus(PUT(rwChildACL, "testuser")));
        assertEquals(SC_FORBIDDEN, getStatus(DELETE(rwChildACL, "testuser")));

        final String controlChildACL = getLink(PUT(controlObj + "/child"), "acl");
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
}
