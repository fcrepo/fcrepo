
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.services.functions.GetBinaryKey;
import org.fcrepo.services.functions.GetBinaryStore;
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
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Function;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceHelpers.class})
public class LowLevelStorageServiceTest {

    @Test
    public void testGetFixity() throws RepositoryException {
        GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        Node mockNode = mock(Node.class);
        Repository mockRepo = mock(Repository.class);
        BinaryKey mockKey = mock(BinaryKey.class);
        BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.getBinaryStore = mockStoreFunc;
        testObj.getBinaryKey = mockKeyFunc;
        testObj.setRepository(mockRepo);
        MessageDigest mockDigest = mock(MessageDigest.class);
        URI mockUri = URI.create("urn:foo:bar"); // can't mock final classes
        long testSize = 4L;
        FixityResult mockFixity = mock(FixityResult.class);
        @SuppressWarnings("unchecked")
        Function<LowLevelCacheEntry, FixityResult> mockFixityFunc =
                mock(Function.class);
        when(mockFixityFunc.apply(any(LowLevelCacheEntry.class))).thenReturn(
                mockFixity);
        PowerMockito.mockStatic(ServiceHelpers.class);
        when(
                ServiceHelpers.getCheckCacheFixityFunction(
                        any(MessageDigest.class), any(URI.class),
                        any(Long.class))).thenReturn(mockFixityFunc);
        Collection<FixityResult> actual =
                testObj.getFixity(mockNode, mockDigest, mockUri, testSize);
        actual.iterator().next();
        verify(mockFixityFunc).apply(any(LowLevelCacheEntry.class));
    }

    @Test
    public void testTransformBinaryBlobs() throws RepositoryException {
        GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        Node mockNode = mock(Node.class);
        Repository mockRepo = mock(Repository.class);
        BinaryKey mockKey = mock(BinaryKey.class);
        BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.getBinaryStore = mockStoreFunc;
        testObj.getBinaryKey = mockKeyFunc;
        testObj.setRepository(mockRepo);
        @SuppressWarnings("unchecked")
        Function<LowLevelCacheEntry, String> testFunc = mock(Function.class);
        when(testFunc.apply(any(LowLevelCacheEntry.class))).thenReturn("bar");
        Collection<String> actual =
                testObj.transformBinaryBlobs(mockNode, testFunc);
        assertEquals("bar", actual.iterator().next());
        verify(testFunc).apply(any(LowLevelCacheEntry.class));
    }

    @Test
    public void testGetBinaryBlobs() throws RepositoryException {
        GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        Node mockNode = mock(Node.class);
        Repository mockRepo = mock(Repository.class);
        BinaryKey mockKey = mock(BinaryKey.class);
        BinaryStore mockStore = mock(BinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.getBinaryStore = mockStoreFunc;
        testObj.getBinaryKey = mockKeyFunc;
        testObj.setRepository(mockRepo);
        Set<LowLevelCacheEntry> actual = testObj.getBinaryBlobs(mockNode);
        assertEquals("foo", actual.iterator().next().getExternalIdentifier());
    }

    @Test
    public void testGetSession() throws LoginException, RepositoryException {
        Repository mockRepo = mock(Repository.class);
        Session mockSession = mock(Session.class);
        Session mockAnotherSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession, mockAnotherSession);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setRepository(mockRepo);
        testObj.getSession();
        verify(mockRepo, times(2)).login();
    }

    @Test
    public void testLogoutSession() throws LoginException, RepositoryException {
        Repository mockRepo = mock(Repository.class);
        Session mockSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setRepository(mockRepo);
        testObj.logoutSession();
        verify(mockSession).logout();
        verify(mockRepo).login();
    }

    @Test
    public void testSetRepository() throws LoginException, RepositoryException {
        Repository mockRepo = mock(Repository.class);
        Repository mockAnotherRepo = mock(Repository.class);
        Session mockSession = mock(Session.class);
        Session mockAnotherSession = mock(Session.class);
        when(mockRepo.login()).thenReturn(mockSession);
        when(mockAnotherRepo.login()).thenReturn(mockAnotherSession);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.setRepository(mockRepo);
        testObj.setRepository(mockAnotherRepo);
        verify(mockSession).logout();
        verify(mockAnotherRepo).login();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunFixityAndFixProblems() throws RepositoryException,
            IOException {
        GetBinaryStore mockStoreFunc = mock(GetBinaryStore.class);
        GetBinaryKey mockKeyFunc = mock(GetBinaryKey.class);
        Node mockNode = mock(Node.class);
        Repository mockRepo = mock(Repository.class);
        BinaryKey mockKey = mock(BinaryKey.class);
        InfinispanBinaryStore mockStore = mock(InfinispanBinaryStore.class);
        when(mockStore.toString()).thenReturn("foo");
        Cache<?, ?> mockGoodCache = mock(Cache.class);
        Cache<?, ?> mockBadCache = mock(Cache.class);
        Cache<?, ?>[] mockCaches = new Cache[] {mockGoodCache, mockBadCache};
        GetCacheStore mockCacheStoreFunc = mock(GetCacheStore.class);
        CacheStore mockGoodCacheStore = mock(CacheStore.class);
        CacheStore mockBadCacheStore = mock(CacheStore.class);
        when(mockCacheStoreFunc.apply(mockGoodCache)).thenReturn(
                mockGoodCacheStore);
        when(mockCacheStoreFunc.apply(mockBadCache)).thenReturn(
                mockBadCacheStore);
        when(mockStore.getCaches()).thenReturn(Arrays.asList(mockCaches));
        when(mockKeyFunc.apply(mockNode)).thenReturn(mockKey);
        when(mockStoreFunc.apply(mockRepo)).thenReturn(mockStore);
        LowLevelStorageService testObj = new LowLevelStorageService();
        testObj.getBinaryStore = mockStoreFunc;
        testObj.getBinaryKey = mockKeyFunc;
        testObj.setRepository(mockRepo);
        mock(MessageDigest.class);
        URI mockUri = URI.create("urn:foo:bar"); // can't mock final classes
        long testSize = 4L;
        Function<LowLevelCacheEntry, FixityResult> mockFixityFunc =
                mock(Function.class);
        PowerMockito.mockStatic(ServiceHelpers.class);
        when(
                ServiceHelpers.getCheckCacheFixityFunction(
                        any(MessageDigest.class), any(URI.class),
                        any(Long.class))).thenReturn(mockFixityFunc);
        GetGoodFixityResults goodMock = mock(GetGoodFixityResults.class);
        testObj.getGoodFixityResults = goodMock;
        testObj.getCacheStore = mockCacheStoreFunc;
        Datastream mockDs = mock(Datastream.class);
        FedoraObject mockObj = mock(FedoraObject.class);
        when(mockObj.getName()).thenReturn("mockObject");
        when(mockDs.getObject()).thenReturn(mockObj);
        when(mockDs.getDsId()).thenReturn("mockDs");
        when(mockDs.getNode()).thenReturn(mockNode);
        when(mockDs.getContentSize()).thenReturn(testSize);
        when(mockDs.getContentDigestType()).thenReturn("MD5"); // whatever, just be quiet
        when(mockDs.getContentDigest()).thenReturn(mockUri);

        FixityResult mockGoodFixity = mock(FixityResult.class);
        FixityResult mockBadFixity = mock(FixityResult.class);
        when(mockFixityFunc.apply(any(LowLevelCacheEntry.class))).thenReturn(
                mockGoodFixity, mockBadFixity);
        FixityResult[] results = new FixityResult[] {mockGoodFixity};
        when(goodMock.apply(any(Collection.class))).thenReturn(
                new HashSet<FixityResult>(Arrays.asList(results)));
        LowLevelCacheEntry goodEntry = mock(LowLevelCacheEntry.class);
        LowLevelCacheEntry badEntry = mock(LowLevelCacheEntry.class);
        when(mockGoodFixity.getEntry()).thenReturn(goodEntry);
        when(mockBadFixity.getEntry()).thenReturn(badEntry);
        mockBadFixity.status = EnumSet.noneOf(FixityState.class);
        InputStream mockIS = mock(InputStream.class);
        when(goodEntry.getInputStream()).thenReturn(mockIS);
        when(
                badEntry.checkFixity(any(URI.class), any(Long.class),
                        any(MessageDigest.class))).thenReturn(mockBadFixity);
        Collection<FixityResult> actual =
                testObj.runFixityAndFixProblems(mockDs);
        actual.iterator().next();
        verify(mockFixityFunc, times(2)).apply(any(LowLevelCacheEntry.class));
        verify(goodMock).apply(any(Collection.class));
        verify(badEntry).storeValue(mockIS);
    }

}
