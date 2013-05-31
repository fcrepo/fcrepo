/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.services.functions.GetCacheStore;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.GetBinaryStore;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @autho Chris Beerr
 * @date Mar 11, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ServiceHelpers.class})
public class LowLevelStorageServiceTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testTransformBinaryBlobs() throws RepositoryException {
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        final Node mockNode = mock(Node.class);
        final Repository mockRepo = mock(Repository.class);
        final BinaryKey mockKey = mock(BinaryKey.class);
        final BinaryStore mockStore = mock(BinaryStore.class);

        final Property mockProperty = mock(Property.class);
        when(mockNode.getProperty(JcrConstants.JCR_DATA)).thenReturn(mockProperty);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockProperty)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetBinaryStore(mockStoreFunc);
        testObj.setGetBinaryKey(mockKeyFunc);
        testObj.setRepository(mockRepo);
        @SuppressWarnings("unchecked")
        final Function<LowLevelCacheEntry, String> testFunc =
                mock(Function.class);
        when(testFunc.apply(any(LowLevelCacheEntry.class))).thenReturn("bar");
        final Collection<String> actual =
                testObj.transformLowLevelCacheEntries(mockNode, testFunc);
        assertEquals("bar", actual.iterator().next());
        verify(testFunc).apply(any(LowLevelCacheEntry.class));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetBinaryBlobs() throws RepositoryException {
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        final Node mockNode = mock(Node.class);
        final Property mockProperty = mock(Property.class);
        when(mockNode.getProperty(JcrConstants.JCR_DATA)).thenReturn(mockProperty);
        final Repository mockRepo = mock(Repository.class);
        final BinaryKey mockKey = mock(BinaryKey.class);
        final BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockProperty)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetBinaryStore(mockStoreFunc);
        testObj.setGetBinaryKey(mockKeyFunc);
        testObj.setRepository(mockRepo);
        final Set<LowLevelCacheEntry> actual =
                testObj.getLowLevelCacheEntries(mockNode);
        assertEquals("/foo", actual.iterator().next().getExternalIdentifier());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRetrieveLowLevelCacheEntryForDefaultBinaryStore()
            throws RepositoryException {
        final BinaryKey key = new BinaryKey("key-123");
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final Repository mockRepo = mock(Repository.class);
        final BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);

        final LowLevelStorageService testObj =
                spy(new LowLevelStorageService());
        testObj.setRepository(mockRepo);
        testObj.setGetBinaryStore(mockStoreFunc);
        testObj.getLowLevelCacheEntries(key);
        verify(testObj, times(1)).getLowLevelCacheEntriesFromStore(mockStore,
                key);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRetrieveLowLevelCacheStoresForBinaryKey()
            throws RepositoryException {

        final BinaryStore mockStore = mock(BinaryStore.class);

        final LowLevelStorageService testObj = new LowLevelStorageService();

        final Set<LowLevelCacheEntry> entries =
                testObj.getLowLevelCacheEntriesFromStore(mockStore,
                        new BinaryKey("key-123"));

        assertEquals(1, entries.size());

        assertTrue("does not contain our entry", entries
                .contains(new LowLevelCacheEntry(mockStore, new BinaryKey(
                        "key-123"))));
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldRetrieveLowLevelCacheStoresForCompositeStore()
            throws RepositoryException, CacheLoaderException {

        final Cache<?, ?> ispnCache1 = mock(Cache.class);
        final Cache<?, ?> ispnCache2 = mock(Cache.class);
        final CacheStore ispnCacheStore1 = mock(CacheStore.class);
        final CacheStore ispnCacheStore2 = mock(CacheStore.class);
        final BinaryStore plainBinaryStore = mock(BinaryStore.class);
        final BinaryStore plainBinaryStore2 = mock(BinaryStore.class);

        final GetCacheStore mockCacheStoreFunc = mock(GetCacheStore.class);
        when(mockCacheStoreFunc.apply(ispnCache1)).thenReturn(ispnCacheStore1);
        when(mockCacheStoreFunc.apply(ispnCache2)).thenReturn(ispnCacheStore2);

        final CompositeBinaryStore mockStore = mock(CompositeBinaryStore.class);

        final HashMap<String, BinaryStore> map =
                new HashMap<String, BinaryStore>();
        final List<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>();
        caches.add(ispnCache1);
        caches.add(ispnCache2);

        map.put("default", plainBinaryStore);
        map.put("a", plainBinaryStore2);
        final InfinispanBinaryStore infinispanBinaryStore =
                mock(InfinispanBinaryStore.class);
        when(infinispanBinaryStore.getCaches()).thenReturn(caches);
        map.put("b", infinispanBinaryStore);
        when(mockStore.getNamedStoreIterator()).thenReturn(
                map.entrySet().iterator());

        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetCacheStore(mockCacheStoreFunc);

        final BinaryKey key = new BinaryKey("key-123");
        when(plainBinaryStore.hasBinary(key)).thenReturn(true);
        when(plainBinaryStore2.hasBinary(key)).thenReturn(false);
        when(infinispanBinaryStore.hasBinary(key)).thenReturn(true);
        when(ispnCacheStore1.containsKey("key-123-data-0")).thenReturn(true);
        when(ispnCacheStore2.containsKey("key-123-data-0")).thenReturn(true);
        final Set<LowLevelCacheEntry> entries =
                testObj.getLowLevelCacheEntriesFromStore(mockStore, key);

        assertEquals(3, entries.size());

        assertTrue(entries.contains(new LowLevelCacheEntry(plainBinaryStore,
                key)));
        assertTrue(!entries.contains(new LowLevelCacheEntry(plainBinaryStore2,
                key)));
        assertTrue(entries.contains(new LowLevelCacheEntry(
                infinispanBinaryStore, ispnCacheStore1, key)));
        assertTrue(entries.contains(new LowLevelCacheEntry(
                infinispanBinaryStore, ispnCacheStore2, key)));

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void shouldReturnAnEmptySetForMissingBinaryStore()
            throws RepositoryException {

        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final Repository mockRepo = mock(Repository.class);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(null);

        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetBinaryStore(mockStoreFunc);
        final Set<LowLevelCacheEntry> entries =
                testObj.getLowLevelCacheEntries(new BinaryKey("key-123"));

        assertEquals(0, entries.size());
    }

}
