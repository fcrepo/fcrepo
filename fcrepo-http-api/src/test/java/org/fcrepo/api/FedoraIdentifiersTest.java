
package org.fcrepo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.jaxb.responses.management.NextPid;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Function;

public class FedoraIdentifiersTest {

    @Mock
    private PidMinter mockPidMinter;

    @InjectMocks
    private FedoraIdentifiers fi = new FedoraIdentifiers();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetNextPid() {
        when(mockPidMinter.makePid()).thenReturn(
                new Function<Object, String>() {

                    @Override
                    public String apply(Object input) {
                        return "asdf:123";
                    }
                });

        NextPid np = fi.getNextPid(2);

        assertNotNull(np);

        for (final String pid : np.pids) {
            assertEquals("Wrong pid value!", "asdf:123", pid);
        }

    }
}
