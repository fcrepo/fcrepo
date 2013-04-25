package org.fcrepo.integration;

import org.fcrepo.Datastream;
import org.fcrepo.binary.MimeTypePolicy;
import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.value.binary.NamedHint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;

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

		Session session = repo.login();

		PolicyDecisionPoint pt = PolicyDecisionPoint.getInstance();
		pt.addPolicy(new MimeTypePolicy("image/tiff", new NamedHint("tiff-store")));

		final Node dsNode =
				datastreamService.createDatastreamNode(session,
															  "/testDatastreamPolicyNode",
															  "application/octet-stream",
															  new ByteArrayInputStream("asdf".getBytes()));

		final Node dsNodeTiff =
				datastreamService.createDatastreamNode(session,
															  "/testDatastreamPolicyNode",
															  "image/tiff",
															  new ByteArrayInputStream("1234".getBytes()));

		session.save();


	}
}
