
package org.fcrepo.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.FedoraResource;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile.DatastreamStates;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*}/fcr:versions")
public class FedoraVersions extends AbstractResource {

	@Autowired
	FedoraNodes objectsResource;


    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
    public List<Version> getVersionProfile(@PathParam("path")
    final List<PathSegment> segments) throws RepositoryException {
        final String path = toPath(segments);
        final Session session = getAuthenticatedSession();
        try {
                final FedoraResource ds =
                        nodeService.getObject(session, path);
                final Version v =
                        new Version(path, ds.getCreatedDate());
                return Arrays.asList(v);

        } finally {
            session.logout();
        }

    }

    @Path("/{id}")
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response getVersion(@PathParam("path")
    final List<PathSegment> segments, @PathParam("id")
    final String versionId) throws RepositoryException, IOException {
        final String path = toPath(segments);
        final Session session = getAuthenticatedSession();

        try {
			final FedoraResource resource = nodeService.getObject(session, path);

			if (resource.hasContent()) {
				return Response.ok(objectsResource.getDatastreamProfile(resource.getNode())).build();
			} else {
				return Response.ok(objectsResource.getObjectProfile(resource.getNode())).build();
			}
        } finally {
            session.logout();
        }

    }

    @XmlRootElement(name = "datastream-version")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Version {

        @XmlAttribute(name = "path")
        private String path;

        @XmlAttribute(name = "created")
        private Date created;

        public Version(final String path, final Date created) {
            super();
            this.path = path;
            this.created = created;
        }

        public Version() {
            super();
        }

        public Date getCreated() {
            return created;
        }

        public String getPath() {
            return path;
        }
    }
}
