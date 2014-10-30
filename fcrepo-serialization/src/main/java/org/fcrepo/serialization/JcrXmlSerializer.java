/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.models.FedoraResource;
import org.springframework.stereotype.Component;

/**
 * Serialize a FedoraObject using the modeshape-provided JCR/XML format
 *
 * @author cbeer
 */
@Component
public class JcrXmlSerializer extends BaseFedoraObjectSerializer {

    @Override
    public String getKey() {
        return JCR_XML;
    }

    @Override
    public String getMediaType() {
        return "application/xml";
    }

    @Override
    /**
     * Serialize JCR/XML with options for recurse and skipBinary.
     * @param obj
     * @param out
     * @param skipBinary
     * @param recurse
     * @throws RepositoryException
     * @throws IOException
     */
    public void serialize(final FedoraResource obj,
                          final OutputStream out,
                          final boolean skipBinary,
                          final boolean recurse)
            throws RepositoryException, IOException {
        final Node node = obj.getNode();
        // jcr/xml export system view implemented for noRecurse:
        // exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
        node.getSession().exportSystemView(obj.getPath(), out, skipBinary, !recurse);
    }

    @Override
    public void deserialize(final Session session, final String path,
            final InputStream stream) throws RepositoryException, IOException {

        session.importXML(path, stream,
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

    }

}
