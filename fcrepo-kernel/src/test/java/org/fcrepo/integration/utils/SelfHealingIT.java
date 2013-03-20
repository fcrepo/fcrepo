package org.fcrepo.integration.utils;

import static org.fcrepo.services.DatastreamService.createDatastreamNode;
import static org.fcrepo.services.DatastreamService.getDatastream;
import static org.fcrepo.services.LowLevelStorageService.getBinaryBlobs;
import static org.fcrepo.services.LowLevelStorageService.getFixity;
import static org.fcrepo.services.ObjectService.createObjectNode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SelfHealingIT {
    protected Logger logger;
    static private Repository repo;


    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Before
    public void setRepository() throws RepositoryException {

        URL config = this.getClass().getClassLoader().getResource("test_selfhealing_repository.json");
        repo = new JcrRepositoryFactory().getRepository(config.toString(), null);

        new DatastreamService().setRepository(repo);
        new ObjectService().setRepository(repo);
        new LowLevelStorageService().setRepository(repo);
        setupInitialNodes();
    }

    private void setupInitialNodes() throws RepositoryException {
        Session s = repo.login();
        final Node objectStore = new JcrTools(true).findOrCreateNode(s, "/objects");

        if(objectStore.canAddMixin("fedora:objectStore")) {
            objectStore.addMixin("fedora:objectStore");

            if(!objectStore.hasProperty("fedora:size")) {
                objectStore.setProperty("fedora:size", 0L);
            }
        }

        s.save();
        s.logout();
    }

    private void tamperWithNode(final Node node) throws Exception {


        logger.info("Tampering with node " + node.toString());
        final Map<LowLevelCacheEntry,InputStream> binaryBlobs = getBinaryBlobs(node);

        Iterator<LowLevelCacheEntry> it = binaryBlobs.keySet().iterator();


        LowLevelCacheEntry entryToTamper = it.next();
        entryToTamper.storeValue(new ByteArrayInputStream("qwerty".getBytes()));
        Thread.sleep(1000);

    }

    private Collection<FixityResult> getNodeFixity(final Datastream ds) throws NoSuchAlgorithmException, RepositoryException {

        return getFixity(ds.getNode(), MessageDigest.getInstance("SHA-1"), ds.getContentDigest(), ds.getContentSize());

    }

   // @Ignore("doesn't play nice with other tests")
    @Test
    public void testEddiesMagicSelfHealingRepository() throws Exception {
        Session session = repo.login();

        createObjectNode(session, "testObjectzzz");

        createDatastreamNode(session,
                "/objects/testObjectzzz/testDatastreamNode4",
                "application/octet-stream", new ByteArrayInputStream(
                "9876543210".getBytes()), "SHA-1", "9cd656169600157ec17231dcf0613c94932efcdc");
        createDatastreamNode(session,
                "/objects/testObjectzzz/testDatastreamNode5",
                "application/octet-stream", new ByteArrayInputStream(
                "0123456789".getBytes()), "SHA-1", "87acec17cd9dcd20a716cc2cf67417b71c8a7016");

        session.save();

        Collection<FixityResult> nodeFixity;
        Collection<FixityResult> nodeFixity2;

        Thread.sleep(1000);


        Datastream ds = getDatastream("testObjectzzz", "testDatastreamNode5");


        Datastream ds2 = getDatastream("testObjectzzz", "testDatastreamNode4");

        logger.info("checking that our setup succeeded");
        nodeFixity = getNodeFixity(ds);

        nodeFixity2 = getNodeFixity(ds2);

        assertNotEquals(0, nodeFixity.size());


        logger.info("ds1");
        boolean fixityOk = true;

        for (FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.toString().equals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016");
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);

        logger.info("ds2");

        fixityOk = true;

        for (FixityResult fixityResult : nodeFixity2) {
            fixityOk &= fixityResult.computedChecksum.toString().equals("urn:sha1:9cd656169600157ec17231dcf0613c94932efcdc");
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);


        tamperWithNode(ds.getNode());


        nodeFixity = getNodeFixity(ds);

        fixityOk = true;
        for (FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.toString().equals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016");
        }

        assertFalse("Expected the fixity check to fail.", fixityOk);


        ds.runFixityAndFixProblems();


        nodeFixity = getNodeFixity(ds);

        fixityOk = true;
        for (FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.toString().equals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016");
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);

    }
}
