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

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.fcrepo.FedoraObject;
import org.fcrepo.services.ObjectService;
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
    public void shouldDoStuff() throws RepositoryException, LDPathParseException {
        Session session = repo.login();

        final FedoraObject object = objectService.createObject(session, "/testObject");

        String s = "SELECT ?x ?uuid\n" +
                           "WHERE { ?x  <info:fedora/fedora-system:def/internal#uuid> ?uuid }";
        final InputStream stringReader = new ByteArrayInputStream(s.getBytes());

        testObj = new SparqlQueryTransform(stringReader);

        final QueryExecution qexec = testObj.apply(object.getPropertiesDataset());

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
