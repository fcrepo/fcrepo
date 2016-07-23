/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;

import javax.jcr.RepositoryException;
import java.security.AccessControlException;

import static java.util.stream.Stream.of;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDboolean;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.Triple.create;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;

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
            getJcrNode(resource()).getSession().checkPermission( resource().getPath(), "add_node,set_property,remove" );
            writable = true;
        } catch ( final AccessControlException ex ) {
            writable = false;
        }
        concat(of(create(subject(), WRITABLE.asNode(), createLiteral(String.valueOf(writable), XSDboolean))));
    }
}
