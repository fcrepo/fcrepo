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
package org.fcrepo.http.commons.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 * @author ajs6f
 */
public class SinglePreferTest  {

    protected SinglePrefer createTestPreferTypeFromHeader(final String header) {
        return new SinglePrefer(header);
    }

    @Test
    public void testHasReturn() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("return=representation");

        assertTrue(prefer.hasReturn());
    }

    @Test
    public void testGetReturn() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("return=representation");

        assertEquals("representation", prefer.getReturn().getValue());
    }

    @Test
    public void testGetReturnParameters() {
        final SinglePrefer prefer =
                createTestPreferTypeFromHeader("return=representation; "
                        + "include=\"http://www.w3.org/ns/ldp#PreferMinimalContainer\"");

        assertTrue(prefer.hasReturn());
        assertEquals("representation", prefer.getReturn().getValue());

        final String returnParams = prefer.getReturn().getParams().get("include");
        assertTrue(returnParams.contains("http://www.w3.org/ns/ldp#PreferMinimalContainer"));
    }

    @Test
    public void testHasHandling() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("handling=strict");

        assertTrue(prefer.hasHandling());
    }

    @Test
    public void testGetHandling() {
        final SinglePrefer prefer = createTestPreferTypeFromHeader("handling=lenient");

        assertEquals("lenient", prefer.getHandling().getValue());
    }

    @Test
    public void testGetHandlingParameters() {
        final SinglePrefer prefer =
                createTestPreferTypeFromHeader("handling=lenient; some=\"parameter\"");

        assertTrue(prefer.hasHandling());
        assertEquals("lenient", prefer.getHandling().getValue());

        final String returnParams = prefer.getHandling().getParams().get("some");
        assertTrue(returnParams.contains("parameter"));
    }
}
