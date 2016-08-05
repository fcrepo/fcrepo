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
package org.fcrepo.kernel.api.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 *
 * @author barmintor
 *
 */
public class InjectiveConverterTest {

    @Test
    public void testAndThen() {
        final InjectiveConverter<String, char[]> composite = new Reverse().andThen(new ToCharArray());
        final String before = "a,b,c";
        final char[] after = new char[]{'c',',','b',',','a'};
        assertEquals(new String(after), new String(composite.apply(before)));
        assertEquals(before, composite.reverse().apply(after));
        assertNull(composite.apply(null));
    }

    @Test
    public void testComposition() {
        final InjectiveConverter<String, char[]> composite = new ToCharArray().compose(new Reverse());
        final String before = "a,b,c";
        final char[] after = new char[]{'c',',','b',',','a'};
        assertEquals(new String(after), new String(composite.apply(before)));
        assertEquals(before, composite.reverse().apply(after));
        assertNull(composite.apply(null));
    }

    static class Reverse implements InjectiveConverter<String, String> {

        private String nullOrReverse(final String resource) {
            return resource == null ?  null : StringUtils.reverse(resource);
        }

        @Override
        public String toDomain(final String resource) {
            return nullOrReverse(resource);
        }

        @Override
        public String apply(final String a) {
            return nullOrReverse(a);
        }

        @Override
        public boolean inDomain(final String a) {
            return a != null;
        }

    }

    static class ToCharArray implements InjectiveConverter<String, char[]> {

        @Override
        public String toDomain(final char[] resource) {
            return resource == null ? null : new String(resource);
        }

        @Override
        public char[] apply(final String a) {
            return a == null ? null : a.toCharArray();
        }

        @Override
        public boolean inDomain(final String a) {
            return a != null;
        }
    }
}
