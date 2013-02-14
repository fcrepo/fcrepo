package org.fcrepo.api.legacy.service;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.builder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.MediaType;

import org.fcrepo.jaxb.responses.DatastreamHistory;
import org.fcrepo.jaxb.responses.DatastreamProfile;
import org.fcrepo.jaxb.responses.ObjectDatastreams;
import org.fcrepo.jaxb.responses.ObjectProfile;
import org.fcrepo.jaxb.responses.ObjectDatastreams.Datastream;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.modeshape.common.SystemFailureException;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet.Builder;

/* Fedora Service ought to be the API to which all external services are coded too */
public class FedoraService {
	/**
	 * The JCR repository at the heart of Fedora.
	 */
	@Inject
	protected Repository repo;

	final private Logger logger = LoggerFactory
			.getLogger(FedoraService.class);

	private Session readOnlySession;

	public void loginReadOnlySession() throws LoginException,
	RepositoryException {
		readOnlySession = repo.login();
	}

	public void logoutReadOnlySession() {
		readOnlySession.logout();
	}

	public ObjectDatastreams getDatastreams(String pid)
			throws RepositoryException, IOException {

		if (!readOnlySession.nodeExists("/objects/" + pid)) {
			return null;
		}
		final ObjectDatastreams objectDatastreams = new ObjectDatastreams();
		final Builder<Datastream> datastreams = builder();

		NodeIterator i = readOnlySession.getNode("/objects/" + pid).getNodes();
		while (i.hasNext()) {
			final Node ds = i.nextNode();
			datastreams.add(new Datastream(ds.getName(), ds.getName(),
					getDSMimeType(ds)));
		}
		objectDatastreams.datastreams = datastreams.build();
		return objectDatastreams;

	}

	private String getDSMimeType(Node ds) throws ValueFormatException,
	PathNotFoundException, RepositoryException, IOException {
		final Binary b = (Binary) ds.getNode(JCR_CONTENT).getProperty(JCR_DATA)
				.getBinary();
		return b.getMimeType();
	}

	public void addDatastream(String pid, String dsid, MediaType contentType,
			InputStream requestBodyStream) throws LoginException, RepositoryException, ObjectNotFoundException, IOException {
		final Session session = repo.login();

		contentType =
				contentType != null ? contentType
						: APPLICATION_OCTET_STREAM_TYPE;
		String dspath = "/objects/" + pid + "/" + dsid;

		if (!session.nodeExists("/objects/" + pid)) {
			logger.debug("Tried to create a datastream for an object that doesn't exist, at resource path: " +
					dspath);
			throw new ObjectNotFoundException(); 
		}

		if (!session.hasPermission(dspath, "add_node")) {
			session.logout();
			throw new AccessControlException("Unable to add a node");
		}		

		if (!session.nodeExists(dspath)) {
			addDatastreamNode(pid, dspath, contentType,
					requestBodyStream, session);
		} else {
			if (session.hasPermission(dspath, "remove")) {
				session.getNode(dspath).remove();
				session.save();
				session.logout();
				addDatastreamNode(pid, dspath, contentType,
						requestBodyStream, session);

			} else {
				session.logout();
				throw new AccessControlException("Unable to replace a node");

			}
		}

	}


	public void addDatastreamNode(final String pid, final String dsPath,
			final MediaType contentType, final InputStream requestBodyStream,
			final Session session) throws RepositoryException, IOException {

		Long oldObjectSize = getObjectSize(session.getNode("/objects/" + pid));
		logger.debug("Attempting to add datastream node at path: " + dsPath);

		boolean created = session.nodeExists(dsPath);

		new DatastreamService().createDatastreamNode(session, dsPath,
				contentType.toString(), requestBodyStream);

		session.save();
		if (created) {
			/*
			 * we save before updating the repo size because the act of
			 * persisting session state creates new system-curated nodes and
			 * properties which contribute to the footprint of this resource
			 */
			updateRepositorySize(getObjectSize(session.getNode("/objects/" +
					pid)) -
					oldObjectSize, session);
			// now we save again to persist the repo size
			session.save();
		}
		session.logout();
		logger.debug("Finished adding datastream node at path: " + dsPath);
	}
	/**
	 * @param obj
	 * @return object size in bytes
	 * @throws RepositoryException
	 */
	static Long getObjectSize(Node obj) throws RepositoryException {
		return getNodePropertySize(obj) + getObjectDSSize(obj);
	}

	/**
	 * @param obj
	 * @return object's datastreams' total size in bytes
	 * @throws RepositoryException
	 */
	private static Long getObjectDSSize(Node obj) throws RepositoryException {
		Long size = 0L;
		NodeIterator i = obj.getNodes();
		while (i.hasNext()) {
			Node ds = i.nextNode();
			size = size + getNodePropertySize(ds);
			size = size + getContentSize(ds);
		}
		return size;
	}

	protected void updateRepositorySize(Long change, Session session)
			throws PathNotFoundException, RepositoryException {
		logger.debug("updateRepositorySize called with change quantity: "
				+ change);
		Property sizeProperty = session.getNode("/objects").getProperty("size");
		Long previousSize = sizeProperty.getLong();
		logger.debug("Previous repository size: " + previousSize);
		synchronized (sizeProperty) {
			sizeProperty.setValue(previousSize + change);
			session.save();
		}
		logger.debug("Current repository size: " + sizeProperty.getLong());
	}


	public void ingest(String pid) throws LoginException, RepositoryException {
		logger.debug("Attempting to ingest with pid: " + pid);

		final Session session = repo.login();

		if (!session.hasPermission("/objects/" + pid, "add_node")) {
			throw new AccessControlException("Cannot add a node");
		}
		final Node obj =
				new ObjectService().createObjectNode(session, "/objects/" +
						pid);

		session.save();
		/*
		 * we save before updating the repo size because the act of
		 * persisting session state creates new system-curated nodes and
		 * properties which contribute to the footprint of this resource
		 */
		updateRepositorySize(getObjectSize(obj), session);
		// now we save again to persist the repo size
		session.save();
		session.logout();
		logger.debug("Finished ingest with pid: " + pid);

	}

	public ObjectProfile getObject(String pid, URI index_uri) throws RepositoryException, ObjectNotFoundException {

		if (!readOnlySession.nodeExists("/objects/" + pid)) {
			throw new ObjectNotFoundException();
		}

		final Node obj = readOnlySession.getNode("/objects/" + pid);
		final ObjectProfile objectProfile = new ObjectProfile();

		objectProfile.pid = pid;
		if (obj.hasProperty(DC_TITLE)) {
			Property dcTitle = obj.getProperty(DC_TITLE);
			if (!dcTitle.isMultiple())
				objectProfile.objLabel =
				obj.getProperty(DC_TITLE).getString();
			else {
				objectProfile.objLabel =
						on('/').join(map(dcTitle.getValues(), value2string));
			}
		}
		objectProfile.objOwnerId =
				obj.getProperty("fedora:ownerId").getString();
		objectProfile.objCreateDate =
				obj.getProperty("jcr:created").getString();
		objectProfile.objLastModDate =
				obj.getProperty("jcr:lastModified").getString();
		objectProfile.objSize = getObjectSize(obj);
		objectProfile.objItemIndexViewURL = index_uri;
		objectProfile.objState = org.fcrepo.jaxb.responses.ObjectProfile.ObjectStates.A;
		objectProfile.objModels =
				map(obj.getMixinNodeTypes(), nodetype2string);
		return objectProfile;

	}

	private Function<Value, String> value2string =
			new Function<Value, String>() {

		@Override
		public String apply(Value v) {
			try {
				return v.getString();
			} catch (RepositoryException e) {
				throw new SystemFailureException(e);
			} catch (IllegalStateException e) {
				throw new SystemFailureException(e);
			}
		}
	};

	private Function<NodeType, String> nodetype2string =
			new Function<NodeType, String>() {

		@Override
		public String apply(NodeType type) {
			return type.getName();
		}
	};
	


    private static <From, To> Collection<To> map(From[] input,
            Function<From, To> f) {
        return transform(copyOf(input), f);
    }

	public void deleteObject(String pid) throws LoginException, RepositoryException {
        final Session session = repo.login();
        final Node obj = session.getNode("/objects/" + pid);
        updateRepositorySize(0L - getObjectSize(obj), session);
        deleteResource(obj);
		
	}

	public DatastreamProfile getDatastream(String pid, String dsid) throws ObjectNotFoundException, RepositoryException, IOException {
		if (!readOnlySession.nodeExists("/objects/" + pid)) {
			throw new ObjectNotFoundException();
		}

		final Node obj = readOnlySession.getNode("/objects/" + pid);

		if (!obj.hasNode(dsid)) {
			throw new ObjectNotFoundException();
		}
		final Node ds = obj.getNode(dsid);
		return getDSProfile(ds);

	}
	

	private DatastreamProfile getDSProfile(Node ds) throws RepositoryException,
	IOException {
		final DatastreamProfile dsProfile = new DatastreamProfile();
		dsProfile.dsID = ds.getName();
		dsProfile.pid = ds.getParent().getName();
		dsProfile.dsLabel = ds.getName();
		dsProfile.dsState = org.fcrepo.jaxb.responses.DatastreamProfile.DatastreamStates.A;
		dsProfile.dsMIME = getDSMimeType(ds);
		dsProfile.dsSize =
				getNodePropertySize(ds) +
				ds.getNode(JCR_CONTENT).getProperty(JCR_DATA)
				.getBinary().getSize();
		dsProfile.dsCreateDate = ds.getProperty("jcr:created").getString();
		return dsProfile;
	}

	public Map<String, Object> getDatastreamContent(String pid, String dsid) throws ObjectNotFoundException, RepositoryException {
		final String dsPath = "/objects/" + pid + "/" + dsid;

		if (!readOnlySession.nodeExists(dsPath)) {
		  throw new ObjectNotFoundException();
		}
		final Node ds = readOnlySession.getNode(dsPath);
		final String mimeType =
				ds.hasProperty("fedora:contentType") ? ds.getProperty(
						"fedora:contentType").getString()
						: "application/octet-stream";
						final InputStream responseStream =
								ds.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
								.getStream();
						HashMap<String, Object> content = new HashMap<String, Object>();
						content.put("stream".intern(), responseStream);
						content.put("mimeType".intern(), mimeType);

						return content;
	}

	public DatastreamHistory getDatastreamHistory(String pid, String dsid) throws RepositoryException, ObjectNotFoundException, IOException {
		final String dsPath = "/objects/" + pid + "/" + dsid;

		if (!readOnlySession.nodeExists(dsPath)) {
			throw new ObjectNotFoundException();
		}
		final Node ds = readOnlySession.getNode(dsPath);
		final DatastreamHistory dsHistory =
				new DatastreamHistory(singletonList(getDSProfile(ds)));
		dsHistory.dsID = dsid;
		dsHistory.pid = pid;
		return dsHistory;

	}

	public void deleteDatastream(String pid, String dsid) throws LoginException, RepositoryException, ObjectNotFoundException {
		final String dsPath = "/objects/" + pid + "/" + dsid;
		final Session session = repo.login();
		final Node ds;
		if (!session.nodeExists(dsPath)) {
			throw new ObjectNotFoundException();
		}
		ds = session.getNode(dsPath);

		updateRepositorySize(0L - getDatastreamSize(ds), session);
		
		deleteResource(ds);
	}


	private static Long getDatastreamSize(Node ds) throws ValueFormatException,
	PathNotFoundException, RepositoryException {
		return getNodePropertySize(ds) + getContentSize(ds);
	}

	private static Long getContentSize(Node ds) throws ValueFormatException,
	PathNotFoundException, RepositoryException {
		return ds.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
				.getSize();
	}
	

	private synchronized void deleteResource(final Node resource)
			throws RepositoryException {

		logger.debug("Attempting to delete resource at path: "
				+ resource.getPath());
		final Session session = resource.getSession();
		if (session.hasPermission(resource.getPath(), "remove")) {
			resource.remove();
			session.save();
			session.logout();
		} else {
			throw new AccessControlException("You do not have access to delete this resource");
		}
	}
	

	private static Long getNodePropertySize(Node node)
			throws RepositoryException {
		Long size = 0L;
		PropertyIterator i = node.getProperties();
		while (i.hasNext()) {
			Property p = i.nextProperty();
			if (p.isMultiple()) {
				for (Value v : copyOf(p.getValues())) {
					size = size + v.getBinary().getSize();
				}
			} else {
				size = size + p.getBinary().getSize();
			}
		}
		return size;
	}

	public String getObjects() throws PathNotFoundException, RepositoryException {
        Node objects = readOnlySession.getNode("/objects");
        StringBuffer nodes = new StringBuffer();

        for (NodeIterator i = objects.getNodes(); i.hasNext();) {
            Node n = i.nextNode();
            nodes.append("Name: " + n.getName() + ", Path:" + n.getPath() +
                    "\n");
        }
        return nodes.toString();
	}

	public void modify(String pid, ObjectProfile objProfile) throws LoginException, RepositoryException, ObjectNotFoundException {
        final String objPath = "/objects/" + pid;
        final Session session = repo.login();

        if (!session.nodeExists(objPath)) {
            session.logout();
            throw new ObjectNotFoundException();
        }
        final Node obj = session.getNode(objPath);
        obj.setProperty(DC_TITLE, objProfile.objLabel);
        if (objProfile.objModels != null)
            for (String model : objProfile.objModels) {
                obj.addMixin(model);
            }
        session.save();
        session.logout();		
	}

	public void modifyDatastream(String pid, String dsid,
			MediaType contentType, InputStream requestBodyStream) throws LoginException, RepositoryException, IOException {
		final Session session = repo.login();

		contentType =
				contentType != null ? contentType
						: MediaType.APPLICATION_OCTET_STREAM_TYPE;
		String dspath = "/objects/" + pid + "/" + dsid;

		if (!session.hasPermission(dspath, "add_node")) {
			session.logout();
			throw new AccessControlException("No permissions to modify node");
		}

		addDatastreamNode(pid, dspath, contentType,
				requestBodyStream, session);
	
	}

}
