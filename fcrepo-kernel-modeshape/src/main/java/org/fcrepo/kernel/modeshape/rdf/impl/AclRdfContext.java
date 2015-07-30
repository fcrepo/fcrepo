/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;

import javax.jcr.RepositoryException;
import java.security.AccessControlException;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDboolean;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;

/**
 * @author cabeer
 * @since 10/1/14
 */
public class AclRdfContext extends NodeRdfContext {
    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the property of idTranslator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public AclRdfContext(final FedoraResource resource,
                         final IdentifierConverter<Resource, FedoraResource> idTranslator) throws RepositoryException {
        super(resource, idTranslator);

        boolean writable = false;
        try {
            resource().getNode().getSession().checkPermission( resource().getPath(), "add_node,set_property,remove" );
            writable = true;
        } catch ( final AccessControlException ex ) {
            writable = false;
        }
        concat(create(subject(), WRITABLE.asNode(), createLiteral(String.valueOf(writable), XSDboolean)));
    }
}
