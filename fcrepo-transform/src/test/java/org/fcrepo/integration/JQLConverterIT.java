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

package org.fcrepo.integration;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.transform.sparql.JQLConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.JcrTools;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/master.xml"})
public class JQLConverterIT {


    @Inject
    Repository repo;

    private Session session;
    private GraphSubjects subjects;

    @Before
    public void setUp() throws Exception {
        session = repo.login();
        subjects = new DefaultGraphSubjects(session);
    }

    @Test
    public void testSimpleQuery() {

    }

    @Test
    public void testSimpleFilterReturningJcrSubject() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT ?subject WHERE { ?subject dc:title \"xyz\"}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[jcr:path] AS subject FROM [fedora:resource] AS [fedoraResource_subject] WHERE [fedoraResource_subject].[dc:title] = 'xyz'", testObj.getStatement());

    }

    @Test
    public void testSimpleMixinRdfTypeFilter() throws RepositoryException {

        final String sparql = "SELECT ?subject WHERE { ?subject a <http://fedora.info/definitions/v4/rest-api#datastream>}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[jcr:path] AS subject FROM [fedora:resource] AS [fedoraResource_subject] INNER JOIN [fedora:datastream] AS [ref_type_fedora_datastream] ON ISSAMENODE([fedoraResource_subject],[ref_type_fedora_datastream],'.')", testObj.getStatement());

    }

    @Test
    public void testSimplePropertyRdfTypeFilter() throws RepositoryException {

        final String sparql = "SELECT ?subject WHERE { ?subject a <http://some/other/uri>}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[jcr:path] AS subject FROM [fedora:resource] AS [fedoraResource_subject] WHERE [fedoraResource_subject].[rdf:type] = CAST('http://some/other/uri' AS URI)", testObj.getStatement());

    }

    @Test
    public void testDistinctFilterReturningJcrSubject() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT DISTINCT ?subject WHERE { ?subject dc:title \"xyz\"}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT DISTINCT [fedoraResource_subject].[jcr:path] AS subject FROM [fedora:resource] AS [fedoraResource_subject] WHERE [fedoraResource_subject].[dc:title] = 'xyz'", testObj.getStatement());
    }

    @Test
    public void testSimpleFilterReturningJcrPropertyValue() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { ?subject dc:title ?title }";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_subject] WHERE [fedoraResource_subject].[dc:title] IS NOT NULL", testObj.getStatement());
    }

    @Test
    public void testSimpleFilterReturningJcrSubjectAndPropertyValue() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT ?subject ?title WHERE { ?subject dc:title ?title }";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[jcr:path] AS subject, [fedoraResource_subject].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_subject] WHERE [fedoraResource_subject].[dc:title] IS NOT NULL", testObj.getStatement());
    }

    @Test
    public void testSimpleFilterReturningJcrSubjectAndOptionalPropertyValue() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> SELECT ?subject ?title WHERE { OPTIONAL { ?subject dc:title ?title } }";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[jcr:path] AS subject, [fedoraResource_subject].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_subject]", testObj.getStatement());
    }

    @Test
    public void testSimpleFilterReturningJcrPropertyValueWithCondition() throws RepositoryException {
        final Node orCreateNode = new JcrTools().findOrCreateNode(session, "/xyz", "nt:folder");
        orCreateNode.addMixin("fedora:resource");
        session.save();

        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/> " +
                                  "PREFIX fedorarelsext: <http://fedora.info/definitions/v4/rels-ext#>" +
                                  "SELECT ?title WHERE { ?subject dc:title ?title . ?subject fedorarelsext:isPartOf <" + subjects.getGraphSubject("/xyz") + "> }";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_subject] WHERE ([fedoraResource_subject].[dc:title] IS NOT NULL AND [fedoraResource_subject].[fedorarelsext:isPartOf] = CAST('" + orCreateNode.getIdentifier() + "' AS REFERENCE))", testObj.getStatement());
    }

    @Test
    public void testSecondOrderReturnValues() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>" +
                                  "PREFIX fedorarelsext: <http://fedora.info/definitions/v4/rels-ext#>" +
                                  "SELECT ?relatedTitle WHERE { ?subject fedorarelsext:hasPart ?part . ?part dc:title ?relatedTitle }";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_part].[dc:title] AS relatedTitle FROM [fedora:resource] AS [fedoraResource_subject] LEFT OUTER JOIN [fedora:resource] AS [fedoraResource_part] ON [fedoraResource_subject].[fedorarelsext:hasPart] = [fedoraResource_part].[jcr:uuid] WHERE ([fedoraResource_subject].[fedorarelsext:hasPart] IS NOT NULL AND [fedoraResource_part].[dc:title] IS NOT NULL)", testObj.getStatement());
    }

    @Test
    public void testJoinAndSecondOrderReturnValues() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>" +
                                  "PREFIX fedorarelsext: <http://fedora.info/definitions/v4/rels-ext#>" +
                                  "SELECT ?subject ?relatedTitle WHERE { ?subject fedorarelsext:hasPart ?part . ?part dc:title ?relatedTitle }";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_subject].[jcr:path] AS subject, [fedoraResource_part].[dc:title] AS relatedTitle FROM [fedora:resource] AS [fedoraResource_subject] LEFT OUTER JOIN [fedora:resource] AS [fedoraResource_part] ON [fedoraResource_subject].[fedorarelsext:hasPart] = [fedoraResource_part].[jcr:uuid] WHERE ([fedoraResource_subject].[fedorarelsext:hasPart] IS NOT NULL AND [fedoraResource_part].[dc:title] IS NOT NULL)", testObj.getStatement());
    }

    @Test
    public void testRegexFilter() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n" +
        "SELECT  ?title \n" +
        "WHERE   { ?x dc:title ?title\n" +
        "    FILTER regex(?title, \"^SPARQL\")\n" +
        "}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_x].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_x] WHERE ([fedoraResource_x].[dc:title] IS NOT NULL AND [fedoraResource_x].[dc:title] LIKE '^SPARQL')", testObj.getStatement());

    }

    @Test
    public void testContainsFilter() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n" +
                                  "SELECT  ?title \n" +
                                  "WHERE   { ?x dc:title ?title\n" +
                                  "    FILTER contains(?title, \"SPARQL\")\n" +
                                  "}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_x].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_x] WHERE ([fedoraResource_x].[dc:title] IS NOT NULL AND [fedoraResource_x].[dc:title] LIKE '%SPARQL%')", testObj.getStatement());

    }

    @Test
    public void testStrstartsFilter() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n" +
                                  "SELECT  ?title \n" +
                                  "WHERE   { ?x dc:title ?title\n" +
                                  "    FILTER strStarts(?title, \"SPARQL\")\n" +
                                  "}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_x].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_x] WHERE ([fedoraResource_x].[dc:title] IS NOT NULL AND [fedoraResource_x].[dc:title] LIKE 'SPARQL%')", testObj.getStatement());

    }

    @Test
    public void testStrendsFilter() throws RepositoryException {
        final String sparql = "PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n" +
                                  "SELECT  ?title \n" +
                                  "WHERE   { ?x dc:title ?title\n" +
                                  "    FILTER strEnds(?title, \"SPARQL\")\n" +
                                  "}";
        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);
        assertEquals("SELECT [fedoraResource_x].[dc:title] AS title FROM [fedora:resource] AS [fedoraResource_x] WHERE ([fedoraResource_x].[dc:title] IS NOT NULL AND [fedoraResource_x].[dc:title] LIKE '%SPARQL')", testObj.getStatement());

    }


    @Test
    public void testComplexQuery() throws RepositoryException {

        String sparql = "PREFIX  ns:  <http://libraries.ucsd.edu/ark:/20775/>"
                            + " SELECT DISTINCT ?subject ?object WHERE  {?subject ns:bb2765355h 'bf2765355h' . ?subject ns:bb3652744n ?object . FILTER regex(?object, \"r\", \"i\") .FILTER (?object >= 'abc' && ?object < 'efg' || !(?object = 'efg')) } " +
                            " ORDER BY DESC(?subject) ?object LIMIT 10 OFFSET 20";

        JQLConverter testObj  = new JQLConverter(session, subjects, sparql);

        final String statement = testObj.getStatement();

        final String namespacePrefix = session.getNamespacePrefix("http://libraries.ucsd.edu/ark:/20775/");

        final String expectedQuery = "SELECT DISTINCT [fedoraResource_subject].[jcr:path] AS subject, [fedoraResource_subject].[ns001:bb3652744n] AS object FROM [fedora:resource] AS [fedoraResource_subject] WHERE ((([fedoraResource_subject].[ns001:bb2765355h] = 'bf2765355h' AND [fedoraResource_subject].[ns001:bb3652744n] IS NOT NULL) AND [fedoraResource_subject].[ns001:bb3652744n] LIKE 'r') AND (([fedoraResource_subject].[ns001:bb3652744n] >= 'abc' AND [fedoraResource_subject].[ns001:bb3652744n] < 'efg') OR NOT ([fedoraResource_subject].[ns001:bb3652744n] = 'efg'))) ORDER BY [fedoraResource_subject].[jcr:path] DESC, [fedoraResource_subject].[ns001:bb3652744n] ASC LIMIT 10 OFFSET 20";

        assertEquals(expectedQuery.replaceAll("ns001", namespacePrefix), statement);
    }
}
