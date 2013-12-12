/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel.services.functions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;

import javax.jcr.Repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GetDefaultWorkspaceTest {

    private GetDefaultWorkspace testObj;

    @Mock
    private JcrRepository mockModeshapeRepository;

    @Mock
    private Repository mockRepository;

    @Mock
    private RepositoryConfiguration mockRepositoryConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mockModeshapeRepository.getConfiguration()).thenReturn(mockRepositoryConfig);
        testObj = new GetDefaultWorkspace();
    }

    @Test
    public void testApply() {
        when(mockRepositoryConfig.getDefaultWorkspaceName()).thenReturn("mock-default-workspace");
        assertEquals("mock-default-workspace", testObj.apply(mockModeshapeRepository));
    }

    @Test
    public void testApplyWithNonModeshapreRepository() {
        assertEquals("default", testObj.apply(mockRepository));
    }
}
