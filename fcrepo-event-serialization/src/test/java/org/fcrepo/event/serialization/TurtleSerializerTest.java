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

package org.fcrepo.event.serialization;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * <p>
 * TurtleSerializerTest class.
 * </p>
 *
 * @author acoburn
 * @author dbernstein
 */
public class TurtleSerializerTest extends EventSerializerTestBase {
    @Test
    public void testTurtle() {
        mockEvent(path);
        final EventSerializer serializer = new TurtleSerializer();
        final String ttl = serializer.serialize(mockEvent);
        assertTrue(ttl.contains("<http://localhost:8080/fcrepo/rest/path/to/resource>"));
    }

}
