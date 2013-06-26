
package org.fcrepo.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseFedoraObjectSerializer implements
        FedoraObjectSerializer {

    @Autowired
    protected Repository repo;

    @Autowired
    protected ObjectService objService;

    @Autowired
    protected DatastreamService dsService;

    @Override
    public abstract void serialize(final FedoraObject obj,
            final OutputStream out) throws RepositoryException, IOException;

    @Override
    public abstract void deserialize(final Session session, final String path,
            final InputStream stream) throws IOException, RepositoryException,
        InvalidChecksumException;

}
