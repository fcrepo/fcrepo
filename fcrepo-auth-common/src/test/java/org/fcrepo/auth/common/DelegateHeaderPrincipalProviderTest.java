/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.auth.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.fcrepo.auth.common.DelegateHeaderPrincipalProvider.DELEGATE_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author awoods
 * @since 10/31/15
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegateHeaderPrincipalProviderTest {

    private final DelegateHeaderPrincipalProvider provider = new DelegateHeaderPrincipalProvider();

    @Mock
    private HttpServletRequest request;

    @Test
    public void testGetDelegate0() {
        when(request.getHeader(DELEGATE_HEADER)).thenReturn(null);
        assertNull("No delegates should return null", provider.getDelegate(request));
    }

    @Test
    public void testGetDelegate1() {
        final String user = "user1";
        when(request.getHeader(DELEGATE_HEADER)).thenReturn(user);
        assertNotNull("Should be a delegate!", provider.getDelegate(request));
        assertEquals(user, provider.getDelegate(request).getName());
    }
}