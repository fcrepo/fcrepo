/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.api.rdf;

import static com.google.common.collect.ImmutableBiMap.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import jakarta.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * <p>HttpTripleUtilTest class.</p>
 *
 * @author awoods
 */
@ExtendWith(MockitoExtension.class)
public class HttpTripleUtilTest {

    private HttpTripleUtil testObj;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private UriAwareResourceModelFactory mockBean1;

    @Mock
    private UriAwareResourceModelFactory mockBean2;

    @Mock
    private ApplicationContext mockContext;

    @Mock
    private FedoraResource mockResource;

    @BeforeEach
    public void setUp() {
        testObj = new HttpTripleUtil();
        testObj.setApplicationContext(mockContext);
    }

    @Test
    public void shouldAddTriplesFromRegisteredBeans() {
        final Map<String, UriAwareResourceModelFactory> mockBeans = of("doesnt", mockBean1, "matter", mockBean2);
        when(mockContext.getBeansOfType(UriAwareResourceModelFactory.class)).thenReturn(mockBeans);
        when(mockBean1.createModelForResource(eq(mockResource), eq(mockUriInfo))).thenReturn(
                createDefaultModel());
        when(mockBean2.createModelForResource(eq(mockResource), eq(mockUriInfo))).thenReturn(
                createDefaultModel());

        try (final RdfStream rdfStream = new DefaultRdfStream(createURI("info:subject"))) {

            assertTrue(testObj.addHttpComponentModelsForResourceToStream(rdfStream, mockResource, mockUriInfo)
                    .count() >= 0);

            verify(mockBean1).createModelForResource(eq(mockResource), eq(mockUriInfo));
            verify(mockBean2).createModelForResource(eq(mockResource), eq(mockUriInfo));
        }
    }
}
