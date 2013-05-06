package org.fcrepo.api;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile.DatastreamStates;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*}/fcr:versions")
public class FedoraVersions extends AbstractResource {

	private static final Logger logger = LoggerFactory.getLogger(FedoraVersions.class);

	private DateTimeFormatter jcrDateFormat = ISODateTimeFormat.dateTime();

	@Autowired
	private DatastreamService datastreamService;

	@Autowired
	private ObjectService objectService;

	public void setDatastreamService(DatastreamService datastreamService) {
		this.datastreamService = datastreamService;
	}
	
	public void setObjectService(ObjectService objectService) {
		this.objectService = objectService;
	}

	@GET
	@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
	public List<Version> getVersionProfile(@PathParam("path") final List<PathSegment> segments) throws RepositoryException {
		final String path = toPath(segments);
		final Session session = getAuthenticatedSession();
		try {
			final Node node = session.getNode(path);

			if (node.isNodeType("nt:file")) {
				Datastream ds = datastreamService.getDatastream(session, path);
				Version v = new Version(path, ds.getDsId(), ds.getLabel(), ds.getCreatedDate());
				return Arrays.asList(v);
			}
			if (node.isNodeType("nt:folder")) {
				FedoraObject obj = objectService.getObject(session, path);
				Version v = new Version(path, obj.getName(), obj.getName(), jcrDateFormat.parseDateTime(obj.getCreated()).toDate());
				return Arrays.asList(v);
			}
		} finally {
			session.logout();
		}

		return Arrays.asList();
	}

	@Path("/{id}")
	@GET
	@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
	public Response getVersion(@PathParam("path") final List<PathSegment> segments, @PathParam("id") final String versionId)
			throws RepositoryException, IOException {
		final String path = toPath(segments);
		final Session session = getAuthenticatedSession();

		try {
			final Node node = session.getNode(path);

			if (node.isNodeType("nt:file")) {
				/* TODO: this should be moved to datastreamservice */
				Datastream ds = datastreamService.getDatastream(session, path);
				return Response.ok(getDSProfile(ds)).build();
			}

			if (node.isNodeType("nt:folder")) {
				/* TODO: this should be moved to objectservice */
				return Response.ok(getObjectProfile(objectService.getObject(session, path))).build();
			}
		} finally {
			session.logout();
		}

		return Response.status(Response.Status.NOT_FOUND).build();
	}

	private ObjectProfile getObjectProfile(FedoraObject object) throws RepositoryException {
		ObjectProfile prof = new ObjectProfile();
		prof.objCreateDate = object.getCreated();
		prof.objLabel = object.getLabel();
		prof.objLastModDate = object.getLastModified();
		prof.objSize = object.getSize();
		prof.objOwnerId = object.getOwnerId();
		prof.objModels = object.getModels();
		return prof;
	}

	/* TODO: this is a duplicate of FedoraDatatstreams.getDSProfile and should be merged into one method */
	private DatastreamProfile getDSProfile(final Datastream ds)
			throws RepositoryException, IOException {
		logger.trace("Executing getDSProfile() with node: " + ds.getDsId());
		final DatastreamProfile dsProfile = new DatastreamProfile();
		dsProfile.dsID = ds.getDsId();
		dsProfile.pid = ds.getObject().getName();
		logger.trace("Retrieved datastream " + ds.getDsId() + "'s parent: " +
				dsProfile.pid);
		dsProfile.dsLabel = ds.getLabel();
		logger.trace("Retrieved datastream " + ds.getDsId() + "'s label: " +
				ds.getLabel());
		dsProfile.dsOwnerId = ds.getOwnerId();
		dsProfile.dsChecksumType = ds.getContentDigestType();
		dsProfile.dsChecksum = ds.getContentDigest();
		dsProfile.dsState = DatastreamStates.A;
		dsProfile.dsMIME = ds.getMimeType();
		dsProfile.dsSize = ds.getSize();
		dsProfile.dsCreateDate = ds.getCreatedDate().toString();
		return dsProfile;
	}

	@XmlRootElement(name = "datastream-version")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Version {

		@XmlAttribute(name = "path")
		private String path;

		@XmlAttribute(name = "name")
		private String name;

		@XmlAttribute(name = "pid")
		private String id;

		@XmlAttribute(name = "created")
		private Date created;

		public Version(String path, String id, String name, Date created) {
			super();
			this.path = path;
			this.name = name;
			this.id = id;
			this.created = created;
		}

		private Version() {
			super();
		}

		public String getId() {
			return id;
		}

		public Date getCreated() {
			return created;
		}

		public String getName() {
			return name;
		}

		public String getPath() {
			return path;
		}
	}
}
