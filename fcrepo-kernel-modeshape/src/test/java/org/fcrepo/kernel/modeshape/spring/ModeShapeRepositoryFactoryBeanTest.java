/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.spring;

import static org.fcrepo.kernel.modeshape.utils.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;

/**
 * <p>ModeShapeRepositoryFactoryBeanTest class.</p>
 *
 * @author awoods
 */
public class ModeShapeRepositoryFactoryBeanTest {

    private ModeShapeRepositoryFactoryBean testObj;

    private Resource config = new ClassPathResource(
            "config/testing/repository.json");

    @Mock
    private JcrRepository mockRepo;

    @Mock
    private ModeShapeEngine mockModeShapeEngine;

    @Mock
    private JcrSession mockSession;

    @Mock
    private org.modeshape.common.collection.Problems mockProblems;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mockRepo.getStartupProblems()).thenReturn(mockProblems);
        when(mockProblems.iterator()).thenReturn(new ArrayList<org.modeshape.common.collection.Problem>().iterator());
        when(mockRepo.login()).thenReturn(mockSession);
        testObj = new ModeShapeRepositoryFactoryBean();
        testObj.setRepositoryConfiguration(config);
        when(mockModeShapeEngine.deploy(any(RepositoryConfiguration.class)))
                .thenReturn(mockRepo);
        setField(testObj, "modeShapeEngine", mockModeShapeEngine);
    }

    @Test
    public void testFactory() throws Exception {
        testObj.buildRepository();
        assertEquals(mockRepo, testObj.getObject());
    }

    @Test
    public void testFactoryMetadata() {
        assertEquals(JcrRepository.class, testObj.getObjectType());
        assertEquals(true, testObj.isSingleton());
    }
}
