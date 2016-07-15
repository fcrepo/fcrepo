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
package org.fcrepo.services;

import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.initMocks;


import javax.management.MBeanServer;

import org.fcrepo.metrics.ReporterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * ReporterFactoryTest class.
 *
 * @author ghill
 */
public class ReporterFactoryTest {

    private ReporterFactory factory;

    @Mock
    private MBeanServer mockMBeanServer;

    @Mock
    private Graphite mockGraphite;

    @Before
    public void setUp() {
        initMocks(this);
        factory = new ReporterFactory();
    }

    @Test
    public void testGetJmxReporter() {
        try (final JmxReporter reporter = factory.getJmxReporter("not-used")) {
            assertNotNull(reporter);
        }
    }

    @Test
    public void testGetGraphiteReporter() {
        try (final GraphiteReporter reporter = factory.getGraphiteReporter("some-prefix", mockGraphite)) {
            assertNotNull(reporter);
        }
    }

}
