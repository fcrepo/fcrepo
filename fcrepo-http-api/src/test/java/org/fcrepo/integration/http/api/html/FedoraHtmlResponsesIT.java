/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.http.api.html;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class FedoraHtmlResponsesIT extends AbstractResourceIT {

    private WebClient webClient;
    private WebClient javascriptlessWebClient;

    @Before
    public void setUp() {
        webClient = getDefaultWebClient();

        javascriptlessWebClient = getDefaultWebClient();
        javascriptlessWebClient.getOptions().setJavaScriptEnabled(false);
    }

    @After
    public void cleanUp() {
        webClient.closeAllWindows();
        javascriptlessWebClient.closeAllWindows();
    }

    @Test
    public void testDescribeHtml() throws IOException {
        final HtmlPage page = webClient.getPage(serverAddress);

        checkForHeaderBranding(page);
        checkForHeaderSearch(page);

        final String namespaceLabel = page
            .getFirstByXPath("//span[@title='http://fedora.info/definitions/v4/repository#']/text()")
            .toString();

        assertEquals("Expected to find namespace URIs displayed as their prefixes", "fcrepo:",
                        namespaceLabel);
    }

    @Test
    public void testCreateNewNodeWithProvidedId() throws IOException, InterruptedException {
        createAndVerifyObjectWithIdFromRootPage(randomUUID().toString());
    }

    private HtmlPage createAndVerifyObjectWithIdFromRootPage(String pid) throws IOException {
        final HtmlPage page = javascriptlessWebClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");
        final HtmlSelect type = form.getSelectByName("mixin");
        type.getOptionByValue("fedora:object").setSelected(true);

        final HtmlInput new_id = (HtmlInput)page.getElementById("new_id");
        new_id.setValueAttribute(pid);
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();


        try {
            final HtmlPage page1 = webClient.getPage(serverAddress + pid);
            assertEquals("Page had wrong title!", serverAddress + pid, page1.getTitleText());
            return page1;
        } catch (final FailingHttpStatusCodeException e) {
            fail("Did not successfully retrieve created page! Got HTTP code: " + e.getStatusCode());
            return null;
        }
    }

    @Test
    public void testCreateNewNodeWithGeneratedId() throws IOException, InterruptedException {

        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");
        final HtmlSelect type = form.getSelectByName("mixin");
        type.getOptionByValue("fedora:object").setSelected(true);
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = webClient.getPage(serverAddress);
        assertTrue("Didn't see new information in page!", page1.asText().length() > page.asText().length());
    }

    @Test
    public void testCreateNewDatastream() throws IOException, InterruptedException {

        final String pid = randomUUID().toString();

        // can't do this with javascript, because HTMLUnit doesn't speak the HTML5 file api
        final HtmlPage page = javascriptlessWebClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");

        final HtmlInput slug = form.getInputByName("slug");
        slug.setValueAttribute(pid);

        final HtmlSelect type = form.getSelectByName("mixin");
        type.getOptionByValue("fedora:datastream").setSelected(true);

        final HtmlFileInput fileInput = (HtmlFileInput)page.getElementById("datastream_payload");
        fileInput.setData("abcdef".getBytes());
        fileInput.setContentType("application/pdf");

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = webClient.getPage(serverAddress + pid);
        assertEquals(serverAddress + pid, page1.getTitleText());
    }

    @Test
    public void testCreateNewObjectAndDeleteIt() throws IOException, InterruptedException {
        final boolean throwExceptionOnFailingStatusCode = webClient.getOptions().isThrowExceptionOnFailingStatusCode();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

        final String pid = createNewObject();
        final HtmlPage page = webClient.getPage(serverAddress + pid);
        final HtmlForm action_delete = page.getFormByName("action_delete");
        action_delete.getButtonByName("delete-button").click();
        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);

        final HtmlPage page2 = webClient.getPage(serverAddress + pid);
        assertEquals("Didn't get a 404!", 404, page2.getWebResponse()
                .getStatusCode());

        webClient.getOptions().setThrowExceptionOnFailingStatusCode(throwExceptionOnFailingStatusCode);
    }

    @Test
    public void testNodeTypes() throws IOException {
        final HtmlPage page = webClient.getPage(serverAddress + "fcr:nodetypes");
        assertTrue(page.asText().contains("fedora:object"));
    }

    /**
     * This test walks through the steps for creating an object, setting some
     * metadata, creating a version, updating that metadata, viewing the
     * version history to find that old version.
     */
    @Ignore
    @Test
    public void testVersionCreationAndNavigation() throws IOException {
        final String pid = randomUUID().toString();
        createAndVerifyObjectWithIdFromRootPage(pid);

        final String updateSparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#>\n" +
                "\n" +
                "INSERT DATA { <> fedora:versioningPolicy \"auto-version\" ; dc:title \"Object Title\". }";
        postSparqlUpdateUsingHttpClient(updateSparql, pid);

        final HtmlPage objectPage = webClient.getPage(serverAddress + pid);
        assertEquals("Auto versioning should be set.", "auto-version", objectPage.getFirstByXPath("//span[@property='http://fedora.info/definitions/v4/rest-api#versioningPolicy']/text()").toString());
        assertEquals("Title should be set.", "Object Title", objectPage.getFirstByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()").toString());

        final String updateSparql2 = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "\n" +
                "DELETE { <> dc:title ?t }\n" +
                "INSERT { <> dc:title \"Updated Title\" }" +
                "WHERE { <> dc:title ?t }";
        postSparqlUpdateUsingHttpClient(updateSparql2, pid);

        final HtmlPage versions = webClient.getPage(serverAddress + pid + "/fcr:versions");
        List<DomAttr> versionLinks = (List<DomAttr>) versions.getByXPath("//a[@class='version_link']/@href");
        assertEquals("There should be two revisions.", 2, versionLinks.size());

        // get the labels
        // will look like "Version from 2013-00-0T00:00:00.000Z"
        // and will sort chronologically based on a String comparison
        List<DomText> labels = (List<DomText>) versions.getByXPath("//a[@class='version_link']/text()");
        boolean chronological = labels.get(0).asText().compareTo(labels.get(1).toString()) < 0;
        logger.debug("Versions {} in chronological order: {}, {}", chronological ? "are" : "are not", labels.get(0).asText(), labels.get(1).asText());


        final HtmlPage firstRevision = webClient.getPage(versionLinks.get(chronological ? 0 : 1).getNodeValue());
        final List<DomText> v1Titles = (List<DomText>) firstRevision.getByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()");
        final HtmlPage secondRevision = webClient.getPage(versionLinks.get(chronological ? 1 : 0).getNodeValue());
        final List<DomText> v2Titles = (List<DomText>) secondRevision.getByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()");

        assertEquals("Version one should have one title.", 1, v1Titles.size());
        assertEquals("Version two should have one title.", 1, v2Titles.size());
        assertNotEquals("Each version should have a different title.", v1Titles.get(0), v2Titles.get(0));
        assertEquals("First version should be preserved.", "Object Title", v1Titles.get(0).getWholeText());
        assertEquals("Second version should be preserved.", "Updated Title", v2Titles.get(0).getWholeText());
    }

    /**
     * This doesn't work due to some incompatibility between HtmlUnit's
     * "browser" and jquery 1.9.1.  Until that's corrected, or we change the
     * javascript we use, consider using {@link
     * #postSparqlUpdateUsingHttpClient(String, String)} instead.
     */
    private void postSparqlUpdateUsingHtml(String sparql, HtmlPage objectPage) throws IOException {
        final HtmlForm form = (HtmlForm) objectPage.getElementById("action_sparql_update");
        final HtmlTextArea sparql_update_query = (HtmlTextArea) objectPage.getElementById("sparql_update_query");
        sparql_update_query.setText(sparql);
        System.out.println(form.getFirstByXPath("button"));
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();
    }

    private void postSparqlUpdateUsingHttpClient(String sparql, String pid) throws IOException {
        final HttpPatch method = new HttpPatch(serverAddress + pid);
        method.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(sparql.getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        assertEquals("Expected successful response.", 204,
                response.getStatusLine().getStatusCode());
    }

    @Test
    @Ignore
    public void testCreateNewObjectAndSetProperties() throws IOException {
        final String pid = createNewObject();

        final HtmlPage page = webClient.getPage(serverAddress + pid);
        final HtmlForm form = (HtmlForm)page.getElementById("action_sparql_update");
        final HtmlTextArea sparql_update_query = (HtmlTextArea)page.getElementById("sparql_update_query");
        sparql_update_query.setText("INSERT { <> <info:some-predicate> 'asdf' } WHERE { }");

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = webClient.getPage(serverAddress + pid);
        assertTrue(page1.getElementById("metadata").asText().contains("some-predicate"));
    }

    @Test
    public void testCreateNewNamespace() throws IOException {
        final HtmlPage page1 = webClient.getPage(serverAddress + "fcr:namespaces");
        final HtmlForm form = (HtmlForm) page1.getElementById("action_register_namespace");
        final HtmlInput prefix = form.getInputByName("prefix");
        final HtmlInput uri = form.getInputByName("uri");
        final String prefix_value = "asdf";
        final String uri_value = "http://example.com/asdf";

        // Doesn't have namespace defined.
        assertFalse("Prefix is not already defined", page1.asText().contains(prefix_value));
        assertFalse("URI is not already defined", page1.asText().contains(uri_value));

        prefix.setValueAttribute(prefix_value);
        uri.setValueAttribute(uri_value);

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page2 = webClient.getPage(serverAddress + "fcr:namespaces");

        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);

        // Has namespace defined.
        assertTrue("New prefix was found", page2.asText().contains(prefix_value));
        assertTrue("New uri was found", page2.asText().contains(uri_value));
    }

    private void checkForHeaderSearch(final HtmlPage page) {
        final HtmlForm form = page.getFirstByXPath("//form[@role='search']");
        assertNotNull(form);
        assertEquals(serverAddress + "fcr:search", form.getActionAttribute());
    }

    private void checkForHeaderBranding(final HtmlPage page) {
        assertNotNull(page.getFirstByXPath("//nav[@role='navigation']/div[@class='navbar-header']/a[@class='navbar-brand']"));
    }

    private String createNewObject() throws IOException {

        final String pid = randomUUID().toString();

        final HtmlPage page = javascriptlessWebClient.getPage(serverAddress);
        final HtmlForm form = page.getFormByName("action_create");

        final HtmlInput slug = form.getInputByName("slug");
        slug.setValueAttribute(pid);

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        return pid;
    }


    private WebClient getDefaultWebClient() {

        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_17);
        webClient.addRequestHeader("Accept", "text/html");

        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        return webClient;

    }

}
