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

package org.fcrepo.jms.legacy;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.fcrepo.jms.legacy.EntryFactory;
import org.junit.Test;

public class EntryFactoryTest {

    private static String ATOM =
            "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:fedora-types=\"http://www.fedora.info/definitions/1/0/types/\"><id>urn:uuid:a447ba5a-4ff5-42e1-8d4d-b728ea86155c</id><category term=\"info:fedora/fedora-system:ATOM-APIM-1.0\" scheme=\"http://www.fedora.info/definitions/1/0/types/formatURI\" label=\"format\"/><category term=\"4.0.0-SNAPSHOT\" scheme=\"info:fedora/fedora-system:def/view#version\" label=\"version\"/></entry>";

    @Test
    public void testNewEntry() {
        Entry actual = EntryFactory.newEntry();
        List<Category> categories =
                actual.getCategories(EntryFactory.FORMAT_PREDICATE);
        assertEquals(1, categories.size());
        Category category = categories.get(0);
        System.out.println("HELLO! " + actual.toString());
        assertEquals(EntryFactory.FORMAT, category.getTerm());
        assertEquals("format", category.getLabel());
    }

    @Test
    public void testParse() {
        EntryFactory.parse(new StringReader(ATOM));
    }
}
