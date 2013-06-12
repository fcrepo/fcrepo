
package org.fcrepo.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.springframework.stereotype.Component;

@Component
public interface FedoraObjectSerializer {

    String getKey();

    void serialize(final FedoraObject obj, final OutputStream out)
            throws RepositoryException, IOException;

    void deserialize(final Session session, final String path, final InputStream stream)
            throws IOException, RepositoryException, InvalidChecksumException;

}
