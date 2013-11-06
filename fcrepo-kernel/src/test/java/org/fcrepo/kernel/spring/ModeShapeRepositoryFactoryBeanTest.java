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

package org.fcrepo.kernel.spring;

import static org.fcrepo.kernel.utils.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import java.io.IOException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.api.Repository;
import org.springframework.core.io.Resource;

public class ModeShapeRepositoryFactoryBeanTest {

    private ModeShapeRepositoryFactoryBean testObj;

    @Mock
    private Resource mockConfig;

    @Mock
    private JcrRepository mockRepo;

    @Mock
    private JcrRepositoryFactory mockRepos;

    @Mock
    private JcrSession mockSession;

    @Before
    public void setUp() throws RepositoryException, IOException, NoSuchFieldException {
        initMocks(this);
        when(mockRepo.login()).thenReturn(mockSession);
        testObj = new ModeShapeRepositoryFactoryBean();
        testObj.setRepositoryConfiguration(mockConfig);
        when(mockRepos.getRepository(any(Map.class))).thenReturn(mockRepo);
        setField(testObj, "jcrRepositoryFactory", mockRepos);
    }

    @Test
    public void testFactory() throws RepositoryException, IOException {
        testObj.buildRepository();
        assertEquals(mockRepo, testObj.getObject());
    }

    @Test
    public void testFactoryMetadata() {
        assertEquals(Repository.class, testObj.getObjectType());
        assertEquals(true, testObj.isSingleton());
    }
}
