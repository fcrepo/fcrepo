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

import static com.google.common.base.Throwables.propagate;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.services.ObjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.api.Repository;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({ObjectService.class})
public class ModeShapeRepositoryFactoryBeanTest {

    private static final Logger LOGGER =
            getLogger(ModeShapeRepositoryFactoryBeanTest.class);

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
    public void setUp() throws RepositoryException, IOException {
        initMocks(this);
        when(mockRepo.login()).thenReturn(mockSession);
        testObj = new ModeShapeRepositoryFactoryBean();
        testObj.setRepositoryConfiguration(mockConfig);
        when(mockRepos.getRepository(any(Map.class))).thenReturn(mockRepo);
        inject("jcrRepositoryFactory", mockRepos, testObj);
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

    private static void inject(final String name, final Object value,
            final Object object) {
        try {
            final Field field =
                    ModeShapeRepositoryFactoryBean.class.getDeclaredField(name);
            if (!field.isAnnotationPresent(Inject.class)) {
                LOGGER.error(
                        "WARNING: test sets ModeShapeRepositoryFactoryBean.{}, which is not annotated as an @Inject!",
                        name);
            }
            field.setAccessible(true);
            field.set(object, value);
        } catch (final Throwable t) {
            propagate(t);
        }
    }
}
