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
package org.fcrepo.auth.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ServletCredentials;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Set;

/**
 * @author daines
 * @author bbpennel
 * @since Mar 6, 2014
 */
public class HttpHeaderPrincipalProviderTest {

    @Mock
    private ServletCredentials credentials;

    @Mock
    private HttpServletRequest request;

    private HttpHeaderPrincipalProvider provider;

    @Before
    public void setUp() {
        initMocks(this);
        when(credentials.getRequest()).thenReturn(request);

        provider = new HttpHeaderPrincipalProvider();
    }

    @Test
    public void testPrincipalsExtractedFromHeaders() {

        when(request.getHeader("Groups")).thenReturn("a,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(credentials);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals
                .contains(new HttpHeaderPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals
                .contains(new HttpHeaderPrincipal("b")));

    }

    @Test
    public void testShouldTrimPrincipalNames() {

        when(request.getHeader("Groups")).thenReturn(" a ,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(credentials);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals
                .contains(new HttpHeaderPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals
                .contains(new HttpHeaderPrincipal("b")));

    }

    @Test
    public void testNoHeaderName() {

        final Set<Principal> principals = provider.getPrincipals(credentials);

        assertTrue("Empty set expected when no header name configured", principals.isEmpty());

    }

    @Test
    public void testNoSeparator() {

        provider.setHeaderName("Groups");

        final Set<Principal> principals = provider.getPrincipals(credentials);

        assertTrue("Empty set expected when no separator name configured", principals.isEmpty());

    }

    @Test
    public void testInvalidCredentials() {

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(mock(Credentials.class));

        assertTrue("Empty set expected when incorrect type of credentials supplied", principals.isEmpty());

    }

    @Test
    public void testNoRequest() {

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        when(credentials.getRequest()).thenReturn(null);

        final Set<Principal> principals = provider.getPrincipals(credentials);

        assertTrue("Empty set expected when no request supplied", principals.isEmpty());

    }

    @Test
    public void testPrincipalEqualsDifferentClass() {

        when(request.getHeader("Groups")).thenReturn("a");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(credentials);

        final Principal principal = principals.iterator().next();

        assertNotEquals("Principals should not be equal if not the same class",
                principal, mock(Principal.class));

    }

}
