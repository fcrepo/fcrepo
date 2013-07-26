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
package org.fcrepo.integration;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.fcrepo.services.ObjectService;
import org.fcrepo.transform.transformations.LDPathTransform;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import javax.jcr.Repository;
import javax.jcr.Session;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;


@ContextConfiguration({"/spring-test/master.xml", "/spring-test/test-container.xml"})
public class FedoraTransformIT extends AbstractResourceIT {


    @Autowired
    Repository repo;

    @Autowired
    ObjectService objectService;

    @Test
    public void testLdpathWithConfiguredProgram() throws Exception {

        final Session session = repo.login();
        objectService.createObject(session, "/ldpathConfigTestObject");
        session.save();
        session.logout();

        HttpGet postLdpathProgramRequest = new HttpGet(serverAddress + "/ldpathConfigTestObject/fcr:transform/default");
        HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved ldpath feed:\n" + content);

        JsonFactory jsonFactory = new JsonFactory();

        ObjectMapper mapper = new ObjectMapper();
        final JsonParser jsonParser = jsonFactory.createJsonParser(content);

        JsonNode rootNode = mapper.readTree(jsonParser);

        assertEquals(serverAddress + "/ldpathConfigTestObject", rootNode.get(0).get("id").getElements().next().asText());

    }

    @Test
    public void testLdpathWithProgramBody() throws Exception {

        final Session session = repo.login();
        objectService.createObject(session, "/ldpathTestObject");
        session.save();
        session.logout();

        HttpPost postLdpathProgramRequest = new HttpPost(serverAddress + "/ldpathTestObject/fcr:transform");
        BasicHttpEntity e = new BasicHttpEntity();

        String s = "id      = . :: xsd:string ;\n";

        e.setContent(new ByteArrayInputStream(s.getBytes()));

        postLdpathProgramRequest.setEntity(e);
        postLdpathProgramRequest.setHeader("Content-Type", LDPathTransform.APPLICATION_RDF_LDPATH);
        HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved ldpath feed:\n" + content);

        JsonFactory jsonFactory = new JsonFactory();

        ObjectMapper mapper = new ObjectMapper();
        final JsonParser jsonParser = jsonFactory.createJsonParser(content);

        JsonNode rootNode = mapper.readTree(jsonParser);

        assertEquals(serverAddress + "/ldpathTestObject", rootNode.get(0).get("id").getElements().next().asText());

    }
}
