/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
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
