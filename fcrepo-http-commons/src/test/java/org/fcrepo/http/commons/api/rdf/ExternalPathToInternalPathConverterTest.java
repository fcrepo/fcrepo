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
package org.fcrepo.http.commons.api.rdf;

import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.util.Arrays;

import org.fcrepo.kernel.api.RdfLexicon;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


/**
 * 
 * @author barmintor
 *
 */
@RunWith(Parameterized.class)
public class ExternalPathToInternalPathConverterTest {

    @Parameterized.Parameter(value = 0)
    public String internalId;

    @Parameterized.Parameter(value = 1)
    public String externalId;

    private ExternalPathToInternalPathConverter testObj;

    @Parameterized.Parameters
    public static Iterable<String[]> data() {
        return Arrays.asList(new String[][]{
                { "/some/path", "/some/path" },
                { RdfLexicon.REPOSITORY_NAMESPACE + "test2/" + JCR_CONTENT, JCR_NAMESPACE + "test2/jcr:content"},
                { "/some/path/#/with-a-hash-uri", "/some/path#with-a-hash-uri" },
                { "/some/path/#/with%2Fa%2Fhash%2Furi", "/some/path#with/a/hash/uri" }
        });
    }

    @Before
    public void setUp() {
        testObj = new ExternalPathToInternalPathConverter();
    }

    @Test
    public void testApply() {
        assertEquals(internalId, testObj.apply(externalId));
    }

    @Test
    public void testToDomain() {
        assertEquals(externalId, testObj.toDomain(internalId));
    }
}
