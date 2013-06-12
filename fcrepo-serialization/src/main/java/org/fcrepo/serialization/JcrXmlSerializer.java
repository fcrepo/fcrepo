
package org.fcrepo.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.serialization.BaseFedoraObjectSerializer;
import org.springframework.stereotype.Component;

@Component
public class JcrXmlSerializer extends BaseFedoraObjectSerializer {

    @Override
    public String getKey() {
        return "jcr/xml";
    }

    @Override
    public void serialize(final FedoraObject obj, final OutputStream out)
            throws RepositoryException, IOException {
        final Node node = obj.getNode();
        node.getSession().exportSystemView(node.getPath(), out, false, false);
    }

    @Override
    public void deserialize(final Session session, final String path, final InputStream stream)
            throws RepositoryException, IOException {

        session.importXML(path, stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

    }

}
