/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.transform.sparql;

import static com.hp.hpl.jena.query.QueryFactory.create;

import com.hp.hpl.jena.query.ResultSet;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.JcrRdfTools;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.QueryObjectModel;

/**
 * Convert a SPARQL query to a JCR query
 *
 * @author cabeer
 */
public class JQLConverter {
    private final JcrRdfTools jcrTools;
    private Session session;
    private IdentifierTranslator subjects;
    private com.hp.hpl.jena.query.Query query;

    /**
     *
     * @param session
     * @param subjects
     * @param sparqlQuery
     */
    public JQLConverter(final Session session, final IdentifierTranslator subjects, final String sparqlQuery ) {
        this(session, subjects, create(sparqlQuery));
    }

    /**
     *
     * @param session
     * @param subjects
     * @param query
     */
    public JQLConverter(final Session session, final IdentifierTranslator subjects,
        final com.hp.hpl.jena.query.Query query) {
        this.session = session;
        this.subjects = subjects;
        this.query = query;
        this.jcrTools = new JcrRdfTools(subjects, session);
    }

    /**
     * Execute the query and get a SPARQL result set
     * @return SPARQL result set
     * @throws RepositoryException
     */
    public ResultSet execute() throws RepositoryException {
        final QueryResult queryResult = getQuery().execute();
        return new JQLResultSet(session, subjects, queryResult);
    }

    /**
     * Get the raw JCR-SQL2 query translation
     * @return JCR-SQL2 query translation
     * @throws RepositoryException
     */
    public String getStatement() throws RepositoryException {
        return getQuery().getStatement();
    }

    private QueryObjectModel getQuery() throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final JQLQueryVisitor jqlVisitor = new JQLQueryVisitor(session, jcrTools, queryManager);
        query.visit(jqlVisitor);
        return jqlVisitor.getQuery();
    }

}
