
package org.fcrepo.kernel.utils.iterators;

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class RdfPersisterTest {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private RdfStream mockStream;

    private RdfPersister testPersister;

    private boolean successfullyOperatedOnAProperty = false;

    private boolean successfullyOperatedOnAMixin = false;

    private static final Model m = createDefaultModel();

    private static final Triple propertyTriple = create(createAnon(),
            createAnon(), createAnon());

    private static final Statement propertyStatement = m
            .asStatement(propertyTriple);

    private static final Triple mixinTriple = create(createAnon(),
            type.asNode(), createAnon());

    private static final Statement mixinStatement = m.asStatement(mixinTriple);


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockStream.hasNext()).thenReturn(true, true, false);
        when(mockStream.next()).thenReturn(propertyTriple, mixinTriple);
        when(
                mockGraphSubjects.getNodeFromGraphSubject(propertyStatement
                        .getSubject())).thenReturn(mockNode);
        when(
                mockGraphSubjects.getNodeFromGraphSubject(mixinStatement
                        .getSubject())).thenReturn(mockNode);
        when(
                mockGraphSubjects.isFedoraGraphSubject(propertyStatement
                        .getSubject())).thenReturn(true);
        when(
                mockGraphSubjects.isFedoraGraphSubject(mixinStatement
                        .getSubject())).thenReturn(true);

        testPersister =
            new RdfPersister(mockGraphSubjects, mockSession, mockStream) {

                @Override
                protected void operateOnProperty(final Statement s,
                    final Node subjectNode) throws RepositoryException {
                    successfullyOperatedOnAProperty = true;
                }

                @Override
                protected void operateOnMixin(final Resource mixinResource,
                        final Node subjectNode) throws RepositoryException {
                    successfullyOperatedOnAMixin = true;
                }
            };

    }

    @Test
    public void testConsumeAsync() throws Exception {
        testPersister.consumeAsync();
        assertTrue("Didn't successfully operate on a property!",
                successfullyOperatedOnAProperty);

        assertTrue("Didn't successfully operate on a mixin!",
                successfullyOperatedOnAMixin);
    }

}
