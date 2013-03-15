package org.fcrepo.api;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.fcrepo.api.legacy.AbstractResourceTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class FedoraFieldSearchTest extends AbstractResourceTest {
    @Test
    public void testSearchForm() throws Exception {

        assertEquals(200, getStatus(new HttpGet(serverAddress + "search")));
    }

    @Test
    public void testSearchSubmit() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "search");
        List<BasicNameValuePair> list = new ArrayList<BasicNameValuePair>();

        list.add(new BasicNameValuePair("terms", ""));
        list.add(new BasicNameValuePair("offset", "0"));
        list.add(new BasicNameValuePair("maxResults", "1"));
        final UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list);
        method.setEntity(formEntity);
        assertEquals(200, getStatus(method));

    }
}
