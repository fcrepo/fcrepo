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

package org.fcrepo.kernel.impl.rdf;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author ajs6f
 *
 */
public class StatementSkolemizerTest {

    private static Model model = createDefaultModel();

    @Test
    public void statementWithNoBNodeShouldNotBeChanged() {
        final Skolemizer testStatementSkolemizer = new Skolemizer(randomURIResource());
        final Statement testStatement =
                model.createStatement(randomURIResource(), randomURIResource(), "literal value");
        assertEquals(testStatement, testStatementSkolemizer.apply(testStatement));
    }

    @Test
    public void statementWithBNodeSubjectShouldBeChanged() {
        final Resource topic = randomURIResource();
        final Skolemizer testStatementSkolemizer = new Skolemizer(topic);
        final Resource bnode = model.createResource();
        final Statement testStatement = model.createStatement(bnode, randomURIResource(), randomURIResource());
        final Statement result = testStatementSkolemizer.apply(testStatement);
        assertNotEquals(testStatement, result);
        final Resource skolem = result.getSubject();
        assertTrue(skolem.isURIResource());
        assertTrue(skolem.getURI().startsWith(topic.getURI()));
    }

    @Test
    public void statementWithBNodeObjectShouldBeChanged() {
        final Resource topic = randomURIResource();
        final Skolemizer testStatementSkolemizer = new Skolemizer(topic);
        final Resource bnode = model.createResource();
        final Statement testStatement = model.createStatement(randomURIResource(), randomURIResource(), bnode);
        final Statement result = testStatementSkolemizer.apply(testStatement);
        assertNotEquals(testStatement, result);
        final RDFNode object = result.getObject();
        assertTrue(object.isURIResource());
        final Resource skolem = object.asResource();
        assertTrue(skolem.getURI().startsWith(topic.getURI()));
    }

    private static Property randomURIResource() {
        return model.createProperty("test:/" + randomUUID());
    }
}
