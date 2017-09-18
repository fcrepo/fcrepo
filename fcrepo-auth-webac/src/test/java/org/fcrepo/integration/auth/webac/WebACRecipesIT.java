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
import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESS_CONTROL_VALUE;
import static org.fcrepo.auth.webac.WebACRolesProvider.GROUP_AGENT_BASE_URI_PROPERTY;
import static org.fcrepo.auth.webac.WebACRolesProvider.ROOT_AUTHORIZATION_PROPERTY;
import static org.fcrepo.auth.webac.WebACRolesProvider.USER_AGENT_BASE_URI_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.ws.rs.core.Link;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Eichman
 * @author whikloj
 * @since September 4, 2015
 */
public class WebACRecipesIT extends AbstractResourceIT {

    private static final Logger logger = LoggerFactory.getLogger(WebACRecipesIT.class);

    /**
     * Convenience method to create an ACL with 0 or more authorization resources in the respository.
     */
    private String ingestAcl(final String username, final String aclResourcePath,
            final String... authorizationResourcePaths) throws IOException {

        // create the ACL
        final HttpResponse aclResponse = ingestTurtleResource(username, aclResourcePath, "/rest");

        // get the URI to the newly created resource
        final String aclURI = aclResponse.getFirstHeader("Location").getValue();

        // add all the authorizations
        for (final String authorizationResourcePath : authorizationResourcePaths) {
            ingestTurtleResource(username, authorizationResourcePath, aclURI.replace(serverAddress, ""));
        }

        return aclURI;
    }

    /**
     * Convenience method to POST the contents of a Turtle file to the repository to create a new resource. Returns
     * the HTTP response from that request. Throws an IOException if the server responds with anything other than a
     * 201 Created response code.
     */
    private HttpResponse ingestTurtleResource(final String username, final String path, final String requestURI)
            throws IOException {
        final HttpPost request = postObjMethod(requestURI);

        logger.debug("POST to {} to create {}", requestURI, path);

        setAuth(request, username);

        final InputStream file = this.getClass().getResourceAsStream(path);
        final InputStreamEntity fileEntity = new InputStreamEntity(file);
        request.setEntity(fileEntity);
        request.setHeader("Content-Type", "text/turtle;charset=UTF-8");

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

    private String ingestDatastream(final String path, final String ds) throws IOException {
        final HttpPut request = putDSMethod(path, ds, "some not so random content");
        setAuth(request, "fedoraAdmin");
        try (final CloseableHttpResponse response = execute(request)) {
            assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("Location").getValue();
        }
    }

    /**
     * Convenience method to link a Resource to a WebACL resource
     *
     * @param protectedResource path of the resource to be protected by the
     * @param aclResource path of the Acl resource
     */
    private void linkToAcl(final String protectedResource, final String aclResource)
            throws IOException {
        final HttpPatch request = patchObjMethod(protectedResource.replace(serverAddress, ""));
        setAuth(request, "fedoraAdmin");
        request.setHeader("Content-type", "application/sparql-update");
        request.setEntity(new StringEntity(
                "INSERT { <> <" + WEBAC_ACCESS_CONTROL_VALUE + "> <" + aclResource + "> . } WHERE {}"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(request));
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
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/01/acl.ttl", "/acls/01/authorization.ttl");
        linkToAcl(testObj, acl1);
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
            // This gets the Link headers and filters for the correct one (aclLink::equals) defined above.
            final Optional<String> header = stream(response.getHeaders("Link")).map(Header::getValue)
                    .filter(aclLink::equals).findFirst();
            // So you either have the correct Link header or you get nothing.
            assertTrue("Missing Link header to ACL on parent", header.isPresent());
        }
    }

    @Test
    public void scenario2() throws IOException {
        final String id = "/rest/box/bag/collection";
        final String testObj = ingestObj(id);
        final String acl2 = ingestAcl("fedoraAdmin", "/acls/02/acl.ttl", "/acls/02/authorization.ttl");
        linkToAcl(testObj, acl2);

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
        final String testObj2 = ingestObj(idLight);
        final String acl3 =
                ingestAcl("fedoraAdmin", "/acls/03/acl.ttl", "/acls/03/auth_open.ttl", "/acls/03/auth_restricted.ttl");
        linkToAcl(testObj, acl3);

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
        final String testObj = ingestObj(id);
        final String acl4 = ingestAcl("fedoraAdmin", "/acls/04/acl.ttl", "/acls/04/auth1.ttl", "/acls/04/auth2.ttl");
        linkToAcl(testObj, acl4);

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
        final String testObj = ingestObj("/rest/mixedCollection");
        final String publicObj = ingestObj(idPublic);
        final HttpPatch patch = patchObjMethod(idPublic);
        final String acl5 =
                ingestAcl("fedoraAdmin", "/acls/05/acl.ttl", "/acls/05/auth_open.ttl", "/acls/05/auth_restricted.ttl");
        linkToAcl(testObj, acl5);

        setAuth(patch, "fedoraAdmin");
        patch.setHeader("Content-type", "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <> a <http://example.com/terms#publicImage> . } WHERE {}"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patch));

        final String privateObj = ingestObj(idPrivate);

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

        final String acl9 = ingestAcl("fedoraAdmin", "/acls/09/acl.ttl", "/acls/09/authorization.ttl");
        linkToAcl(testObj, acl9);

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

    @Test
    public void testAccessToRoot() throws IOException {
        final String id = "/rest/" + getRandomUniqueId();
        final String testObj = ingestObj(id);
        final String acl = ingestAcl("fedoraAdmin", "/acls/06/acl.ttl", "/acls/06/authorization.ttl",
                "/acls/06/noslash.ttl");

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
        linkToAcl("/rest/", acl);

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
    public void testAccessToBinary() throws IOException {
        // Block access to "book"
        final String idBook = "/rest/book";
        ingestObj(idBook);

        // Open access datastream, "file"
        final String id = idBook + "/file";
        final String testObj = ingestDatastream(idBook, "file");
        final String acl = ingestAcl("fedoraAdmin",
                "/acls/07/acl.ttl",
                "/acls/07/authorization.ttl",
                "/acls/07/authorization-book.ttl");

        linkToAcl(id + "/fcr:metadata", acl);

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
        final String acl = ingestAcl("fedoraAdmin", "/acls/08/acl.ttl", "/acls/08/authorization.ttl");
        linkToAcl(testObj, acl);

        logger.debug("Anonymous can't read");
        final HttpGet requestGet1 = getObjMethod(id);
        assertEquals(HttpStatus.SC_FORBIDDEN, getStatus(requestGet1));

        logger.debug("Can username 'user08' read {}", testObj);
        final HttpGet requestGet2 = getObjMethod(id);
        setAuth(requestGet2, "user08");
        assertEquals(HttpStatus.SC_OK, getStatus(requestGet2));
    }

    @Test
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
                "/acls/10/acl.ttl",
                "/acls/10/authorization.ttl");

        linkToAcl(idVersion, acl);

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

        final String acl = ingestAcl("fedoraAdmin", "/acls/11/acl.ttl", "/acls/11/authorization.ttl");
        linkToAcl(targetResource, acl);

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
        final String idVersion = "/rest/versionResourceUri";
        ingestObj(idVersion);

        final String acl = ingestAcl("fedoraAdmin",
                "/acls/12/acl.ttl",
                "/acls/12/authorization.ttl");

        linkToAcl(idVersion, acl);

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
        final String acl = ingestAcl("fedoraAdmin", "/acls/16/acl.ttl", "/acls/16/authorization.ttl");

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
        linkToAcl(testObj, acl);

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
    public void testInvalidAccessControlLink() throws IOException {
        final String id = "/rest/" + getRandomUniqueId();
        ingestObj(id);

        final HttpPatch patchReq = patchObjMethod(id);
        setAuth(patchReq, "fedoraAdmin");
        patchReq.addHeader("Content-type", "application/sparql-update");
        patchReq.setEntity(new StringEntity(
                "INSERT { <> <" + WEBAC_ACCESS_CONTROL_VALUE + "> \"/rest/acl/badAclLink\" . } WHERE {}"));
        assertEquals(HttpStatus.SC_NO_CONTENT, getStatus(patchReq));

        final HttpGet getReq = getObjMethod(id);
        setAuth(getReq, "fedoraAdmin");
        assertEquals("Non-URI accessControl property did not throw Exception", HttpStatus.SC_BAD_REQUEST,
                getStatus(getReq));
    }

    @Test
    public void testRegisterNamespace() throws IOException {
        final String testObj = ingestObj("/rest/test_namespace");
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/13/acl.ttl", "/acls/13/authorization.ttl");
        linkToAcl(testObj, acl1);

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
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/14/acl.ttl", "/acls/14/authorization.ttl");
        linkToAcl(testObj, acl1);

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
        final String acl1 = ingestAcl("fedoraAdmin", "/acls/15/acl.ttl", "/acls/15/authorization.ttl");
        linkToAcl(testObj, acl1);

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
    public void testNoAccessRolesResource() throws IOException {
        final String objectPath = "/rest/test_accessroles";
        ingestObj(objectPath);
        final String accessRolesResource = objectPath + "/fcr:accessroles";
        final HttpGet getReq = getObjMethod(accessRolesResource);
        setAuth(getReq, "fedoraAdmin");
        assertEquals(HttpStatus.SC_NOT_FOUND, getStatus(getReq));
    }


}
