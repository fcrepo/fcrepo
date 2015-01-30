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


import com.hp.hpl.jena.query.ResultSet;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeResultsBIO;
import static org.apache.jena.riot.WebContent.contentTypeResultsJSON;
import static org.apache.jena.riot.WebContent.contentTypeResultsXML;
import static org.apache.jena.riot.WebContent.contentTypeTextCSV;
import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests custom MessageBodyWriter
 * @author osmandin
 */
public class ResultSetStreamingOutputIT extends JerseyTest {

    @Test
    public void testContentType() {
        final WebTarget target = target("test/point");

        Response response = target.request(contentTypeTextCSV).post(Entity.text("test")
                , Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeTextTSV).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeRDFXML).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeTurtle).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeNTriples).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeResultsXML).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeResultsJSON).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request(contentTypeResultsBIO).post(Entity.text("test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.OK_200.getStatusCode());

        response = target.request("gamma-rays").post(Entity.text("self-test"), Response.class);
        assertEquals(response.getStatus(), HttpStatus.NOT_ACCEPTABLE_406.getStatusCode());
    }

    @Override
    protected Application configure() {
        return new TestResourceConfig()
                .property("contextConfigLocation",
                        "classpath:spring-test/test-config.xml");
    }

    @Path("test")
    public static class TestHttpResource {

        @POST
        @Path("/point")
        @Produces({contentTypeTextTSV, contentTypeTextCSV, contentTypeResultsJSON,
                contentTypeResultsXML, contentTypeResultsBIO, contentTypeTurtle,
                contentTypeNTriples, contentTypeRDFXML})
        public ResultSet content() {
            return mock(ResultSet.class);
        }

    }
}
