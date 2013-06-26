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
package org.fcrepo.services.functions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Test;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date Apr 3, 2013
 */
public class CheckCacheEntryFixityTest {

    /**
     * @throws BinaryStoreException, NoSuchAlgorithmException 
     * @todo Add Documentation.
     */
    @Test
    public void testApply() throws BinaryStoreException, BinaryStoreException, NoSuchAlgorithmException {
        final URI testUri = URI.create("sha1:foo:bar");
        final long testSize = new SecureRandom().nextLong();
        final CheckCacheEntryFixity testObj =
                new CheckCacheEntryFixity(testUri, testSize);
        final LowLevelCacheEntry mockEntry = mock(LowLevelCacheEntry.class);
        testObj.apply(mockEntry);
        verify(mockEntry).checkFixity(testUri, testSize);
    }
}
