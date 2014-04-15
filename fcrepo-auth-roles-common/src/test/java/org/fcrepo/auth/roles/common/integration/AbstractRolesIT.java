/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.auth.roles.common.integration;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * @author Gregory Jansen
 * @author Scott Prater
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractRolesIT {

    private static Logger logger = getLogger(AbstractRolesIT.class);

    protected static final int SERVER_PORT = Integer.parseInt(System
            .getProperty("test.port", "8080"));

    protected static final String HOSTNAME = "localhost";

    protected static final String SUFFIX = "fcr:accessroles";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + "/rest/";

    protected final PoolingHttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager();

    protected static CloseableHttpClient client;

    private static List<RolesFadTestObjectBean> test_objs;

    private static boolean is_setup = false;

    public AbstractRolesIT() {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client =
                HttpClientBuilder.create().setConnectionManager(
                        connectionManager).build();
    }

    @Before
    public void setUp() throws Exception {
        if (is_setup == false) {
            test_objs = getTestObjs();
            for (final RolesFadTestObjectBean obj : test_objs) {
                deleteTestObject(obj);
                ingestObject(obj);
            }
            is_setup = true;
            logger.info("SETUP SUCCESSFUL");
        }
    }

    public int canRead(final String username, final String path,
            final boolean is_authenticated)
                    throws IOException {
        // get the object info
        final HttpGet method = getObjectMethod(path);
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "canRead REST response status code [user: {}, path: {}]: {}",
                username, path, status);
        return status;
    }

    public int canDelete(final String username, final String path,
            final boolean is_authenticated) throws IOException {
        // get the object info
        final HttpDelete method = deleteObjMethod(path);
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "canDelete REST response status code [user: {}, path: {}]: {}",
                username, path, status);
        return status;
    }

    public int canAddDS(final String username, final String path,
            final String dsName, final boolean is_authenticated)
                    throws IOException {
        final HttpPost method =
                postDSMethod(path, dsName, "This is the datastream contents.");
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug("canAddDS REST response status code:  {}", status);
        return status;
    }

    public int canUpdateDS(final String username, final String path,
            final String dsName, final boolean is_authenticated)
                    throws IOException {
        final HttpPut method =
                putDSMethod(path, dsName, "This is my updated content.");
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug("canUpdateDS REST response status code:  {}", status);
        return status;
    }

    public int canAddACL(final String username, final String path,
            final String principal, final String role,
            final boolean is_authenticated)
                    throws IOException {
        final Map<String, String> tmap = new HashMap<>();
        tmap.put(principal, role);
        final List<Map<String, String>> acls = singletonList(tmap);
        final String jsonACLs = createJsonACLs(acls);
        final HttpPost method = postRolesMethod(path);
        if (is_authenticated) {
            setAuth(method, username);
        }
        method.addHeader("Content-Type", "application/json");
        final StringEntity entity = new StringEntity(jsonACLs, "utf-8");
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug("canAddACL REST response status code:  {}", status);
        return status;
    }

    public int canGetRoles(final String username, final String path,
            final boolean is_authenticated) throws IOException {
        // get the roles
        final HttpGet method = getRolesMethod(path);
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "canGetRoles REST response status code [user: {}, path: {}]: {}",
                username, path, status);
        return status;
    }

    public int canGetEffectiveRoles(final String username,
            final String path, final boolean is_authenticated)
                    throws IOException {
        // get the effective roles
        final HttpGet method = getEffectiveRolesMethod(path);
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "canGetRoles REST response status code [user: {}, path: {}]: {}",
                username, path, status);
        return status;
    }

    public int canDeleteRoles(final String username, final String path,
            final boolean is_authenticated) throws IOException {
        // delete the roles
        final HttpDelete method = deleteRolesMethod(path);
        if (is_authenticated) {
            setAuth(method, username);
        }
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "canDeleteRoles REST response status code [user: {}, path: {}]: {}",
                username, path, status);
        return status;
    }

    protected HttpGet getRolesMethod(final String param) {
        final HttpGet get = new HttpGet(serverAddress + param + "/" + SUFFIX);
        logger.debug("GET: {}", get.getURI());
        return get;
    }



    protected HttpGet getEffectiveRolesMethod(final String param) {
        final HttpGet get =
                new HttpGet(serverAddress + param + "/" + SUFFIX + "?effective");
        logger.debug("GET: {}", get.getURI());
        return get;
    }



    protected HttpGet getObjectMethod(final String param) {
        final HttpGet get = new HttpGet(serverAddress + param);
        logger.debug("GET: {}", get.getURI());
        return get;
    }



    protected HttpPost postObjMethod(final String param) {
        final HttpPost post = new HttpPost(serverAddress + param);
        logger.debug("POST: {}", post.getURI());
        return post;
    }



    protected HttpPut putDSMethod(final String objectPath, final String ds,
            final String content) throws UnsupportedEncodingException {
        final HttpPut put =
                new HttpPut(serverAddress + objectPath + "/" + ds +
                        "/fcr:content");
        put.setEntity(new StringEntity(content));
        logger.debug("PUT: {}", put.getURI());
        return put;
    }



    protected HttpPost postDSMethod(final String objectPath,
            final String ds, final String content)
                    throws UnsupportedEncodingException {
        final HttpPost post =
                new HttpPost(serverAddress + objectPath + "/" + ds +
                        "/fcr:content");
        post.setEntity(new StringEntity(content));
        return post;
    }



    protected HttpPost postRolesMethod(final String param) {
        final HttpPost post =
                new HttpPost(serverAddress + param + "/" + SUFFIX);
        logger.debug("POST: {}", post.getURI());
        return post;
    }



    protected HttpDelete deleteObjMethod(final String param) {
        final HttpDelete delete = new HttpDelete(serverAddress + param);
        logger.debug("DELETE: {}", delete.getURI());
        return delete;
    }



    protected HttpDelete deleteRolesMethod(final String param) {
        final HttpDelete delete = new HttpDelete(serverAddress + param + "/" + SUFFIX);
        logger.debug("DELETE: {}", delete.getURI());
        return delete;
    }



    protected HttpResponse execute(final HttpUriRequest method)
            throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                method.getURI());
        return client.execute(method);
    }



    protected int getStatus(final HttpUriRequest method)
            throws IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
        }
        return result;
    }



    protected int postRoles(final String path, final String json_roles)
            throws ParseException, IOException {
        final HttpPost method = postRolesMethod(path);
        setAuth(method, "fedoraAdmin");
        method.addHeader("Content-Type", "application/json");
        final StringEntity entity = new StringEntity(json_roles, "utf-8");
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        assertNotNull("There must be content for a post.", response.getEntity());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("post response content: \n {}", content);
        return response.getStatusLine().getStatusCode();
    }

    protected Map<String, List<String>> getRoles(final String path)
            throws ParseException, IOException {
        final HttpGet method = getRolesMethod(path);
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "getRoles REST response status code [user: {}, path: {}]: {}",
                path, status);
        final HttpEntity entity = response.getEntity();
        final String content = EntityUtils.toString(entity);
        logger.debug("content: {}", content);
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, List<String>> result =
            mapper.readValue(content,
                    new TypeReference<Map<String, List<String>>>() {});
        return result;
    }

    protected Map<String, List<String>> getEffectiveRoles(final String path)
            throws ParseException, IOException {
        final HttpGet method = getEffectiveRolesMethod(path);
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        logger.debug(
                "getEffectiveRoles REST response status code [user: {}, path: {}]: {}",
                path, status);
        final HttpEntity entity = response.getEntity();
        final String content = EntityUtils.toString(entity);
        logger.debug("content: {}", content);
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, List<String>> result =
            mapper.readValue(content,
                    new TypeReference<Map<String, List<String>>>() {});
        return result;
    }

    protected void deleteTestObject(
            final RolesFadTestObjectBean obj) {
        try {
            final HttpDelete method = deleteObjMethod(obj.getPath());
            setAuth(method, "fedoraAdmin");
            client.execute(method);
        } catch (final Throwable ignored) {
            logger.debug("object {} doesn't exist -- not deleting", obj
                    .getPath());

        }
    }



    protected void
    ingestObject(final RolesFadTestObjectBean obj)
            throws Exception {
        final HttpPost method = postObjMethod(obj.getPath());
        setAuth(method, "fedoraAdmin");
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);

        addObjectACLs(obj);
        addDatastreams(obj);
    }



    protected String makeJson(final Map<String, List<String>> roles) {
        final ObjectMapper mapper = new ObjectMapper();
        final StringWriter sw = new StringWriter();
        try {
            mapper.writeValue(sw, roles);
            return sw.toString();
        } catch (final IOException e) {
            throw new Error(e);
        }
    }



    private static void setAuth(final AbstractHttpMessage method, final String username) {
        final String creds = username + ":password";
        // in test configuration we don't need real passwords
        final String encCreds =
                new String(Base64.encodeBase64(creds.getBytes()));
        final String basic = "Basic " + encCreds;
        method.setHeader("Authorization", basic);
    }

    private void addObjectACLs(
            final RolesFadTestObjectBean obj)
                    throws Exception {
        if (obj.getACLs().size() > 0) {
            final String jsonACLs = createJsonACLs(obj.getACLs());
            assertEquals(CREATED.getStatusCode(), postRoles(obj.getPath(),
                    jsonACLs));
        }
    }

    private void addDatastreams(
            final RolesFadTestObjectBean obj)
                    throws Exception {
        for (final Map<String, String> entries : obj.getDatastreams()) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                final String dsid = entry.getKey();
                final HttpPost method =
                        postDSMethod(obj.getPath(), dsid, entry.getValue());
                setAuth(method, "fedoraAdmin");
                final HttpResponse response = client.execute(method);
                final String content =
                        EntityUtils.toString(response.getEntity());
                final int status = response.getStatusLine().getStatusCode();
                assertEquals("Didn't get a CREATED response! Got content:\n" +
                        content, CREATED.getStatusCode(), status);
                addDatastreamACLs(obj, dsid);
            }
        }
    }

    private void addDatastreamACLs(
            final RolesFadTestObjectBean obj,
            final String dsid) throws Exception {
        if (obj.getDatastreamACLs(dsid) != null) {
            final String jsonACLs = createJsonACLs(obj.getDatastreamACLs(dsid));
            logger.debug("addDatastreamACLs:  Datastream path: {}/{}", obj
                    .getPath(), dsid);
            logger.debug("addDatastreamACLs:  JSON acls: {}{}", jsonACLs);
            assertEquals(CREATED.getStatusCode(), postRoles(obj.getPath() +
                    "/" + dsid, jsonACLs));
        }
    }

    private String createJsonACLs(
            final List<Map<String, String>> principals_and_roles) {
        final Map<String, List<String>> acls =
                new HashMap<>();

        for (final Map<String, String> entries : principals_and_roles) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                final String key = entry.getKey();
                if (acls.containsKey(key)) {
                    acls.get(key).add(entry.getValue());
                } else {
                    acls.put(key, new ArrayList<String>(Arrays.asList(new String[] { entry.getValue() })));
                }
            }
        }
        return makeJson(acls);
    }

    protected abstract List<RolesFadTestObjectBean> getTestObjs();
}
