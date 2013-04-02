package org.fcrepo.integration.utils;

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
import java.util.Set;

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
    
    private DatastreamService datastreamService;
    
    private ObjectService objectService;
    
    private LowLevelStorageService lowLevelService;


    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Before
    public void setRepository() throws RepositoryException {

        URL config = this.getClass().getClassLoader().getResource("test_selfhealing_repository.json");
        repo = new JcrRepositoryFactory().getRepository(config.toString(), null);

        datastreamService = new DatastreamService();
        datastreamService.setRepository(repo);
        objectService = new ObjectService();
        objectService.setRepository(repo);
        lowLevelService = new LowLevelStorageService();
        lowLevelService.setRepository(repo);
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
        final Set<LowLevelCacheEntry> binaryBlobs = lowLevelService.getBinaryBlobs(node);

        Iterator<LowLevelCacheEntry> it = binaryBlobs.iterator();


        LowLevelCacheEntry entryToTamper = it.next();
        entryToTamper.storeValue(new ByteArrayInputStream("qwerty".getBytes()));
        Thread.sleep(1000);

    }

    private Collection<FixityResult> getNodeFixity(final Datastream ds) throws NoSuchAlgorithmException, RepositoryException {

        return lowLevelService.getFixity(ds.getNode(), MessageDigest.getInstance("SHA-1"), ds.getContentDigest(), ds.getContentSize());

    }

    @Test
    public void testEddiesMagicSelfHealingRepository() throws Exception {
        Session session = repo.login();

        objectService.createObjectNode(session, "testSelfHealingObject");

        datastreamService.createDatastreamNode(session,
                "/objects/testSelfHealingObject/testDatastreamNode4",
                "application/octet-stream", new ByteArrayInputStream(
                "9876543210".getBytes()), "SHA-1", "9cd656169600157ec17231dcf0613c94932efcdc");
        datastreamService.createDatastreamNode(session,
                "/objects/testSelfHealingObject/testDatastreamNode5",
                "application/octet-stream", new ByteArrayInputStream(
                "0123456789".getBytes()), "SHA-1", "87acec17cd9dcd20a716cc2cf67417b71c8a7016");

        session.save();

        Collection<FixityResult> nodeFixity;
        Collection<FixityResult> nodeFixity2;

        Thread.sleep(1000);


        Datastream ds = datastreamService.getDatastream("testSelfHealingObject", "testDatastreamNode5");


        Datastream ds2 = datastreamService.getDatastream("testSelfHealingObject", "testDatastreamNode4");

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


        lowLevelService.runFixityAndFixProblems(ds);


        nodeFixity = getNodeFixity(ds);

        fixityOk = true;
        for (FixityResult fixityResult : nodeFixity) {
            fixityOk &= fixityResult.computedChecksum.toString().equals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016");
        }

        assertTrue("Expected the fixity check to pass.", fixityOk);

    }
}
