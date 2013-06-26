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
package org.fcrepo.spring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.fcrepo.services.ObjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.api.Repository;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.Resource;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 14, 2013
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ObjectService.class})
public class ModeShapeRepositoryFactoryBeanTest {

    private ModeShapeRepositoryFactoryBean testObj;

    private Resource mockConfig;

    private JcrRepository mockRepo;

    private JcrRepositoryFactory mockRepos;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setUp() throws RepositoryException, IOException {
        mockConfig = mock(Resource.class);
        mockRepos = mock(JcrRepositoryFactory.class);
        mockRepo = mock(JcrRepository.class);
        JcrSession mockSession = mock(JcrSession.class);
        when(mockRepo.login())
        .thenReturn(mockSession);
        testObj = new ModeShapeRepositoryFactoryBean();
        testObj.setRepositoryConfiguration(mockConfig);
        when(mockRepos.getRepository(any(Map.class)))
        .thenReturn(mockRepo);
        inject("jcrRepositoryFactory", mockRepos, testObj);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testFactory() throws RepositoryException, IOException {
        mockStatic(ObjectService.class);
        ObjectService mockObjects = mock(ObjectService.class);
        when(ObjectService.get(mockRepo)).thenReturn(mockObjects);
        testObj.buildRepository();
        assertEquals(mockRepo, testObj.getObject());
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testFactoryMetadata() {
        assertEquals(Repository.class, testObj.getObjectType());
        assertEquals(true, testObj.isSingleton());
    }

    private static void inject(String name, Object value, Object object) {
        try {
            Field field =
                ModeShapeRepositoryFactoryBean.class.getDeclaredField(name);
            if (!field.isAnnotationPresent(Inject.class)) {
                System.err.println(
                        "WARNING: test sets ModeShapeRepositoryFactoryBean." +
                                name + ", which is not annotated as an @Inject");
            }
            field.setAccessible(true);
            field.set(object, value);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
