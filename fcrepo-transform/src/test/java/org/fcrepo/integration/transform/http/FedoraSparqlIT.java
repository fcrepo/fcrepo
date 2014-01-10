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

package org.fcrepo.integration.transform.http;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.fcrepo.integration.AbstractResourceIT;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration({"/spring-test/master.xml", "/spring-test/test-container.xml"})
public class FedoraSparqlIT  extends AbstractResourceIT {

        @Autowired
        Repository repo;

    private Session session;

    @Before
    public void setUpTestData() throws RepositoryException {
        session = repo.login();
        session.setNamespacePrefix("zz", "http://zz.com/");
        final ValueFactory valueFactory = session.getValueFactory();
        final FedoraResourceImpl fedoraResource = new FedoraResourceImpl(session, "/abc", JcrConstants.NT_FOLDER);
        final FedoraResourceImpl fedoraResource2 = new FedoraResourceImpl(session, "/xyz", JcrConstants.NT_FOLDER);
        final FedoraResourceImpl fedoraResource3 = new FedoraResourceImpl(session, "/anobject", JcrConstants.NT_FOLDER);

        fedoraResource.getNode().setProperty("dc:title", new Value[] { valueFactory.createValue("xyz") });
        fedoraResource.getNode().setProperty("fedorarelsext:hasPart", new Value[] { valueFactory.createValue(fedoraResource2.getNode()) });
        fedoraResource2.getNode().setProperty("fedorarelsext:isPartOf", new Value[] { valueFactory.createValue(fedoraResource.getNode()) });
        fedoraResource3.getNode().setProperty("zz:name", new Value[] { valueFactory.createValue("junk") });
        fedoraResource3.getNode().setProperty("rdf:type", new Value[] { valueFactory.createValue("info:some-type") });
        session.save();

    }

    @After
    public void destroy() {
        session.logout();
    }
    
    @Test
    public void itShouldHaveAnHtmlView() throws IOException {
        HttpGet request = new HttpGet(serverAddress + "/fcr:sparql");
        request.addHeader("Accept", "text/html");
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        assertTrue(content.contains("SPARQL"));
        assertTrue(content.contains("PREFIX dc: <http://purl.org/dc/elements/1.1/>"));
    }

    @Test
    public void itShouldWorkWithSimpleProperties() throws IOException, RepositoryException {

        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT ?subject WHERE { ?subject dc:title \"xyz\"}";

        String content = getResponseContent(sparql);
        final ResultSet resultSet = ResultSetFactory.fromTSV(IOUtils.toInputStream(content));


        assertTrue(resultSet.hasNext());

        assertEquals("subject", resultSet.getResultVars().get(0));

        assertEquals(serverAddress + "/abc", resultSet.next().get("subject").toString());
    }

    @Test
    public void itShouldWorkWithRdfTypeMixins() throws IOException, RepositoryException {

        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT ?subject WHERE { ?subject a <http://fedora.info/definitions/v4/rest-api#resource> . ?subject dc:title \"xyz\"}";

        String content = getResponseContent(sparql);
        final ResultSet resultSet = ResultSetFactory.fromTSV(IOUtils.toInputStream(content));


        assertTrue(resultSet.hasNext());

        assertEquals("subject", resultSet.getResultVars().get(0));

        assertEquals(serverAddress + "/abc", resultSet.next().get("subject").toString());

    }

    @Test
    public void itShouldWorkWithRdfTypeProperties() throws IOException, RepositoryException {

        final String sparql = "SELECT ?subject WHERE { ?subject a <info:some-type> }";

        String content = getResponseContent(sparql);
        final ResultSet resultSet = ResultSetFactory.fromTSV(IOUtils.toInputStream(content));

        assertTrue(resultSet.hasNext());

        assertEquals("subject", resultSet.getResultVars().get(0));

        assertEquals(serverAddress + "/anobject", resultSet.next().get("subject").toString());

    }

    @Test
    public void itShouldWorkWithReferenceProperties() throws IOException, RepositoryException {

        final String sparql = "PREFIX  fedorarelsext:  <http://fedora.info/definitions/v4/rels-ext#> SELECT ?subject ?part WHERE { ?subject fedorarelsext:hasPart ?part }";

        String content = getResponseContent(sparql);
        final ResultSet resultSet = ResultSetFactory.fromTSV(IOUtils.toInputStream(content));


        assertTrue(resultSet.hasNext());

        assertEquals("subject", resultSet.getResultVars().get(0));

        final QuerySolution row = resultSet.next();
        assertEquals(serverAddress + "/abc", row.get("subject").toString());
        assertEquals(serverAddress + "/xyz", row.get("part").toString());
    }

    @Test
    public void itShouldWorkWithJoinedQueries() throws IOException, RepositoryException {

        final String sparql = "PREFIX  fedorarelsext:  <http://fedora.info/definitions/v4/rels-ext#>\n" +
                              "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n" +
                              "SELECT ?part ?collectionTitle WHERE { ?part fedorarelsext:isPartOf ?collection .\n" +
                                  "  ?collection dc:title ?collectionTitle }";

        String content = getResponseContent(sparql);
        final ResultSet resultSet = ResultSetFactory.fromTSV(IOUtils.toInputStream(content));


        assertTrue(resultSet.hasNext());

        assertEquals("part", resultSet.getResultVars().get(0));

        final QuerySolution row = resultSet.next();
        assertEquals(serverAddress + "/xyz", row.get("part").toString());
        assertEquals("xyz", row.get("collectionTitle").asLiteral().getLexicalForm());
    }

    @Test
    public void itShouldIndexCustomProperties() throws Exception {
        final String sparql = "PREFIX zz: <http://zz.com/> SELECT ?subject WHERE { ?subject zz:name \"junk\"}";

        String content = getResponseContent(sparql);

        final ResultSet resultSet = ResultSetFactory.fromTSV(IOUtils.toInputStream(content));
        assertTrue(resultSet.hasNext());
        assertEquals("subject", resultSet.getResultVars().get(0));
        assertEquals(serverAddress + "/anobject", resultSet.next().get("subject").toString());
    }

    private String getResponseContent(final String sparql) throws IOException {
        HttpPost sparqlRequest = new HttpPost(serverAddress + "/fcr:sparql");
        BasicHttpEntity entity =  new BasicHttpEntity();
        entity.setContent(IOUtils.toInputStream(sparql));
        sparqlRequest.setEntity(entity);
        HttpResponse response = client.execute(sparqlRequest);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String content = EntityUtils.toString(response.getEntity());
        logger.trace("Retrieved sparql feed:\n" + content);
        return content;
    }
}
