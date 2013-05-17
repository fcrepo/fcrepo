
package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class FedoraFieldSearchIT extends AbstractResourceIT {

    @Test
    public void testSearchForm() throws Exception {

        assertEquals(200, getStatus(new HttpGet(serverAddress + "fcr:search")));
    }

    @Test
    public void testSearchSubmit() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "fcr:search");
        final List<BasicNameValuePair> list =
                new ArrayList<BasicNameValuePair>();

        list.add(new BasicNameValuePair("terms", ""));
        list.add(new BasicNameValuePair("offset", "0"));
        list.add(new BasicNameValuePair("maxResults", "1"));
        final UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list);
        method.setEntity(formEntity);
        assertEquals(200, getStatus(method));

    }

    @Test
    public void testSearchSubmitRdfJSON() throws Exception {
        /* first post an object which can be used for the search */
        HttpPost postObj = postObjMethod("testobj");
        HttpResponse postResp = execute(postObj);
        postObj.releaseConnection();
        assertEquals(201, postResp.getStatusLine().getStatusCode());

        /* and add a dc title to the object so the query returns a result */
        HttpPost postDc = new HttpPost(serverAddress + "objects/testobj");
        postDc.setHeader("Content-Type", "application/sparql-update");
        String updateString = "INSERT { <" + serverAddress + "objects/testobj> <http://purl.org/dc/terms/title> \"testobj\" } WHERE { }";
        postDc.setEntity(new StringEntity(updateString));
        HttpResponse dcResp = execute(postDc);
        assertTrue(dcResp.getStatusLine().toString(),204 == dcResp.getStatusLine().getStatusCode());
        postDc.releaseConnection();

        final HttpPost method = new HttpPost(serverAddress + "fcr:search");
        method.setHeader("Accept", "application/rdf+json");
        final List<BasicNameValuePair> list = new ArrayList<>();
        list.add(new BasicNameValuePair("terms", "testobj"));
        list.add(new BasicNameValuePair("offset", "0"));
        list.add(new BasicNameValuePair("maxResults", "1"));
        final UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list);
        method.setEntity(formEntity);

        HttpResponse resp = execute(method);
        String json = IOUtils.toString(resp.getEntity().getContent());
        assertEquals("Server returned\n"  + json, 200, resp.getStatusLine().getStatusCode());
        assertTrue(new JSONObject(json).getJSONObject(serverAddress + "fcr:search")
                .getJSONArray("info:fedora/fedora-system:def/search#numSearchResults")
                .getJSONObject(0).getString("value").equals("1"));
        method.releaseConnection();
    }

    @Test
    public void testSearchSubmitPaging() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "fcr:search");
        final List<BasicNameValuePair> list =
                new ArrayList<BasicNameValuePair>();

        list.add(new BasicNameValuePair("terms", ""));
        list.add(new BasicNameValuePair("offset", "1"));
        list.add(new BasicNameValuePair("maxResults", "1"));
        final UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list);
        method.setEntity(formEntity);
        assertEquals(200, getStatus(method));

    }
}
