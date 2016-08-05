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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiFunction;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

import org.modeshape.jcr.query.JcrQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

/**
 * 
 * @author barmintor
 *
 */
public class ReferencePropertyIterator extends AbstractIterator<Value> {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ReferencePropertyIterator.class);
    private final BiFunction<String, Value[], Value> valueTranslator;
    //private final Converter<Node, Resource> nodeToResource;
    private final RowIterator rows;
    private final Iterator<Value> values;
    /**
     * 
     * @param property
     * @param valueTranslator
     * @throws RepositoryException
     */
    public ReferencePropertyIterator(final Property property, final BiFunction<String, Value[], Value> valueTranslator)
            throws RepositoryException {
        this.valueTranslator = valueTranslator;
        /** this would just reproduce much of the overhead of value->node iteration
        this.nodeToResource = idTranslator
                .andThen(new InternalPathToNodeConverter(property.getSession())).inverse();
         */
        final Session session = property.getSession();
        final String sqlQuery = getQueryJoinOnReferee(property);

        // Obtain the query manager for the session ...
        final javax.jcr.query.QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(sqlQuery, "JCR-SQL2");
        final JcrQueryResult result = (JcrQueryResult) query.execute();
        rows = result.getRows();
        if (rows.hasNext()) {
            values = Collections.emptyIterator();
        } else {
            //TODO This should be a MODE query that only gets literals that we can execute
            //  unconditionally, to clean up mixed-range properties
            values = Iterators.forArray(property.getValues());
        }
        //LOGGER.info(result.getPlan());
    }

    @Override
    protected Value computeNext() {
        if (rows.hasNext()) {
            final Row row = rows.nextRow();
            try {
                final Value result =
                        valueTranslator.apply(row.getValue("path").getString(),row.getValues());
                return result;
            } catch (InvalidItemStateException e) {
                LOGGER.info(e.toString());
                // deleted references error if a session hasn't been saved, so skip them
                return computeNext();
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        } else {
            if (values.hasNext()) {
                return values.next();
            } else {
                return endOfData();
            }
        }
    }

    @SuppressWarnings("unused")
    private static String getQueryJoinOnReferee(final Property property) throws RepositoryException {
        // use a column from 2 selectors deliberately to prevent eager node lookups
        // not necessary if MODE lazily loaded the node in the SingleSelectorRow
        return
        "SELECT references.[jcr:path] AS path, references.[jcr:mixinTypes] AS type, " +
        "referee.[jcr:uuid] AS parent\n" +
        "FROM [mix:referenceable] AS references\n" +
        "INNER JOIN [nt:base] AS referee\n" +
        "ON referee.[" + property.getName() + "] = references.[jcr:uuid]\n" +
        "WHERE referee.[jcr:uuid] = '" + property.getParent().getIdentifier() + "'";
    }

    @SuppressWarnings("unused")
    @Deprecated
    /**
     * This is really slow. Don't do it this way.
     * @param property
     * @return
     * @throws RepositoryException
     */
    private static String getQueryJoinOnReference(final Property property) throws RepositoryException {
        // use a column from 2 selectors deliberately to prevent eager node lookups
        // not necessary if MODE lazily loaded the node in the SingleSelectorRow
        return
        "SELECT references.[jcr:path] AS path, references.[jcr:mixinTypes] AS type, " +
        "referee.[jcr:uuid] AS parent\n" +
        "FROM [nt:base] AS referee\n" +
        "INNER JOIN [nt:base] AS references\n" +
        "ON references.[jcr:uuid] = referee.[" + property.getName() + "] \n" +
        "WHERE referee.[jcr:uuid] = '" + property.getParent().getIdentifier() + "' ";
    }

    @SuppressWarnings("unused")
    private static String getQueryWithSubquery(final Property property) throws RepositoryException {
        return
        "SELECT [jcr:path] AS path, [jcr:mixinTypes] AS type\n" +
        "FROM [mix:referenceable]\n" +
        "WHERE [jcr:uuid] IN (\n" +
        "  SELECT [" + property.getName() + "]\n" +
        "  FROM [nt:base]\n" +
        "  WHERE [jcr:uuid] = '" + property.getParent().getIdentifier() + "'\n" +
        ")";
    }

    @SuppressWarnings("unused")
    private static String getQueryWithRefQuery(final Property property) throws RepositoryException {
        return
        "SELECT [jcr:path] AS path, [jcr:mixinTypes] AS type\n" +
        "FROM [mix:referenceable]\n" +
        "WHERE REFERENCE() IN (\n" +
        "  SELECT [" + property.getName() + "]\n" +
        "  FROM [nt:base]\n" +
        "  WHERE [jcr:uuid] = '" + property.getParent().getIdentifier() + "'\n" +
        ")";
    }
}
