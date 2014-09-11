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

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.fcrepo.metrics.ReporterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * ReporterFactoryTest class.
 *
 * @author ghill
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*", "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({ManagementFactory.class})
public class ReporterFactoryTest {

    private ReporterFactory factory;

    @Mock
    private MBeanServer mockMBeanServer;

    @Mock
    private Graphite mockGraphite;

    @Before
    public void setUp() {
        initMocks(this);
        mockStatic(ManagementFactory.class);
        when(getPlatformMBeanServer()).thenReturn(mockMBeanServer);
        factory = new ReporterFactory();
    }

    @Test
    public void testGetJmxReporter() {
        final JmxReporter reporter = factory.getJmxReporter("not-used");
        assertNotNull(reporter);
    }

    @Test
    public void testGetGraphiteReporter() {
        final GraphiteReporter reporter = factory.getGraphiteReporter("some-prefix", mockGraphite);
        assertNotNull(reporter);
    }

}
