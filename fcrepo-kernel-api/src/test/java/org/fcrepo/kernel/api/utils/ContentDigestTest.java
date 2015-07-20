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
package org.fcrepo.kernel.api.utils;

import static java.net.URI.create;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.fcrepo.kernel.api.utils.ContentDigest.getAlgorithm;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * <p>ContentDigestTest class.</p>
 *
 * @author ksclarke
 */
public class ContentDigestTest {

    @Test
    public void testSHA_1() {
        assertEquals("Failed to produce a proper content digest URI!",
                create("urn:sha1:fake"), asURI("SHA-1", "fake"));
    }

    @Test
    public void testSHA1() {
        assertEquals("Failed to produce a proper content digest URI!",
                create("urn:sha1:fake"), asURI("SHA1", "fake"));
    }

    @Test
    public void testGetAlgorithm() {
        assertEquals("Failed to produce a proper digest algorithm!", "SHA-1",
                getAlgorithm(asURI("SHA-1", "fake")));
    }
}
