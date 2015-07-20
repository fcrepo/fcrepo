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
package org.fcrepo.kernel.modeshape.rdf.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import javax.jcr.Session;
import java.util.Arrays;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 * @author escowles
 * @since 2015-04-24
 */
@RunWith(Parameterized.class)
public class PrefixingIdentifierTranslatorTest {

    @Parameterized.Parameter(value = 0)
    public String internalId;

    @Parameterized.Parameter(value = 1)
    public String externalId;

    private PrefixingIdentifierTranslator testObj;

    @Mock
    private Session mockSession;

    @Parameterized.Parameters
    public static Iterable<String[]> data() {
        return Arrays.asList(new String[][]{
                {"/", "http://example.com:8080/rest/"},
                {"/some/path", "http://example.com:8080/rest/some/path"},
                {"/some/path/#/with-a-hash-uri", "http://example.com:8080/rest/some/path#with-a-hash-uri"}
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new PrefixingIdentifierTranslator(mockSession, "http://example.com:8080/rest/");
    }

    @Test
    public void testToString() {
        assertEquals(internalId, testObj.asString(createResource(externalId)));
    }

    @Test
    public void testToDomain() {
        assertEquals(createResource(externalId), testObj.toDomain(internalId));
    }
}
