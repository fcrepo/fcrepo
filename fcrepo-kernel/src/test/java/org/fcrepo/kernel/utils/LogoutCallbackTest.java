/**
 * Copyright 2014 DuraSpace, Inc.
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
