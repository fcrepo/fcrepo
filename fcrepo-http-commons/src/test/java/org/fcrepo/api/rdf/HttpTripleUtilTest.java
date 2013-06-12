package org.fcrepo.api.rdf;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraResource;
import org.fcrepo.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class HttpTripleUtilTest {

    private HttpTripleUtil testObj;
    private Dataset dataset;
    private UriInfo mockUriInfo;
    private GraphSubjects mockSubjects;
    private ApplicationContext mockContext;

    @Before
    public void setUp() {
        mockContext = mock(ApplicationContext.class);
        testObj = new HttpTripleUtil();
        testObj.setApplicationContext(mockContext);

        dataset = DatasetFactory.create(ModelFactory.createDefaultModel());
        mockUriInfo = mock(UriInfo.class);
        mockSubjects = mock(GraphSubjects.class);


    }

    @Test
    public void shouldAddTriplesFromRegisteredBeans() throws RepositoryException {
        final FedoraResource mockResource = mock(FedoraResource.class);

        UriAwareResourceModelFactory mockBean1 = mock(UriAwareResourceModelFactory.class);
        UriAwareResourceModelFactory mockBean2 = mock(UriAwareResourceModelFactory.class);
        Map<String, UriAwareResourceModelFactory> mockBeans = ImmutableBiMap.of("doesnt", mockBean1, "matter", mockBean2);
        when(mockContext.getBeansOfType(UriAwareResourceModelFactory.class)).thenReturn(mockBeans);
        when(mockBean1.createModelForResource(eq(mockResource), eq(mockUriInfo), eq(mockSubjects))).thenReturn(ModelFactory.createDefaultModel());
        when(mockBean2.createModelForResource(eq(mockResource), eq(mockUriInfo), eq(mockSubjects))).thenReturn(ModelFactory.createDefaultModel());

        testObj.addHttpComponentModelsForResource(dataset, mockResource, mockUriInfo, mockSubjects);
        verify(mockBean1).createModelForResource(eq(mockResource), eq(mockUriInfo), eq(mockSubjects));
        verify(mockBean2).createModelForResource(eq(mockResource), eq(mockUriInfo), eq(mockSubjects));
    }
}
