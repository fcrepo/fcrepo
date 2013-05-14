
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.services.functions.GetBinaryKey;
import org.infinispan.loaders.CacheLoaderException;
import org.modeshape.jcr.GetBinaryStore;
import org.fcrepo.services.functions.GetCacheStore;
import org.fcrepo.services.functions.GetGoodFixityResults;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.FixityResult.FixityState;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Function;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ServiceHelpers.class})
public class LowLevelStorageServiceTest {

    @Test
    public void testGetFixity() throws RepositoryException {
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        final Node mockNode = mock(Node.class);
        final Repository mockRepo = mock(Repository.class);
        final BinaryKey mockKey = mock(BinaryKey.class);
        final BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetBinaryStore(mockStoreFunc);
        testObj.setGetBinaryKey(mockKeyFunc);
        testObj.setRepository(mockRepo);
        final MessageDigest mockDigest = mock(MessageDigest.class);
        final URI mockUri = URI.create("urn:foo:bar"); // can't mock final classes
        final long testSize = 4L;
        final FixityResult mockFixity = mock(FixityResult.class);
        @SuppressWarnings("unchecked")
        final Function<LowLevelCacheEntry, FixityResult> mockFixityFunc =
                mock(Function.class);
        when(mockFixityFunc.apply(any(LowLevelCacheEntry.class))).thenReturn(
                mockFixity);
        PowerMockito.mockStatic(ServiceHelpers.class);
        when(
                ServiceHelpers.getCheckCacheFixityFunction(
                        any(MessageDigest.class), any(URI.class),
                        any(Long.class))).thenReturn(mockFixityFunc);
        final Collection<FixityResult> actual =
                testObj.getFixity(mockNode, mockDigest, mockUri, testSize);
        actual.iterator().next();
        verify(mockFixityFunc).apply(any(LowLevelCacheEntry.class));
    }

    @Test
    public void testTransformBinaryBlobs() throws RepositoryException {
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        final Node mockNode = mock(Node.class);
        final Repository mockRepo = mock(Repository.class);
        final BinaryKey mockKey = mock(BinaryKey.class);
        final BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
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

    @Test
    public void testGetBinaryBlobs() throws RepositoryException {
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        final Node mockNode = mock(Node.class);
        final Repository mockRepo = mock(Repository.class);
        final BinaryKey mockKey = mock(BinaryKey.class);
        final BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetBinaryStore(mockStoreFunc);
        testObj.setGetBinaryKey(mockKeyFunc);
        testObj.setRepository(mockRepo);
        final Set<LowLevelCacheEntry> actual = testObj.getLowLevelCacheEntries(mockNode);
        assertEquals("/foo", actual.iterator().next().getExternalIdentifier());
    }

    @Test
    public void testGetSession() throws LoginException, RepositoryException {
        final Repository mockRepo = mock(Repository.class);
        final Session mockSession = mock(Session.class);
        final Session mockAnotherSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession, mockAnotherSession);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setRepository(mockRepo);
        testObj.getSession();
        verify(mockRepo, times(2)).login();
    }

    @Test
    public void testLogoutSession() throws LoginException, RepositoryException {
        final Repository mockRepo = mock(Repository.class);
        final Session mockSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setRepository(mockRepo);
        testObj.logoutSession();
        verify(mockSession).logout();
        verify(mockRepo).login();
    }

    @Test
    public void testSetRepository() throws LoginException, RepositoryException {
        final Repository mockRepo = mock(Repository.class);
        final Repository mockAnotherRepo = mock(Repository.class);
        final Session mockSession = mock(Session.class);
        final Session mockAnotherSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        when(mockAnotherRepo.login()).thenReturn(mockAnotherSession);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setRepository(mockRepo);
        testObj.setRepository(mockAnotherRepo);
        verify(mockSession).logout();
        verify(mockAnotherRepo).login();
    }

	@Test
	public void shouldRetrieveLowLevelCacheEntryForDefaultBinaryStore() throws RepositoryException {
		final BinaryKey key = new BinaryKey("key-123");
		final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
		final Repository mockRepo = mock(Repository.class);
		final BinaryStore mockStore = mock(BinaryStore.class);
		when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);

		final LowLevelStorageService testObj = spy(new LowLevelStorageService());
		testObj.setRepository(mockRepo);
		testObj.setGetBinaryStore(mockStoreFunc);
		final Set<LowLevelCacheEntry> entries = testObj.getLowLevelCacheEntries(key);
		verify(testObj, times(1)).getLowLevelCacheEntriesFromStore(mockStore, key);
	}

	@Test
	public void shouldRetrieveLowLevelCacheStoresForBinaryKey() throws RepositoryException {

		final BinaryStore mockStore = mock(BinaryStore.class);

		final LowLevelStorageService testObj = new LowLevelStorageService();

		final Set<LowLevelCacheEntry> entries = testObj.getLowLevelCacheEntriesFromStore(mockStore, new BinaryKey("key-123"));

		assertEquals(1, entries.size());

		assertTrue("does not contain our entry", entries.contains(new LowLevelCacheEntry(mockStore, new BinaryKey("key-123"))));
	}


	@Test
	public void shouldRetrieveLowLevelCacheStoresForCompositeStore() throws RepositoryException, CacheLoaderException {

		final Cache ispnCache1 = mock(Cache.class);
		final Cache ispnCache2 = mock(Cache.class);
		final CacheStore ispnCacheStore1 = mock(CacheStore.class);
		final CacheStore ispnCacheStore2 = mock(CacheStore.class);
		final BinaryStore plainBinaryStore = mock(BinaryStore.class);
		final BinaryStore plainBinaryStore2 = mock(BinaryStore.class);


		final GetCacheStore mockCacheStoreFunc = mock(GetCacheStore.class);
		when(mockCacheStoreFunc.apply(ispnCache1)).thenReturn(ispnCacheStore1);
		when(mockCacheStoreFunc.apply(ispnCache2)).thenReturn(ispnCacheStore2);

		final CompositeBinaryStore mockStore = mock(CompositeBinaryStore.class);

		final HashMap<String, BinaryStore> map = new HashMap<String, BinaryStore>();
		final List<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>();
		caches.add(ispnCache1);
		caches.add(ispnCache2);

		map.put("default", plainBinaryStore);
		map.put("a", plainBinaryStore2);
		final InfinispanBinaryStore infinispanBinaryStore = mock(InfinispanBinaryStore.class);
		when(infinispanBinaryStore.getCaches()).thenReturn(caches);
		map.put("b", infinispanBinaryStore);
		when(mockStore.getNamedStoreIterator()).thenReturn(map.entrySet().iterator());

		final LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.setGetCacheStore(mockCacheStoreFunc);

		final BinaryKey key = new BinaryKey("key-123");
		when(plainBinaryStore.hasBinary(key)).thenReturn(true);
		when(plainBinaryStore2.hasBinary(key)).thenReturn(false);
		when(infinispanBinaryStore.hasBinary(key)).thenReturn(true);
		when(ispnCacheStore1.containsKey("key-123-data-0")).thenReturn(true);
		when(ispnCacheStore2.containsKey("key-123-data-0")).thenReturn(true);
		final Set<LowLevelCacheEntry> entries = testObj.getLowLevelCacheEntriesFromStore(mockStore, key);

		assertEquals(3, entries.size());

		assertTrue(entries.contains(new LowLevelCacheEntry(plainBinaryStore, key)));
		assertTrue(!entries.contains(new LowLevelCacheEntry(plainBinaryStore2, key)));
		assertTrue(entries.contains(new LowLevelCacheEntry(infinispanBinaryStore, ispnCacheStore1, key)));
		assertTrue(entries.contains(new LowLevelCacheEntry(infinispanBinaryStore, ispnCacheStore2, key)));


	}

	@Test
	public void shouldReturnAnEmptySetForMissingBinaryStore() throws RepositoryException {

		final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
		final Repository mockRepo = mock(Repository.class);
		when(mockStoreFunc.apply(mockRepo)).thenReturn(null);

		final LowLevelStorageService testObj = new LowLevelStorageService();
		testObj.setGetBinaryStore(mockStoreFunc);
		final Set<LowLevelCacheEntry> entries = testObj.getLowLevelCacheEntries(new BinaryKey("key-123"));

		assertEquals(0, entries.size());
	}

    @SuppressWarnings("unchecked")
    @Test
    public void testRunFixityAndFixProblems() throws RepositoryException,
																 IOException, CacheLoaderException {
        final GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        final GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        final Node mockNode = mock(Node.class);
        final Repository mockRepo = mock(Repository.class);
        final BinaryKey mockKey = new BinaryKey("key-123");
        final InfinispanBinaryStore mockStore =
                mock(InfinispanBinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        final Cache<?, ?> mockGoodCache = mock(Cache.class);
        final Cache<?, ?> mockBadCache = mock(Cache.class);
        final Cache<?, ?>[] mockCaches =
                new Cache[] {mockGoodCache, mockBadCache};
        final GetCacheStore mockCacheStoreFunc = mock(GetCacheStore.class);
        final CacheStore mockGoodCacheStore = mock(CacheStore.class);
		when(mockGoodCacheStore.containsKey("key-123-data-0")).thenReturn(true);
        final CacheStore mockBadCacheStore = mock(CacheStore.class);
		when(mockBadCacheStore.containsKey("key-123-data-0")).thenReturn(true);
        when(mockCacheStoreFunc.apply(mockGoodCache)).thenReturn(
                mockGoodCacheStore);
        when(mockCacheStoreFunc.apply(mockBadCache)).thenReturn(
                mockBadCacheStore);
        when(mockStore.getCaches()).thenReturn(Arrays.asList(mockCaches));
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        final LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setGetBinaryStore(mockStoreFunc);
        testObj.setGetBinaryKey(mockKeyFunc);
        testObj.setRepository(mockRepo);
        mock(MessageDigest.class);
        final URI mockUri = URI.create("urn:foo:bar"); // can't mock final classes
        final long testSize = 4L;
        final Function<LowLevelCacheEntry, FixityResult> mockFixityFunc =
                mock(Function.class);
        PowerMockito.mockStatic(ServiceHelpers.class);
        when(
                ServiceHelpers.getCheckCacheFixityFunction(
                        any(MessageDigest.class), any(URI.class),
                        any(Long.class))).thenReturn(mockFixityFunc);
        final GetGoodFixityResults goodMock = mock(GetGoodFixityResults.class);
        testObj.setGetGoodFixityResults(goodMock);
        testObj.setGetCacheStore(mockCacheStoreFunc);
        final Datastream mockDs = mock(Datastream.class);
        final FedoraObject mockObj = mock(FedoraObject.class);
        when(mockObj.getName()).thenReturn("mockObject");
        when(mockDs.getObject()).thenReturn(mockObj);
        when(mockDs.getDsId()).thenReturn("mockDs");
        when(mockDs.getNode()).thenReturn(mockNode);
        when(mockDs.getContentSize()).thenReturn(testSize);
        when(mockDs.getContentDigestType()).thenReturn("MD5"); // whatever, just be quiet
        when(mockDs.getContentDigest()).thenReturn(mockUri);

        final FixityResult mockGoodFixity = mock(FixityResult.class);
        final FixityResult mockBadFixity = mock(FixityResult.class);
        when(mockFixityFunc.apply(any(LowLevelCacheEntry.class))).thenReturn(
                mockGoodFixity, mockBadFixity);
        final FixityResult[] results = new FixityResult[] {mockGoodFixity};
        when(goodMock.apply(any(Collection.class))).thenReturn(
                new HashSet<FixityResult>(Arrays.asList(results)));
        final LowLevelCacheEntry goodEntry = mock(LowLevelCacheEntry.class);
        final LowLevelCacheEntry badEntry = mock(LowLevelCacheEntry.class);
        when(mockGoodFixity.getEntry()).thenReturn(goodEntry);
        when(mockBadFixity.getEntry()).thenReturn(badEntry);
        mockBadFixity.status = EnumSet.noneOf(FixityState.class);
        final InputStream mockIS = mock(InputStream.class);
        when(goodEntry.getInputStream()).thenReturn(mockIS);
        when(
                badEntry.checkFixity(any(URI.class), any(Long.class),
                        any(MessageDigest.class))).thenReturn(mockBadFixity);
        final Collection<FixityResult> actual =
                testObj.runFixityAndFixProblems(mockDs);
        actual.iterator().next();
        verify(mockFixityFunc, times(2)).apply(any(LowLevelCacheEntry.class));
        verify(goodMock).apply(any(Collection.class));
        verify(badEntry).storeValue(mockIS);
    }

}
