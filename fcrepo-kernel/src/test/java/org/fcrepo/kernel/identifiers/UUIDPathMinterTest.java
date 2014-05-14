/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.identifiers;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * <p>UUIDPathMinterTest class.</p>
 *
 * @author awoods
 */
public class UUIDPathMinterTest {

    private static final String PID_PATTERN =
            "[a-f0-9]{3}/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";

    @Test
    public void testMintPid() throws Exception {

        final String pid = new UUIDPathMinter(3, 1).mintPid();

        assertTrue("PID wasn't a UUID path", compile(PID_PATTERN).matcher(pid)
                .find());

    }
}
