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
package org.fcrepo.integration;

import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.transform.transformations.SparqlQueryTransform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;


/**
 * <p>SparqlQueryTransformIT class.</p>
 *
 * @author cbeer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/master.xml"})
public class SparqlQueryTransformIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;
    private SparqlQueryTransform testObj;

    @Before
    public void setUp() {

    }

    @Test
    public void shouldDoStuff() throws RepositoryException {
        final Session session = repo.login();

        final FedoraObject object = objectService.createObject(session, "/testObject");

        final String s = "SELECT ?x ?uuid\n" +
                "WHERE { ?x  <" + REPOSITORY_NAMESPACE + "uuid> ?uuid }";
        final InputStream stringReader = new ByteArrayInputStream(s.getBytes());

        testObj = new SparqlQueryTransform(stringReader);

        final QueryExecution qexec = testObj.apply(object.getPropertiesDataset(new DefaultIdentifierTranslator()));

        assert(qexec != null);

        try {
            final ResultSet results = qexec.execSelect();

            assert(results != null);
            assertTrue(results.hasNext());
        } finally {
            qexec.close();
        }
    }
}
