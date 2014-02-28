/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;
import static org.junit.Assert.assertEquals;


/**
 * <p>FedoraTransformIT class.</p>
 *
 * @author cbeer
 */
@ContextConfiguration({"/spring-test/test-container.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class FedoraTransformIT extends AbstractResourceIT {

    protected HttpResponse createObject(final String pid) throws IOException {
        final HttpPost httpPost =  new HttpPost(serverAddress + "/");
        if (pid.length() > 0) {
            httpPost.addHeader("Slug", pid);
        }
        final HttpResponse response = client.execute(httpPost);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

    @Test
    public void testLdpathWithConfiguredProgram() throws RepositoryException, IOException {

        createObject("ldpathConfigTestObject");
        final HttpGet postLdpathProgramRequest
                = new HttpGet(serverAddress + "/ldpathConfigTestObject/fcr:transform/default");
        final HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved ldpath feed:\n" + content);

        final JsonNode rootNode = new ObjectMapper().readTree(new JsonFactory().createJsonParser(content));

        assertEquals("Failed to retrieve correct identifier in JSON!", serverAddress + "/ldpathConfigTestObject",
                rootNode.get("id").getElements().next().asText());

    }

    @Test
    public void testLdpathWithProgramBody() throws Exception {

        createObject("ldpathTestObject");

        final HttpPost postLdpathProgramRequest = new HttpPost(serverAddress + "/ldpathTestObject/fcr:transform");
        final BasicHttpEntity e = new BasicHttpEntity();

        final String s = "id = . :: xsd:string ;\n";

        e.setContent(new ByteArrayInputStream(s.getBytes()));

        postLdpathProgramRequest.setEntity(e);
        postLdpathProgramRequest.setHeader("Content-Type", APPLICATION_RDF_LDPATH);
        final HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved LDPath result:\n" + content);

        final JsonNode rootNode = new ObjectMapper().readTree(new JsonFactory().createJsonParser(content));

        assertEquals("Failed to retrieve correct identifier in JSON!", serverAddress + "/ldpathTestObject", rootNode
                .get("id").getElements().next().asText());

    }
}
