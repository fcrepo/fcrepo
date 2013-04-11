
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
        JcrRepository mockRepo = mock(JcrRepository.class);
        JcrSession mockSession = mock(JcrSession.class);
        RepositoryConfiguration mockConfig =
                mock(RepositoryConfiguration.class);
        BinaryStorage mockBinStorage = mock(BinaryStorage.class);
        when(mockConfig.getBinaryStorage()).thenReturn(mockBinStorage);
        when(mockRepo.getConfiguration()).thenReturn(mockConfig);
        when(mockSession.getRepository()).thenReturn(mockRepo);
        when(mockRepo.login()).thenReturn(mockSession);
        GetBinaryStore testObj = new GetBinaryStore();
        testObj.apply(mockRepo);
    }

}
