package org.fcrepo.spring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.Resource;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ObjectService.class})
public class ModeShapeRepositoryFactoryBeanTest {

	private ModeShapeRepositoryFactoryBean testObj;
	
	private Resource mockConfig;
	
	private JcrRepository mockRepo;
	
	private JcrRepositoryFactory mockRepos;
	
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
	
	@Test
	public void testFactory() throws RepositoryException, IOException {
		mockStatic(ObjectService.class);
		ObjectService mockObjects = mock(ObjectService.class);
		when(ObjectService.get(mockRepo)).thenReturn(mockObjects);
		testObj.buildRepository();
		assertEquals(mockRepo, testObj.getObject());
	}
	
	@Test
	public void testFactoryMetadata() {
		assertEquals(Repository.class, testObj.getObjectType());
		assertEquals(true, testObj.isSingleton());
	}
	
	private static void inject(String name, Object value, Object object) {
		try {
			Field field = ModeShapeRepositoryFactoryBean.class.getDeclaredField(name);
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
