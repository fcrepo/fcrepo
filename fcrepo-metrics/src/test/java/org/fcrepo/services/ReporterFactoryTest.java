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
package org.fcrepo.services;

import java.lang.management.ManagementFactory;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricFilter;

import javax.jcr.RepositoryException;
import javax.management.MBeanServer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.junit.runner.RunWith;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.fcrepo.metrics.ReporterFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*", "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({ManagementFactory.class})

/**
 * <p>ReporterFactoryTest class.</p>
 *
 * @author ghill
 */
public class ReporterFactoryTest {

    @Mock
    private MetricRegistry mockRegistry;

    @Mock
    private MBeanServer mockMBeanServer;

    @Mock
    private MetricFilter mockFilter;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
    }

    @Test
    public void testGetJmxReporterAW() {
        mockStatic(ManagementFactory.class);
        final ManagementFactory mockManagementFactory = mock(ManagementFactory.class);
        when(mockManagementFactory.getPlatformMBeanServer()).thenReturn(mockMBeanServer);

        final ReporterFactory factory = new ReporterFactory();
        final JmxReporter reporter = factory.getJmxReporter("not-used");
        Assert.assertNotNull(reporter);

    }
}
