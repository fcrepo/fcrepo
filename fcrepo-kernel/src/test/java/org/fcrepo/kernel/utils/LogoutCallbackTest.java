package org.fcrepo.kernel.utils;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


public class LogoutCallbackTest {

    private LogoutCallback testLogoutCallback;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testLogoutCallback = new LogoutCallback(mockSession);
    }

    @Test
    public void testonSuccess() {
        testLogoutCallback.onSuccess(null);
        verify(mockSession).logout();
    }

    @Test
    public void testonFailure() {
        try {
            testLogoutCallback.onFailure(new Exception("Expected."));
            fail("Should have propagated exception!");
        } catch (final RuntimeException e) {
            // expected.
        }
        verify(mockSession).logout();
    }



}
