package org.fcrepo.modeshape;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.DiscreteDomains.integers;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Ranges.closed;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.fcrepo.modeshape.jaxb.responses.NextPid;

import com.google.common.base.Function;

/**
 * JAX-RS Resource offering PID creation.
 * 
 * @author ajs6f
 * 
 */
@Path("nextPID")
public class FedoraIdentifiers extends AbstractResource {

	/**
	 * @param numPids
	 * @return HTTP 200 with XML-formatted block of PIDs
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws TemplateException
	 */
	@POST
	@Produces({ TEXT_XML, APPLICATION_JSON })
	public NextPid getNextPid(
			@QueryParam("numPids") @DefaultValue("1") Integer numPids)
			throws RepositoryException, IOException {

		return new NextPid(copyOf(transform(closed(1, numPids)
				.asSet(integers()), makePid)));

	}

	private Function<Integer, String> makePid = new Function<Integer, String>() {
		@Override
		public String apply(Integer slot) {
			return pidMinter.mintPid();
		}
	};
}
