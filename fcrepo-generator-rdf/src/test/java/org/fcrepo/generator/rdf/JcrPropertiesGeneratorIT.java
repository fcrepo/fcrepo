package org.fcrepo.generator.rdf;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.JcrTools;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;


@ContextConfiguration({"/spring-test/repo.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class JcrPropertiesGeneratorIT {


    @Inject
    private Repository repository;

    JcrTools jcrTools = new JcrTools();

    @Test
    public void testUpdateResourceFromNode() throws Exception {

        final Session session = repository.login();

        final Node node = jcrTools.findOrCreateNode(session, "/a");

        node.addMixin("mix:title");

        node.setProperty("jcr:title", "123");
        TripleGenerator g = new JcrPropertiesGenerator();

        final Model model = ModelFactory.createDefaultModel();

        final com.hp.hpl.jena.rdf.model.Resource resource = model.createResource(node.getIdentifier());

        g.updateResourceFromNode(resource, node);

        resource.hasProperty(model.createProperty("http://www.jcp.org/jcr/1.0", "title"), "123");

    }
}
