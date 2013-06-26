/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo;

import static org.fcrepo.utils.FedoraTypesUtils.isFedoraObject;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 *
 * @author ajs6f
 * @date Feb 21, 2013
 */
public class FedoraObject extends FedoraResource {

    static final Logger logger = getLogger(FedoraObject.class);

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraObject(final Node node) {
        super(node);
        mixinTypeSpecificCrap();
    }

    /**
     * Create or find a FedoraObject at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public FedoraObject(final Session session, final String path,
                        final String nodeType) throws RepositoryException {
        super(session, path, nodeType);
        mixinTypeSpecificCrap();
    }
    /**
     * Create or find a FedoraDatastream at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public FedoraObject(final Session session, final String path)
        throws RepositoryException {
        this(session, path, JcrConstants.NT_FOLDER);
    }


    private void mixinTypeSpecificCrap() {
        try {
            if (node.isNew() || !hasMixin(node)) {
                logger.debug("Setting {} properties on a {} node {}...",
                             FEDORA_OBJECT,
                             JcrConstants.NT_FOLDER,
                             node.getPath());
                node.addMixin(FEDORA_OBJECT);
            }
        } catch (RepositoryException e) {
            logger.warn("Could not decorate {} with {} properties: {} ",
                        JcrConstants.JCR_CONTENT, FEDORA_OBJECT, e);
        }
    }

    /**
     * @return The JCR name of the node that backs this object.
     * @throws RepositoryException
     */
    public String getName() throws RepositoryException {
        return node.getName();
    }

    /**
     * @todo Add Documentation.
     */
    public static boolean hasMixin(Node node) throws RepositoryException {
        return isFedoraObject.apply(node);
    }

}
