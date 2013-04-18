
package org.fcrepo.services.functions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.BinaryStorage;

public class GetBinaryStoreTest {

    @Test
    public void testApply() throws LoginException, RepositoryException {
        final JcrRepository mockRepo = mock(JcrRepository.class);
        final JcrSession mockSession = mock(JcrSession.class);
        final RepositoryConfiguration mockConfig =
                mock(RepositoryConfiguration.class);
        final BinaryStorage mockBinStorage = mock(BinaryStorage.class);
        when(mockConfig.getBinaryStorage()).thenReturn(mockBinStorage);
        when(mockRepo.getConfiguration()).thenReturn(mockConfig);
        when(mockSession.getRepository()).thenReturn(mockRepo);
        when(mockRepo.login()).thenReturn(mockSession);
        final GetBinaryStore testObj = new GetBinaryStore();
        testObj.apply(mockRepo);
    }

}
