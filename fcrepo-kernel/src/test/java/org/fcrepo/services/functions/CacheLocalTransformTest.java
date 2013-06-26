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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.lang.reflect.Field;

import org.fcrepo.utils.LowLevelCacheEntry;
import org.fcrepo.utils.impl.CacheStoreEntry;
import org.junit.Test;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;

import org.infinispan.Cache;
import org.infinispan.CacheImpl;
import org.infinispan.loaders.CacheStore;
import com.google.common.base.Function;

public class CacheLocalTransformTest {

    @Test
    public void testTransform() throws Exception {

        GetCacheStore cacheExtractor = mock(GetCacheStore.class);
        Field field = CacheLocalTransform.class.getDeclaredField("TRANSFORM");
        field.setAccessible(true);
        field.set(CacheLocalTransform.class, cacheExtractor);

        CacheStore mockCacheStore = mock(CacheStore.class);

        when(cacheExtractor.apply(any(Cache.class))).thenReturn(mockCacheStore);

        final BinaryKey key = new BinaryKey("key-123");

        Function<LowLevelCacheEntry, Object> mockTransform =
                mock(Function.class);


        CacheLocalTransform<?, ?, Object> testObj =
                new CacheLocalTransform<>(key, mockTransform);
        CacheImpl mockCache = mock(CacheImpl.class);
        when(mockCache.getName()).thenReturn("foo");
        testObj.setEnvironment(mockCache, null);

        testObj.call();

        verify(mockTransform).apply(eq(new CacheStoreEntry(mockCacheStore, "foo", key)));
    }
}
