
package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.fcrepo.jaxb.responses.management.NamespaceListing;
import org.fcrepo.jaxb.responses.management.NamespaceListing.Namespace;
import org.junit.Test;

public class FedoraNamespacesIT extends AbstractResourceIT {

    @Test
    public void testGet() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.setAnnotationIntrospector(introspector);

        HttpGet get = new HttpGet(serverAddress + "fcr:namespaces");
        get.addHeader("Accept", "application/json");
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);
        
        String content = EntityUtils.toString(response.getEntity());
        NamespaceListing listing = mapper.readValue(content, NamespaceListing.class);
        assertNotNull(listing);
        
        get = new HttpGet(serverAddress + "fcr:namespaces/nt");
        response = execute(get);
        status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);
        
        content = EntityUtils.toString(response.getEntity());
        Namespace ns = mapper.readValue(content, Namespace.class);
        assertNotNull(ns);
        
    }

    @Test
    public void testCreate() throws Exception {
        String expected = "http://foo.gov/" + new java.util.Date().getTime() + "/";
        final HttpPost method = new HttpPost(serverAddress + "fcr:namespaces/foo");
 
        final StringEntity entity = new StringEntity(expected);
        method.setEntity(entity);
        assertEquals(201, getStatus(method));
        final HttpGet get = new HttpGet(method.getURI());
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);
        
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.setAnnotationIntrospector(introspector);
        String content = EntityUtils.toString(response.getEntity());
        Namespace actual = mapper.readValue(content, Namespace.class);
        assertEquals(expected, actual.uri.toString());
    }

}
