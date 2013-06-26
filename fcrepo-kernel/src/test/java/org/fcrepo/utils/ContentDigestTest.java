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
package org.fcrepo.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Mar 7, 2013
 */
public class ContentDigestTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSHA_1() {
        assertEquals("Failed to produce a proper content digest URI!", URI
                .create("urn:sha1:fake"), ContentDigest.asURI("SHA-1", "fake"));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testSHA1() {
        assertEquals("Failed to produce a proper content digest URI!", URI
                .create("urn:sha1:fake"), ContentDigest.asURI("SHA1", "fake"));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetAlgorithm() {
        assertEquals("Failed to produce a proper digest algorithm!", "SHA-1",
                     ContentDigest.getAlgorithm(ContentDigest.asURI("SHA-1",
                                                                    "fake")));
    }
}
