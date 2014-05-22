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
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;
import static org.junit.Assert.assertEquals;


/**
 * <p>FedoraTransformIT class.</p>
 *
 * @author cbeer
 */
@ContextConfiguration({"/spring-test/master.xml", "/spring-test/test-container.xml"})
public class FedoraTransformIT extends AbstractResourceIT {


    @Autowired
    Repository repo;

    @Autowired
    ObjectService objectService;

    @Test
    public void testLdpathWithConfiguredProgram() throws RepositoryException, IOException {

        final Session session = repo.login();
        try {
            objectService.createObject(session, "/ldpathConfigTestObject");
            session.save();
        } finally {
            session.logout();
        }
        final HttpGet postLdpathProgramRequest =
                new HttpGet(serverAddress + "/ldpathConfigTestObject/fcr:transform/default");
        final HttpResponse response = client.execute(postLdpathProgramRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved ldpath feed:\n" + content);

        final JsonNode rootNode = new ObjectMapper().readTree(new JsonFactory().createJsonParser(content));

        assertEquals("Failed to retrieve correct identifier in JSON!", serverAddress + "/ldpathConfigTestObject",
                rootNode.get("id").getElements().next().asText());

    }

    @Test
    public void testLdpathWithProgramBody() throws RepositoryException, IOException {
        final Session session = repo.login();
        try {
            objectService.createObject(session, "/ldpathTestObject");
            session.save();
        } finally {
            session.logout();
        }

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
