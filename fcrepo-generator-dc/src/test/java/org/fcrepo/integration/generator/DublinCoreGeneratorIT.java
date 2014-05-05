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
package org.fcrepo.integration.generator;

import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;

@ContextConfiguration({"/spring-test/test-container.xml"})
public class DublinCoreGeneratorIT extends AbstractResourceIT {

    private static final Logger log = getLogger(DublinCoreGeneratorIT.class);

    @Test
    public void testJcrPropertiesBasedOaiDc() throws Exception {
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();
        final HttpPatch post = new HttpPatch(location);
        post.setHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        post.setEntity(entity);
        assertEquals(204, getStatus(post));
        final HttpGet getWorstCaseOaiMethod = new HttpGet(location + "/oai:dc");
        getWorstCaseOaiMethod.setHeader("Accept", TEXT_XML);
        final HttpResponse oaiResponse = client.execute(getWorstCaseOaiMethod);

        assertEquals(200, oaiResponse.getStatusLine().getStatusCode());

        final String content = EntityUtils.toString(oaiResponse.getEntity());
        logger.debug("Got content: {}", content);
        assertTrue("Didn't find oai_dc!", compile("oai_dc", DOTALL).matcher(content).find());

        assertTrue("Didn't find dc:identifier!", compile("dc:identifier", DOTALL).matcher(content).find());
    }

    @Test
    public void testWellKnownPathOaiDc() throws Exception {

        final String pid = randomUUID().toString();

        createObject(pid);

        HttpResponse response = client.execute(postDSMethod(pid, "DC", "marbles for everyone"));
        final int status = response.getStatusLine().getStatusCode();
        if (status != 201) {
            log.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals(201, status);

        final HttpGet getWorstCaseOaiMethod = new HttpGet(serverOAIAddress + pid + "/oai:dc");
        getWorstCaseOaiMethod.setHeader("Accept", TEXT_XML);
        response = client.execute(getWorstCaseOaiMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Didn't find our datastream!", compile("marbles for everyone", DOTALL).matcher(content).find());
    }
}
