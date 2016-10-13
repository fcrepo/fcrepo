/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.api;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * <p>RdfLexiconTest class.</p>
 *
 * @author ajs6f
 */
public class RdfLexiconTest {

    @Test
    public void repoPredicatesAreManaged() {
        assertTrue(isManagedPredicate.test(createProperty(PREMIS_NAMESPACE + "hasSize")));
        assertTrue(isManagedPredicate.test(createProperty(REPOSITORY_NAMESPACE + "hasParent")));
    }
    @Test
    public void otherPredicatesAreNotManaged() {
        assertFalse(isManagedPredicate.test(createProperty("http://purl.org/dc/elements/1.1/title")));
    }
}
