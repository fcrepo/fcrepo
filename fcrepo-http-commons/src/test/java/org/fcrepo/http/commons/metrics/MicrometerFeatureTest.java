/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.metrics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.glassfish.jersey.micrometer.server.MetricsApplicationEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.FeatureContext;

/**
 * Test class for {@link MicrometerFeature}
 *
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class MicrometerFeatureTest {

    private MicrometerFeature feature;

    @Mock
    private ServletContext servletContext;

    @Mock
    private FeatureContext featureContext;

    @Mock
    private WebApplicationContext appContext;

    @Mock
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        feature = new MicrometerFeature();
        setField(feature, "servletContext", servletContext);
    }

    @Test
    void testConfigureWithNullServletContext() {
        // Simulate returning null for servlet context
        setField(feature, "servletContext", null);

        final boolean result = feature.configure(featureContext);
        assertFalse(result);
        verifyNoInteractions(featureContext);
    }

    @Test
    void testConfigureWithNoWebAppContext() {
        // Simulate returning null for webapp context
        try (final var mockedContext = mockStatic(WebApplicationContextUtils.class)) {
            mockedContext.when(() ->
                    WebApplicationContextUtils.getWebApplicationContext(servletContext)
            ).thenReturn(null);
            final boolean result = feature.configure(featureContext);
            assertFalse(result);
            verifyNoInteractions(featureContext);
        }
    }

    @Test
    void testConfigureWithMissingMeterRegistryBean() {
        // Simulate returning a valid webapp context but missing MeterRegistry bean
        try (final var mockedContext = mockStatic(WebApplicationContextUtils.class)) {
            mockedContext.when(() ->
                    WebApplicationContextUtils.getWebApplicationContext(servletContext)
            ).thenReturn(appContext);

            when(appContext.getBean(MeterRegistry.class)).thenThrow(new RuntimeException("Bean not found"));

            final boolean result = feature.configure(featureContext);
            assertFalse(result);
            verifyNoInteractions(featureContext);
        }
    }

    @Test
    void testConfigureSuccess() {
        // Simulate returning a valid webapp context with MeterRegistry bean
        try (final var mockedContext = mockStatic(WebApplicationContextUtils.class)) {
            mockedContext.when(() ->
                    WebApplicationContextUtils.getWebApplicationContext(servletContext)
            ).thenReturn(appContext);

            when(appContext.getBean(MeterRegistry.class)).thenReturn(meterRegistry);

            final ArgumentCaptor<MetricsApplicationEventListener> captor =
                    ArgumentCaptor.forClass(MetricsApplicationEventListener.class);

            final boolean result = feature.configure(featureContext);

            assertTrue(result);
            verify(featureContext).register(captor.capture());

            final MetricsApplicationEventListener listener = captor.getValue();
            assertNotNull(listener);
        }
    }
}
