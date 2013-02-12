package org.fcrepo.services;

import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;

import static org.modeshape.jcr.api.JcrConstants.*;


public class DatastreamService {

    private static final Logger logger = LoggerFactory
            .getLogger(DatastreamService.class);

    @Inject
    private Repository repo;

    private JcrTools jcrTools = new JcrTools();

    public Node createDatastreamNode(final Session session, final String dsPath, final MediaType contentType, final InputStream requestBodyStream) throws RepositoryException, IOException {

        final Node ds = jcrTools.findOrCreateNode(session, dsPath, NT_FILE);
        ds.addMixin("fedora:datastream");

        final Node contentNode = jcrTools.findOrCreateChild(ds, JCR_CONTENT,
                NT_RESOURCE);
        logger.debug("Created content node at path: " + contentNode.getPath());
		/*
		 * This next line of code deserves explanation. If we chose for the
		 * simpler line:
		 *
		 * Property dataProperty = contentNode.setProperty(JCR_DATA,
		 * requestBodyStream);
		 *
		 * then the JCR would not block on the stream's completion, and we would
		 * return to the requestor before the mutation to the repo had actually
		 * completed. So instead we use createBinary(requestBodyStream), because
		 * its contract specifies:
		 *
		 * "The passed InputStream is closed before this method returns either
		 * normally or because of an exception."
		 *
		 * which lets us block and not return until the job is done! The simpler
		 * code may still be useful to us for an asychronous method that we
		 * develop later.
		 */
        Property dataProperty = contentNode.setProperty(JCR_DATA, session
                .getValueFactory().createBinary(requestBodyStream));
        logger.debug("Created data property at path: " + dataProperty.getPath());

        ds.setProperty("fedora:contentType", contentType.toString());

        ds.addMixin("fedora:owned");
        ds.setProperty("fedora:ownerId", session.getUserID());

        if(!ds.hasProperty("fedora:created")) {
            ds.setProperty("fedora:created", Calendar.getInstance());
        }
        ds.setProperty("jcr:lastModified", Calendar.getInstance());

        // TODO: I guess we should also have the PID + DSID..
        ds.setProperty("dc:identifier", new String[] { ds.getIdentifier(), ds.getParent().getName() + "/" + ds.getName() });

        return ds;
    }
}
