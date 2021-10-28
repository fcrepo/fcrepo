/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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