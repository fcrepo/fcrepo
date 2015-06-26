/**
 * Copyright 2015 DuraSpace, Inc.
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

import static java.util.UUID.randomUUID;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * <p>FedoraTransformIT class.</p>
 *
 * @author cbeer
 */
@ContextConfiguration({"/spring-test/test-container.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class FedoraTransformIT extends AbstractResourceIT {

    @Test
    public void testLdpathWithConfiguredProgram() throws IOException {

        final String pid = "testLdpathWithConfiguredProgram-" + randomUUID();
        createObject(pid);
        final HttpGet postLdpathProgramRequest
                = new HttpGet(serverAddress + "/" + pid + "/fcr:transform/default");
        final HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved ldpath feed:\n" + content);

        final JsonNode rootNode = new ObjectMapper().readTree(new JsonFactory().createParser(content));

        assertEquals("Failed to retrieve correct identifier in JSON!", serverAddress + "/" + pid,
                rootNode.get(0).get("id").elements().next().asText());

    }

    @Test
    public void testLdpathWithProgramBody() throws ParseException, IOException {

        final String pid = UUID.randomUUID().toString();
        createObject(pid);

        final HttpPost postLdpathProgramRequest = new HttpPost(serverAddress + "/" + pid + "/fcr:transform");
        final BasicHttpEntity e = new BasicHttpEntity();

        final String s = "id = . :: xsd:string ;\n";

        e.setContent(new ByteArrayInputStream(s.getBytes()));

        postLdpathProgramRequest.setEntity(e);
        postLdpathProgramRequest.setHeader("Content-Type", APPLICATION_RDF_LDPATH);
        final HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved LDPath result:\n" + content);

        final JsonNode rootNode = new ObjectMapper().readTree(new JsonFactory().createParser(content));

        assertEquals("Failed to retrieve correct identifier in JSON!", serverAddress + "/" + pid, rootNode
                .get(0).get("id").elements().next().asText());

    }
}
