/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.fcrepo.auth.common.DelegateHeaderPrincipalProvider.DELEGATE_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author awoods
 * @since 10/31/15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DelegateHeaderPrincipalProviderTest {

    private final DelegateHeaderPrincipalProvider provider = new DelegateHeaderPrincipalProvider();

    @Mock
    private HttpServletRequest request;

    @Test
    public void testGetDelegate0() {
        when(request.getHeader(DELEGATE_HEADER)).thenReturn(null);
        assertNull(provider.getDelegate(request), "No delegates should return null");
    }

    @Test
    public void testGetDelegate1() {
        final String user = "user1";
        when(request.getHeader(DELEGATE_HEADER)).thenReturn(user);
        assertNotNull(provider.getDelegate(request), "Should be a delegate!");
        assertEquals(user, provider.getDelegate(request).getName());
    }
}