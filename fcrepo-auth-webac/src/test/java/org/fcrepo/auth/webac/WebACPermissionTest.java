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
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

/**
 * @author peichman
 */
public class WebACPermissionTest {

    private static final URI resourceA = URI.create("http://localhost:8080/fcrepo/test");

    private static final URI resourceB = URI.create("http://localhost:8080/fcrepo/test2");

    @Test
    public void testEquality() {
        final WebACPermission p1 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        final WebACPermission p2 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));
    }

    @Test
    public void testInequalityOfResources() {
        final WebACPermission p1 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        final WebACPermission p2 = new WebACPermission(WEBAC_MODE_READ, resourceB);
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));
    }

    @Test
    public void testInequalityOfModes() {
        final WebACPermission p1 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        final WebACPermission p2 = new WebACPermission(WEBAC_MODE_WRITE, resourceA);
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));
    }

}
