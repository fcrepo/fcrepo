/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Providers;

import org.fcrepo.kernel.api.exception.RepositoryConfigurationException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author cabeer
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RepositoryRuntimeExceptionMapperTest {

    @Mock
    private Providers mockProviders;

    private RepositoryRuntimeExceptionMapper testObj;

    @Mock
    private ExceptionMapper<RepositoryConfigurationException> mockProvider;

    @BeforeEach
    public void setUp() {
        testObj = new RepositoryRuntimeExceptionMapper(mockProviders);
    }

    @Test
    public void testToResponseWithHandledRepositoryException() {
        when(mockProviders.getExceptionMapper(RepositoryConfigurationException.class)).thenReturn(mockProvider);
        final RepositoryConfigurationException cause = new RepositoryConfigurationException("xyz");
        final RepositoryRuntimeException ex = new RepositoryRuntimeException(cause.getMessage(), cause);
        testObj.toResponse(ex);
        verify(mockProvider).toResponse(cause);
    }

    @Test
    public void testToResponseWithUnhandledRepositoryException() {
        when(mockProviders.getExceptionMapper(Exception.class)).thenReturn(null);
        final Exception cause = new Exception("xyz");
        final RepositoryRuntimeException ex = new RepositoryRuntimeException(cause.getMessage(), cause);
        final Response response = testObj.toResponse(ex);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testToResponseWithNoWrappedException() {
        when(mockProviders.getExceptionMapper(Exception.class)).thenReturn(null);
        final RepositoryRuntimeException ex = new RepositoryRuntimeException("!");
        final Response response = testObj.toResponse(ex);
        assertEquals(500, response.getStatus());
    }
}
