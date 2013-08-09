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
package org.fcrepo.integration.kernel.utils.infinispan;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.utils.infinispan.StoreChunkInputStream;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Test;


public class StoreChunkInputStreamIT {

    @Test
    public void tryRetrievingContentFromInfinispanIT() throws IOException {
        final EmbeddedCacheManager cm =
                new DefaultCacheManager("config/infinispan/basic/infinispan.xml");

        final Cache<String, byte[]> ispn = cm.getCache("FedoraRepository");

        final CacheStore cs =
                ispn.getAdvancedCache().getComponentRegistry().getComponent(
                        CacheLoaderManager.class).getCacheStore();

        ispn.put("key-data-0", "0".getBytes());
        ispn.put("key-data-1", "1".getBytes());
        ispn.put("key-data-2", "2".getBytes());
        ispn.put("key-data-3", "3".getBytes());
        ispn.put("key-data-4", "4".getBytes());
        ispn.put("key-data-5", "5".getBytes());

        final InputStream is = new StoreChunkInputStream(cs, "key-data");

        final String data = IOUtils.toString(is);

        assertEquals("012345", data);
    }
}
