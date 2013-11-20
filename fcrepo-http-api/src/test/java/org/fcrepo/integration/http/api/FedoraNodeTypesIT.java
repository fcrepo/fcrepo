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
package org.fcrepo.integration.http.api;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.fcrepo.kernel.RdfLexicon;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FedoraNodeTypesIT  extends AbstractResourceIT {

    @Test
    public void itShouldContainFcrepoClasses() throws IOException {

        final HttpGet httpGet = new HttpGet(serverAddress + "/fcr:nodetypes");
        httpGet.addHeader("Accept", "application/n-triples");
        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(RdfLexicon.RESTAPI_NAMESPACE + "resource"), RDF.type.asNode(), RDFS.Class.asNode()));
        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(RdfLexicon.RESTAPI_NAMESPACE + "object"), RDF.type.asNode(), RDFS.Class.asNode()));
        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(RdfLexicon.RESTAPI_NAMESPACE + "datastream"), RDF.type.asNode(), RDFS.Class.asNode()));
    }

    @Test
    public void itShouldAllowUpdatesUsingCNDDeclarations() throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress + "/fcr:nodetypes");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("<fedora = 'http://fedora.info/definitions/v4/rest-api#'>\n" +
                                                    "<special = 'info:local#'>\n" +
                                                       "[special:object] > fedora:object mixin").getBytes()));
        httpPost.setEntity(entity);
        final HttpResponse response = client.execute(httpPost);

        assertEquals(204, response.getStatusLine().getStatusCode());


        final HttpGet httpGet = new HttpGet(serverAddress + "/fcr:nodetypes");
        httpGet.addHeader("Accept", "application/n-triples");

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI("info:local#object"), RDFS.subClassOf.asNode(), NodeFactory.createURI(RdfLexicon.RESTAPI_NAMESPACE + "object")));


    }
}
