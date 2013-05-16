
package org.fcrepo.responses;

import static com.hp.hpl.jena.graph.Node.createLiteral;
import static com.hp.hpl.jena.graph.Node.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.responses.RdfSerializationUtils.setCachingHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RdfSerializationUtilsTest {

    Dataset testData = new DatasetImpl(createDefaultModel());

    {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));

    }

    @Test
    public void testGetFirstValueForPredicate() {
        final String foundValue =
                getFirstValueForPredicate(testData, createURI("test:predicate"));
        assertEquals("Didn't find correct value for predicate!", foundValue,
                "test:object");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetCachingHeaders() {
        final MultivaluedMap<?, ?> headers = new MultivaluedMapImpl();
        setCachingHeaders((MultivaluedMap<String, Object>) headers, testData);
        assertTrue(headers.get("Cache-Control").size() > 0);
    }

}
