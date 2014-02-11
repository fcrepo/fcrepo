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

package org.fcrepo.kernel.services.functions;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.security.SecureRandom;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CheckCacheEntryFixityTest {

    @Mock
    private LowLevelCacheEntry mockEntry;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testApply() throws RepositoryException {
        final URI testUri = URI.create("sha1:foo:bar");
        final long testSize = new SecureRandom().nextLong();
        final CheckCacheEntryFixity testObj =
                new CheckCacheEntryFixity(testUri, testSize);
        testObj.apply(mockEntry);
        verify(mockEntry).checkFixity(testUri, testSize);
    }
}
