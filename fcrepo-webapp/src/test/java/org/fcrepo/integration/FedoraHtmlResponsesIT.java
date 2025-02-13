/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX;
import static com.google.common.collect.Lists.transform;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomElement;
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

    @BeforeEach
    public void setUp() {
        webClient = getDefaultWebClient();

        javascriptlessWebClient = getDefaultWebClient();
        javascriptlessWebClient.getOptions().setJavaScriptEnabled(false);
    }

    @AfterEach
    public void cleanUp() {
        webClient.close();
        javascriptlessWebClient.close();
    }

    @Test
    public void testDescribeHtml() throws IOException {
        final HtmlPage page = webClient.getPage(serverAddress);
        ((HtmlElement)page.getFirstByXPath("//h4[text()='Properties']")).click();

        checkForHeaderBranding(page);
        final String namespaceLabel = page
            .getFirstByXPath("//span[@title='" + REPOSITORY_NAMESPACE + "']/text()")
            .toString();

        assertEquals(namespaceLabel, "fedora:", "Expected to find namespace URIs displayed as their prefixes");
    }

    @Test
    public void testCreateNewNodeWithProvidedId() throws IOException {
        createAndVerifyObjectWithIdFromRootPage(newPid());
    }

    private String newPid() {
        return randomUUID().toString();
    }

    private HtmlPage createAndVerifyObjectWithIdFromRootPage(final String pid) throws IOException {
        return createAndVerifyObjectWithIdFromRootPage(pid, "basic container");
    }

    private HtmlPage createAndVerifyObjectWithIdFromRootPage(final String pid, final String containerType)
            throws IOException {
        final HtmlPage page = webClient.getPage(serverAddress);
        final HtmlForm form = (HtmlForm)page.getElementById("action_create");
        final HtmlSelect type = (HtmlSelect)page.getElementById("new_mixin");
        type.getOptionByValue(containerType).setSelected(true);

        final HtmlInput new_id = (HtmlInput)page.getElementById("new_id");
        new_id.setValueAttribute(pid);
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();


        try {
            final HtmlPage page1 = webClient.getPage(serverAddress + pid);
            assertEquals(serverAddress + pid, page1.getTitleText(), "Page had wrong title!");
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
        type.getOptionByValue("basic container").setSelected(true);
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();

        final HtmlPage page1 = javascriptlessWebClient.getPage(serverAddress);
        assertTrue(!page1.asText().equals(page.asText()), "Didn't see new information in page!");
    }

    @Test
    public void testCreateNewBasicContainer() throws IOException {
        final HtmlPage newPage = createAndVerifyObjectWithIdFromRootPage(newPid(), "basic container");
        assertTrue(newPage.asText().contains("http://www.w3.org/ns/ldp#BasicContainer"),
                "Set container type to ldp:BasicContainer");
    }

    @Test
    public void testCreateNewDirectContainer() throws IOException {
        final HtmlPage newPage = createAndVerifyObjectWithIdFromRootPage(newPid(), "direct container");
        assertTrue(newPage.asText().contains("http://www.w3.org/ns/ldp#DirectContainer"),
                "Set container type to ldp:DirectContainer");
    }

    @Test
    public void testCreateNewIndirectContainer() throws IOException {
        final HtmlPage newPage = createAndVerifyObjectWithIdFromRootPage(newPid(), "indirect container");
        assertTrue(newPage.asText().contains("http://www.w3.org/ns/ldp#IndirectContainer"),
                "Set container type to ldp:IndirectContainer");
    }

    @Test
    public void testCreateNewDatastream() throws Exception {

        // can't do this with javascript, because HTMLUnit doesn't speak the HTML5 file api
        final HtmlPage page = javascriptlessWebClient.getPage(serverAddress);

        final HtmlSelect type = (HtmlSelect)page.getElementById("new_mixin");
        type.getOptionByValue("binary").setSelected(true);

        final HtmlFileInput fileInput = (HtmlFileInput)page.getElementById("binary_payload");
        fileInput.setData("abcdef".getBytes());
        fileInput.setContentType("application/pdf");

        final HtmlButton button = (HtmlButton)page.getElementById("btn_action_create");
        button.click();

        // Without Javascript you end up at a blank page with just the newly generated URI as text.
        final Page resultPage = javascriptlessWebClient.getCurrentWindow().getEnclosedPage();
        final String newUri = resultPage.getWebResponse().getContentAsString();

        final Page page1 = javascriptlessWebClient.getPage(newUri + "/" + FCR_METADATA);
        assertTrue(page1.isHtmlPage());
        assertEquals(newUri, ((HtmlPage)page1).getTitleText());
    }

    @Test
    public void testCreateNewDatastreamWithNoFileAttached() throws IOException {

        final String pid = newPid();

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
        assertEquals(410, page2.getWebResponse()
                .getStatusCode(), "Didn't get a 410!");

        webClient.getOptions().setThrowExceptionOnFailingStatusCode(throwExceptionOnFailingStatusCode);
    }

    @Test
    public void testVersionsListWorksWhenNoVersionsPresent() throws IOException {
        final boolean throwExceptionOnFailingStatusCode = webClient.getOptions().isThrowExceptionOnFailingStatusCode();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);

        final String pid = createNewObject();
        final HtmlPage page = webClient.getPage(serverAddress + pid);
        final DomElement viewVersions = page.getElementById("view_versions");
        final Page versionsPage = viewVersions.click();
        assertEquals(versionsPage.getWebResponse().getStatusCode(), 200, "Didn't get a 200!");
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(throwExceptionOnFailingStatusCode);
    }

    /**
     * This test walks through the steps for creating an object, setting some
     * metadata, creating a version, updating that metadata, viewing the
     * version history to find that old version.
     *
     * @throws IOException exception thrown during this function
     */
    @Test
    public void testVersionCreationAndNavigation() throws Exception {
        final String pid = newPid();
        createAndVerifyObjectWithIdFromRootPage(pid);

        TimeUnit.SECONDS.sleep(1);

        final String updateSparql = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "PREFIX fedora: <" + REPOSITORY_NAMESPACE + ">\n" +
                "\n" +
                "INSERT DATA { <> dc:title \"Object Title\". }";
        postSparqlUpdateUsingHttpClient(updateSparql, pid);

        final HtmlPage objectPage = javascriptlessWebClient.getPage(serverAddress + pid);
        assertEquals("Object Title", objectPage.getFirstByXPath(
                    "//span[@property='http://purl.org/dc/elements/1.1/title']/text()").toString(),
                     "Title should be set.");

        TimeUnit.SECONDS.sleep(1);
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
        assertEquals(3, versionLinks.size(), "There should be three versions.");

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
                javascriptlessWebClient.getPage(versionLinks.get(chronological ? 1 : 2)
                    .getNodeValue());
        final List<DomText> v1Titles =
            castList(firstRevision
                    .getByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()"));
        final HtmlPage secondRevision =
                javascriptlessWebClient.getPage(versionLinks.get(chronological ? 2 : 1)
                    .getNodeValue());
        final List<DomText> v2Titles =
            castList(secondRevision
                    .getByXPath("//span[@property='http://purl.org/dc/elements/1.1/title']/text()"));

        assertEquals(1, v1Titles.size(), "Version one should have one title.");
        assertEquals(1, v2Titles.size(), "Version two should have one title.");
        assertNotEquals(v1Titles.get(0), v2Titles.get(0), "Each version should have a different title.");
        assertEquals("Object Title", v1Titles.get(0).getWholeText(), "First version should be preserved.");
        assertEquals("Updated Title", v2Titles.get(0).getWholeText(), "Second version should be preserved.");
    }

    private static void postSparqlUpdateUsingHttpClient(final String sparql, final String pid) throws IOException {
        final HttpPatch method = new HttpPatch(serverAddress + pid);
        method.addHeader(CONTENT_TYPE, "application/sparql-update");
        final StringEntity entity = new StringEntity(sparql, StandardCharsets.UTF_8);
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        assertEquals(204, response.getStatusLine().getStatusCode(),
                "Expected successful response.");
    }

    @Test
    public void testCreateNewObjectAndSetProperties() throws Exception {
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
    public void testSimpleSearch() throws Exception {
        final HtmlPage page = webClient.getPage(serverAddress);
        page.getAnchorByText("Search").click();

        final HtmlPage searchPage = (HtmlPage)webClient.getCurrentWindow().getEnclosedPage();

        final HtmlForm form = (HtmlForm)searchPage.getElementById("action_search");
        final HtmlInput q = (HtmlInput)searchPage.getElementById("search_value_1");
        q.setValueAttribute("info:fedora/*");
        final HtmlButton button = form.getFirstByXPath("button");
        button.click();
    }


    private static void checkForHeaderBranding(final HtmlPage page) {
        assertNotNull(
                page.getFirstByXPath("//nav[@role='navigation']/div[@class='navbar-header']/a[@class='navbar-brand']"));
    }

    private String createNewObject() throws IOException {

        final String pid = newPid();

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

    private CredentialsProvider getFedoraAdminCredentials() {
        final CredentialsProvider credentials  = new DefaultCredentialsProvider();
        credentials.setCredentials(AuthScope.ANY, FEDORA_ADMIN_CREDENTIALS);
        return credentials;
    }

    private WebClient getDefaultWebClient() {

        final WebClient webClient = new WebClient(FIREFOX);
        webClient.addRequestHeader(ACCEPT, "text/html");
        webClient.setCredentialsProvider(getFedoraAdminCredentials());
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
