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
import org.fcrepo.search.impl.InstantParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author dbernstein
 */
public class InstantParserTest {
    @Test
    public void test() {
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("2020-01-01").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("20200101").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("2020-01-01T00:00:00Z").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("2020-01-01 00:00:00").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("20200101 00:00:00").toString());
        assertEquals("2020-01-01T07:00:00Z", InstantParser.parse("2020-01-01T00:00:00-07:00").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("20200101 00:00:00").toString());
        assertEquals("2020-01-01T00:00:00Z", InstantParser.parse("Wed, 1 Jan 2020 00:00:00 GMT").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidString() {
        InstantParser.parse("2020-01-01 24").toString();
    }
}
