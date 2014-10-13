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
package org.fcrepo.integration.http.api;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.RdfLexicon.VERSIONING_POLICY;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>FedoraVersionsIT class.</p>
 *
 * @author awoods
 */
@UseAutoVersioningConfiguration
public class FedoraAutoVersioningIT extends AbstractVersioningIT {

    @Test
    public void testRepositoryWideAutoVersioning() throws IOException {
        final String objName = getRandomUniquePid();
        final String dsName = "datastream";

        createObject(objName);

        final GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy",
                initialVersion.contains(ANY,
                                        createResource(serverAddress + objName).asNode(),
                                        VERSIONING_POLICY.asNode(),
                                        createLiteral("auto-version")));

        testDatastreamContentUpdatesCreateNewVersions(objName, dsName);

    }

}
