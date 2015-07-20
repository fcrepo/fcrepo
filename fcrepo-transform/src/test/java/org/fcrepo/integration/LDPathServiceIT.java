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
package org.fcrepo.integration;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.modeshape.rdf.impl.PropertiesRdfContext;
import org.fcrepo.transform.transformations.LDPathTransform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.List;

import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * <p>LDPathServiceIT class.</p>
 *
 * @author cbeer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/master.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class LDPathServiceIT {

    @Inject
    Repository repo;

    @Inject
    ContainerService containerService;
    private LDPathTransform testObj;

    @Before
    public void setUp() {

    }

    @Test
    public void shouldDoStuff() throws RepositoryException {
        final Session session = repo.login();

        final Container object = containerService.findOrCreate(session, "/testObject");
        object.getNode().setProperty("dc:title", "some-title");

        final String s = "@prefix dces : <http://purl.org/dc/elements/1.1/>\n" +
                       "@prefix fcrepo : <" + REPOSITORY_NAMESPACE + ">\n" +
                           "id      = . :: xsd:string ;\n" +
                           "title = dc:title :: xsd:string ;";
        final InputStream stringReader = new ByteArrayInputStream(s.getBytes());

        testObj = new LDPathTransform(stringReader);

        final DefaultIdentifierTranslator subjects = new DefaultIdentifierTranslator(session);
        final Resource topic = subjects.reverse().convert(object);
        final RdfStream triples = object.getTriples(subjects, PropertiesRdfContext.class)
                                        .topic(topic.asNode());
        final List<Map<String, Collection<Object>>> list = testObj.apply(triples);

        assertNotNull("Failed to retrieve results!", list);

        assertTrue("List didn't contain a result", list.size() == 1);

        final Map<String, Collection<Object>> stuff = list.get(0);

        assertTrue("Results didn't contain an identifier!", stuff.containsKey("id"));
        assertTrue("Results didn't contain a title!", stuff.containsKey("title"));

        assertEquals("Received more than one identifier!", 1, stuff.get("id").size());
        assertEquals("Got wrong subject in identifier!", subjects.toDomain("/testObject").getURI(), stuff.get(
                "id").iterator().next());
        assertEquals("Got wrong title!", "some-title", stuff.get("title").iterator().next());
    }
}
