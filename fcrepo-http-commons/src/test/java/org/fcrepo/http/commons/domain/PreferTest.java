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
package org.fcrepo.http.commons.domain;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PreferTest  {

    @Test
    public void testHasReturn() throws ParseException {
        final Prefer prefer = new Prefer("return=representation");

        assertTrue(prefer.hasReturn());
    }

    @Test
    public void testGetReturn() throws ParseException {
        final Prefer prefer = new Prefer("return=representation");

        assertEquals("representation", prefer.getReturn().getValue());
    }

    @Test
    public void testGetReturnParameters() throws ParseException {
        final Prefer prefer = new Prefer("return=representation; include=\"http://www.w3.org/ns/ldp#PreferEmptyContainer\"");

        assertTrue(prefer.hasReturn());
        assertEquals("representation", prefer.getReturn().getValue());

        final String returnParams = prefer.getReturn().getParams().get("include");
        assertTrue(returnParams.contains("http://www.w3.org/ns/ldp#PreferEmptyContainer"));
    }
}
