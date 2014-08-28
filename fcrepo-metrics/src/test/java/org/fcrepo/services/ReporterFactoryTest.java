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

import java.io.IOException;
import java.util.Map;

import static com.codahale.metrics.MetricFilter.ALL;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import java.lang.management.ManagementFactory;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import  java.util.concurrent.TimeUnit;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.management.MBeanServer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
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
// PowerMock needs to ignore some packages to prevent class-cast errors
    @PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*", "org.xml.sax.*", "javax.management.*"})
    @PrepareForTest({ManagementFactory.class,JmxReporter.class})

/**
 * <p>ReporterFactoryTest class.</p>
 *
 * @author ghill
 */
public class ReporterFactoryTest {

    private JmxReporter testObj;

    @Mock
    private JmxReporter mockJmxReporter;

    @Mock
    private MetricRegistry mockRegistry;

    @Mock
    private MBeanServer mockMBeanServer;

    @Mock
    private MetricFilter mockFilter;

    @Mock
    private Logger mockLogger;

    @Before
    public void setUp() throws RepositoryException {
	initMocks(this);
	mockStatic(ManagementFactory.class);
	mockStatic(JmxReporter.class);
	mockJmxReporter = mock(JmxReporter.class);
        when(ManagementFactory.getPlatformMBeanServer()).thenReturn(mockMBeanServer);
        when (JmxReporter.forRegistry(mockRegistry).registerWith(mockMBeanServer).inDomain(anyString()).convertDurationsTo(MILLISECONDS).convertRatesTo(SECONDS).filter(mockFilter).build()).thenReturn(mockJmxReporter);
        when(LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);
	// set up a test
        testObj = JmxReporter.forRegistry(mockRegistry).registerWith(mockMBeanServer).inDomain("org.fcrepo").convertDurationsTo(MILLISECONDS).convertRatesTo(SECONDS).filter(mockFilter).build();
	testObj.start();
    }
    
    @Test
	public void testGetJmxReporter() {
	when(ManagementFactory.getPlatformMBeanServer()).thenReturn(mockMBeanServer);
	when (JmxReporter.forRegistry(mockRegistry).registerWith(mockMBeanServer).inDomain(anyString()).convertDurationsTo(any(TimeUnit.class)).convertRatesTo(any(TimeUnit.class)).filter(mockFilter).build()).thenReturn(mockJmxReporter);
	when(LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);
	ReporterFactory factory = new ReporterFactory();
        JmxReporter rep = factory.getJmxReporter("test");
	assertEquals(rep, testObj);
    }
}
