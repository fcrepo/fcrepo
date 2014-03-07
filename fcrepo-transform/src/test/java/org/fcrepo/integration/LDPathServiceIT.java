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

import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.transform.transformations.LDPathTransform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/master.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class LDPathServiceIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;
    private LDPathTransform testObj;

    @Before
    public void setUp() {

    }

    @Test
    public void shouldDoStuff() throws RepositoryException {
        final Session session = repo.login();

        final FedoraObject object = objectService.createObject(session, "/testObject");
        object.getNode().setProperty("dc:title", "some-title");

        final String s = "@prefix dces : <http://purl.org/dc/elements/1.1/>\n" +
                       "@prefix fcrepo : <" + REPOSITORY_NAMESPACE + ">\n" +
                           "id      = . :: xsd:string ;\n" +
                           "title = dc:title :: xsd:string ;\n" +
                           "uuid = fcrepo:uuid :: xsd:string ;";
        final InputStream stringReader = new ByteArrayInputStream(s.getBytes());

        testObj = new LDPathTransform(stringReader);

        final DefaultGraphSubjects subjects = new DefaultGraphSubjects(session);
        final List<Map<String, Collection<Object>>> list = testObj.apply(object.getPropertiesDataset(subjects));

        assert(list != null);
        assertEquals(1, list.size());
        final Map<String, Collection<Object>> stuff = list.get(0);

        assertTrue(stuff.containsKey("id"));
        assertTrue(stuff.containsKey("title"));

        assertEquals(1, stuff.get("id").size());
        assertEquals(subjects.getGraphSubject("/testObject").getURI(),
                stuff.get("id").iterator().next());
        assertEquals("some-title", stuff.get("title").iterator().next());
        assertEquals(object.getNode().getIdentifier(), stuff.get("uuid").iterator().next());
    }
}
