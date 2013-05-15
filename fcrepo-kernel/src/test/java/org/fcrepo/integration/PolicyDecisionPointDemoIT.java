
package org.fcrepo.integration;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.binary.MimeTypePolicy;
import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class PolicyDecisionPointDemoIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    DatastreamService datastreamService;

    @Inject
    ObjectService objectService;

    @Test
    public void shouldDemonstratePolicyDecisionPoints() throws Exception {

        final Session session = repo.login();

        final PolicyDecisionPoint pt = new PolicyDecisionPoint();
        pt.addPolicy(new MimeTypePolicy("image/tiff", "tiff-store"));

        datastreamService.createDatastreamNode(session,
                "/testDatastreamPolicyNode", "application/octet-stream",
                new ByteArrayInputStream("asdf".getBytes()));

        datastreamService.createDatastreamNode(session,
                "/testDatastreamPolicyNode", "image/tiff",
                new ByteArrayInputStream("1234".getBytes()));

        session.save();

    }
}
