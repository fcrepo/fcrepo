/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.RdfLiteralHelper;
import org.slf4j.Logger;

import javax.jcr.Session;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.isRelaxed;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Mike Durbin
 */
public class FilteringJcrPropertyStatementListener extends JcrPropertyStatementListener {

    private static final Logger LOGGER = getLogger(FilteringJcrPropertyStatementListener.class);

    private List<Statement> filteredAddStatements;

    /**
     * Construct a statement listener within the given session that filters out changes to
     * any relaxed server managed triples.
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param topic the topic of the RDF statement
     */
    public FilteringJcrPropertyStatementListener(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                                 final Session session, final Node topic) {
        super(idTranslator, session, topic);
        filteredAddStatements = new ArrayList<Statement>();
    }

    @Override
    public void addedStatement(final Statement input) {
        if (isRelaxed.test(input.getPredicate())) {
            filteredAddStatements.add(input);
        } else {
            super.addedStatement(input);
        }
    }

    @Override
    public void removedStatement(final Statement input) {
        if (!isRelaxed.test(input.getPredicate())) {
            super.removedStatement(input);
        }
    }

    /**
     * Gets the created date (if any) that was specified to be applied as part of the statments
     * made to the model to which this StatementListener is listening.
     * @return the date that should be set for the CREATED_DATE or null if it should be
     *         untouched
     */
    public Calendar getAddedCreatedDate() {
        Calendar cal = null;
        for (Statement added : filteredAddStatements) {
            if (added.getPredicate().equals(CREATED_DATE)) {
                if (cal == null) {
                    cal = RdfLiteralHelper.parseExpectedXsdDateTimeValue(added.getObject());
                } else {
                    throw new MalformedRdfException(CREATED_DATE + " may only appear once!");
                }
            }
        }
        return cal;
    }

    /**
     * Gets the created by user (if any) that was specified to be applied as part of the statments
     * made to the model to which this StatementListener is listening.
     * @return the date that should be set for the CREATED_BY or null if it should be
     *         untouched
     */
    public String getAddedCreatedBy() {
        String username = null;
        for (Statement added : filteredAddStatements) {
            if (added.getPredicate().equals(CREATED_BY)) {
                if (username == null) {
                    username = added.getObject().asLiteral().getString();
                } else {
                    throw new MalformedRdfException(CREATED_BY + " may only appear once!");
                }
            }
        }
        return username;
    }

    /**
     * Gets the modified date (if any) that was specified to be applied as part of the statments
     * made to the model to which this StatementListener is listening.
     * @return the date that should be set for the LAST_MODIFIED_DATE or null if it should be
     *         untouched
     */
    public Calendar getAddedModifiedDate() {
        Calendar cal = null;
        for (Statement added : filteredAddStatements) {
            if (added.getPredicate().equals(LAST_MODIFIED_DATE)) {
                if (cal == null) {
                    cal = RdfLiteralHelper.parseExpectedXsdDateTimeValue(added.getObject());
                } else {
                    throw new MalformedRdfException(LAST_MODIFIED_DATE + " may only appear once!");
                }
            }
        }
        return cal;
    }

    /**
     * Gets the modified by user (if any) that was specified to be applied as part of the statments
     * made to the model to which this StatementListener is listening.
     * @return the date that should be set for the MODIFIED_BY or null if it should be
     *         untouched
     */
    public String getAddedModifiedBy() {
        String username = null;
        for (Statement added : filteredAddStatements) {
            if (added.getPredicate().equals(LAST_MODIFIED_BY)) {
                if (username == null) {
                    username = added.getObject().asLiteral().getString();
                } else {
                    throw new MalformedRdfException(LAST_MODIFIED_BY + " may only appear once!");
                }
            }
        }
        return username;
    }
}
