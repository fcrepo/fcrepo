/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.integration;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX_24;
import static com.google.common.collect.Lists.transform;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONFIG_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.BasicHttpEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

/**
 * <p>FedoraHtmlResponsesIT class.</p>
 *
 * @author cbeer
 */
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
        ((HtmlElement)page.getFirstByXPath("//h4[text()='Properties']")).click();

        checkForHeaderBranding(page);
        final String namespaceLabel = page
            .getFirstByXPath("//span[@title='" + REPOSITORY_NAMESPACE + "']/text()")
            .toString();

        assertEquals("Expected to find namespace URIs displayed as their prefixes", "fedora:",
                        namespaceLabel);
    }

    @Test
    public void testCreateNewNodeWithProvidedId() throws IOException {
        createAndVerifyObjectWithIdFromRootPage(randomUUID().toString());
    }

    private HtmlPage createAndVerifyObjectWithIdFromRootPage(final String pid) throws IOException {
        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");
        final HtmlSelect type = (HtmlSelect)page.getElementById("new_mixin");
        type.getOptionByValue("container").setSelected(true);

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
    public void testCreateNewNodeWithGeneratedId() throws IOException {

        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");
        final HtmlSelect type = (HtmlSelect)page.getElementById("new_mixin");
        type.getOptionByValue("container").setSelected(true);
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = javascriptlessWebClient.getPage(serverAddress);
        assertTrue("Didn't see new information in page!", !page1.asText().equals(page.asText()));
    }

    @Test
    @Ignore("The htmlunit web client can't handle the HTML5 file API")
    public void testCreateNewDatastream() throws IOException {

        final String pid = randomUUID().toString();

        // can't do this with javascript, because HTMLUnit doesn't speak the HTML5 file api
        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");

        final HtmlInput slug = form.getInputByName("slug");
        slug.setValueAttribute(pid);

        final HtmlSelect type = (HtmlSelect)page.getElementById("new_mixin");
        type.getOptionByValue("binary").setSelected(true);

        final HtmlFileInput fileInput = (HtmlFileInput)page.getElementById("datastream_payload");
        fileInput.setData("abcdef".getBytes());
        fileInput.setContentType("application/pdf");

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = javascriptlessWebClient.getPage(serverAddress + pid);
        assertEquals(serverAddress + pid, page1.getTitleText());
    }

    @Test
    public void testCreateNewDatastreamWithNoFileAttached() throws IOException {

        final String pid = randomUUID().toString();

        // can't do this with javascript, because HTMLUnit doesn't speak the HTML5 file api
        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");

        final HtmlInput slug = form.getInputByName("slug");
        slug.setValueAttribute(pid);

        final HtmlSelect type = (HtmlSelect)page.getElementById("new_mixin");
        type.getOptionByValue("binary").setSelected(true);

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        javascriptlessWebClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        final int status = javascriptlessWebClient.getPage(serverAddress + pid).getWebResponse().getStatusCode();
        assertEquals(NOT_FOUND.getStatusCode(), status);
    }

    @Test
    public void testCreateNewObjectAndDeleteIt() throws IOException {
        final boolean throwExceptionOnFailingStatusCode = webClient.getOptions().isThrowExceptionOnFailingStatusCode();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

        final String pid = createNewObject();
        final HtmlPage page = webClient.getPage(serverAddress + pid);
        final HtmlForm action_delete = page.getFormByName("action_delete");
        action_delete.getButtonByName("delete-button").click();
        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);

        final Page page2 = webClient.getPage(serverAddress + pid);
        assertEquals("Didn't get a 410!", 410, page2.getWebResponse()
                .getStatusCode());

        webClient.getOptions().setThrowExceptionOnFailingStatusCode(throwExceptionOnFailingStatusCode);
    }

    @Test
    public void testNodeTypes() throws IOException {
        final HtmlPage page = javascriptlessWebClient.getPage(serverAddress + "fcr:nodetypes");
        assertTrue(page.asText().contains("fedora:Container"));
    }

    /**
     * This test walks through the steps for creating an object, setting some
     * metadata, creating a version, updating that metadata, viewing the
     * version history to find that old version.
     *
     * @throws IOException exception thrown during this function
     */
    @Ignore
    @Test
    public void testVersionCreationAndNavigation() throws IOException {
        final String pid = randomUUID().toString();
        createAndVerifyObjectWithIdFromRootPage(pid);

        final String updateSparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "PREFIX fedora: <" + REPOSITORY_NAMESPACE + ">\n" +
                "\n" +
                "INSERT DATA { <> fedoraconfig:versioningPolicy \"auto-version\" ; dc:title \"Object Title\". }";
        postSparqlUpdateUsingHttpClient(updateSparql, pid);

        final HtmlPage objectPage = javascriptlessWebClient.getPage(serverAddress + pid);
        assertEquals("Auto versioning should be set.", "auto-version",
                     objectPage.getFirstByXPath(
                             "//span[@property='" + FEDORA_CONFIG_NAMESPACE + "versioningPolicy']/text()")
                             .toString());
        assertEquals("Title should be set.", "Object Title",
                     objectPage.getFirstByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()")
                             .toString());

        final String updateSparql2 = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "\n" +
                "DELETE { <> dc:title ?t }\n" +
                "INSERT { <> dc:title \"Updated Title\" }" +
                "WHERE { <> dc:title ?t }";
        postSparqlUpdateUsingHttpClient(updateSparql2, pid);

        final HtmlPage versions =
                javascriptlessWebClient.getPage(serverAddress + pid + "/fcr:versions");
        final List<DomAttr> versionLinks =
            castList(versions.getByXPath("//a[@class='version_link']/@href"));
        assertEquals("There should be two revisions.", 2, versionLinks.size());

        // get the labels
        // will look like "Version from 2013-00-0T00:00:00.000Z"
        // and will sort chronologically based on a String comparison
        final List<DomText> labels =
            castList(versions
                    .getByXPath("//a[@class='version_link']/text()"));
        final boolean chronological = labels.get(0).asText().compareTo(labels.get(1).toString()) < 0;
        logger.debug("Versions {} in chronological order: {}, {}",
                     chronological ? "are" : "are not", labels.get(0).asText(), labels.get(1).asText());

        final HtmlPage firstRevision =
                javascriptlessWebClient.getPage(versionLinks.get(chronological ? 0 : 1)
                    .getNodeValue());
        final List<DomText> v1Titles =
            castList(firstRevision
                    .getByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()"));
        final HtmlPage secondRevision =
                javascriptlessWebClient.getPage(versionLinks.get(chronological ? 1 : 0)
                    .getNodeValue());
        final List<DomText> v2Titles =
            castList(secondRevision
                    .getByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()"));

        assertEquals("Version one should have one title.", 1, v1Titles.size());
        assertEquals("Version two should have one title.", 1, v2Titles.size());
        assertNotEquals("Each version should have a different title.", v1Titles.get(0), v2Titles.get(0));
        assertEquals("First version should be preserved.", "Object Title", v1Titles.get(0).getWholeText());
        assertEquals("Second version should be preserved.", "Updated Title", v2Titles.get(0).getWholeText());
    }

    private static void postSparqlUpdateUsingHttpClient(final String sparql, final String pid) throws IOException {
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

        final HtmlPage page = javascriptlessWebClient.getPage(serverAddress + pid);
        final HtmlForm form = (HtmlForm)page.getElementById("action_sparql_update");
        final HtmlTextArea sparql_update_query = (HtmlTextArea)page.getElementById("sparql_update_query");
        sparql_update_query.setText("INSERT { <> <info:some-predicate> 'asdf' } WHERE { }");

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = javascriptlessWebClient.getPage(serverAddress + pid);
        assertTrue(page1.getElementById("metadata").asText().contains("some-predicate"));
    }

    @Test
    @Ignore("htmlunit can't see links in the HTML5 <nav> element..")
    public void testSparqlSearch() throws IOException {
        final HtmlPage page = webClient.getPage(serverAddress);

        logger.error(page.toString());
        page.getAnchorByText("Search").click();

        final HtmlForm form = (HtmlForm)page.getElementById("action_sparql_select");
        final HtmlTextArea q = form.getTextAreaByName("q");
        q.setText("SELECT ?subject WHERE { ?subject a <" + REPOSITORY_NAMESPACE + "Resource> }");
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();
    }


    private static void checkForHeaderBranding(final HtmlPage page) {
        assertNotNull(
                page.getFirstByXPath("//nav[@role='navigation']/div[@class='navbar-header']/a[@class='navbar-brand']"));
    }

    private String createNewObject() throws IOException {

        final String pid = randomUUID().toString();

        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = page.getFormByName("action_create");

        final HtmlInput slug = form.getInputByName("slug");
        slug.setValueAttribute(pid);

        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);


        return pid;
    }


    private WebClient getDefaultWebClient() {

        final WebClient webClient = new WebClient(FIREFOX_24);
        webClient.addRequestHeader("Accept", "text/html");

        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        //Suppress warning from IncorrectnessListener
        webClient.setIncorrectnessListener(new SuppressWarningIncorrectnessListener());

        //Suppress css warning with the silent error handler.
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;

    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(final List<?> l) {
        return transform(l, x -> (T) x);
    }

    private static class SuppressWarningIncorrectnessListener
            implements IncorrectnessListener {
        @Override
        public void notify(final String arg0, final Object arg1) {

        }
      }
}
