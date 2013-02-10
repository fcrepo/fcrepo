package org.fcrepo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.*;

import java.io.ByteArrayInputStream;

import static java.lang.Thread.sleep;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/master.xml"})
public class TestJrubySequencerExample {

    @Inject
    private Repository repository;



    @Test
    public void testJrubySequencerJavaClass() throws RepositoryException, InterruptedException {

        Session session = repository.login();
        Node root = session.getRootNode().addNode("jruby");
        Node n = root.addNode("should-be-decorated.example");
        n.addNode("jcr:content", "nt:resource").setProperty("jcr:data", "asdf");

        session.save();
        session.logout();

        // HA!
        sleep(10000);

        session = repository.login();

        Node ni = session.getRootNode().getNode("jruby").getNode("should-be-decorated.example").getNode("reversed-content");

        assertEquals(ni.getProperty("jcr:data").getString(), "fdsa");



    }
}
