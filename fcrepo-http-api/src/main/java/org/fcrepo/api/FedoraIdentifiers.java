
package org.fcrepo.api;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ContiguousSet.create;
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Range.closed;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.yammer.metrics.annotation.Timed;
import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.management.NextPid;
import org.springframework.stereotype.Component;

/**
 * JAX-RS Resource offering PID creation.
 * 
 * @author ajs6f
 * 
 */
@Component
@Path("/rest/nextPID")
public class FedoraIdentifiers extends AbstractResource {

    /**
     * @param numPids
     * @return HTTP 200 with block of PIDs
     * @throws RepositoryException
     * @throws IOException
     * @throws TemplateException
     */
    @POST
	@Timed
    @Produces({TEXT_XML, APPLICATION_JSON})
    public NextPid getNextPid(@QueryParam("numPids")
    @DefaultValue("1")
    final Integer numPids) {

        return new NextPid(copyOf(transform(create(closed(1, numPids),
                integers()), pidMinter.makePid())));

    }

}
