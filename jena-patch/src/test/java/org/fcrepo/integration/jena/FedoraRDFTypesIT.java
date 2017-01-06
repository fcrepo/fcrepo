package org.fcrepo.integration.jena;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author awoods
 * @since 2017/01/04
 */
public class FedoraRDFTypesIT extends AbstractResourceIT {

    private static Set<String> contentTypes;

    private static final String STRING_XSD_TYPE = XSDDatatype.XSDstring.getURI();
    private static final String BOOLEAN_XSD_TYPE = XSDDatatype.XSDboolean.getURI();

    @BeforeClass
    public static void setup() {
        contentTypes = new HashSet<>();
        contentTypes.add("application/ld+json");
        contentTypes.add("application/n-triples");
        contentTypes.add("application/rdf+xml");
        contentTypes.add("application/x-turtle");
        contentTypes.add("text/n3");
        contentTypes.add("text/rdf+n3");
        contentTypes.add("text/turtle");
    }

    @Test
    public void testRDFTypes() throws IOException {
        // Create a test resource with String and Boolean properties
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "application/n3");
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\"; " +
                "<info:test#flag> \"\"^^<" + BOOLEAN_XSD_TYPE + "> .", UTF_8));
        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        contentTypes.forEach(contentType -> verifyResponse(subjectURI, contentType));
    }

    private void verifyResponse(final String subjectURI, final String contentType) {
        final HttpGet method = new HttpGet(subjectURI);
        method.addHeader(HttpHeaders.ACCEPT, contentType);
        try (final CloseableHttpResponse response = execute(method)) {
            final String body = EntityUtils.toString(response.getEntity());

            // Verify String XSD Type is present
            if (contentType.equals("application/ld+json")) {
                // String XSD Type should not be present in JSON-LD
                assertFalse("Did not find " + STRING_XSD_TYPE + " for " + contentType + " in " + body,
                        body.contains(STRING_XSD_TYPE));
            } else {
                assertTrue("Did not find " + STRING_XSD_TYPE + " for " + contentType + " in " + body,
                        body.contains(STRING_XSD_TYPE));
            }
            // Verify Boolean XSD Type is present
            assertTrue("Did not find " + BOOLEAN_XSD_TYPE + " for " + contentType + " in " + body,
                    body.contains(BOOLEAN_XSD_TYPE));

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
