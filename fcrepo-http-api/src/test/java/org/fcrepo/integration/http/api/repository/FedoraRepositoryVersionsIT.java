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
package org.fcrepo.integration.http.api.repository;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>FedoraRepositoryVersionsIT class.</p>
 *
 * @author lsitu
 */
public class FedoraRepositoryVersionsIT extends AbstractResourceIT {

    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        final String pid = "";

        // AddMixin() got javax.jcr.RepositoryException: java.lang.ClassCastException:
        // org.modeshape.jcr.JcrSystemNode cannot be cast to org.modeshape.jcr.JcrVersionNode
        // By default, the versionable Mixin type is available for the root level
        addMixin( pid, MIX_NAMESPACE + "versionable" );

        // Create version got the same error as addMixin() above.
        final HttpPost postVersion = postObjMethod(getServerPath(pid));
        final HttpResponse response = client.execute(postVersion);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode() );

        final HttpGet getVersion =
            new HttpGet(getServerPath (pid) + "/fcr:versions");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject = createResource(getServerPath (""));
        assertFalse("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));
    }
}
