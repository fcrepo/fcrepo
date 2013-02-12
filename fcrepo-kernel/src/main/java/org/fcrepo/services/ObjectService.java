package org.fcrepo.services;


import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;

import static javax.ws.rs.core.Response.created;

public class ObjectService {

    private static final Logger logger = LoggerFactory
            .getLogger(ObjectService.class);

    @Inject
    private Repository repo;

    private JcrTools jcrTools = new JcrTools();

    public Node createObjectNode(Session session, String path) throws RepositoryException {
        final Node obj = jcrTools.findOrCreateNode(session, path, "nt:folder");
        obj.addMixin("fedora:object");
        obj.addMixin("fedora:owned");
        obj.setProperty("fedora:ownerId", session.getUserID());
        obj.setProperty("jcr:lastModified", Calendar.getInstance());
        obj.setProperty("dc:identifier", new String[]{obj.getIdentifier(), obj.getName()});

        return obj;
    }


}
