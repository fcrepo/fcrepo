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

package org.fcrepo.kernel.impl.rdf;

import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.models.FedoraResource;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Detects references to hash-URI RDF nodes and records them for possible creation of the corresponding JCR nodes.
 *
 * @author ajs6f
 */
public class HashURIDetector implements Function<Statement, Statement>, Supplier<Set<Resource>> {

    private final Set<Resource> hashNodes = new HashSet<>();

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    /**
     * @param idTranslator
     */
    public HashURIDetector(final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        this.idTranslator = idTranslator;
    }

    @Override
    public Statement apply(final Statement stmnt) {
        final Resource subject = stmnt.getSubject();
        if (subject.isURIResource() && idTranslator.inDomain(subject) && subject.getURI().contains("#")) {
            hashNodes.add(subject);
        }
        final Property predicate = stmnt.getPredicate();
        if (idTranslator.inDomain(predicate) && predicate.getURI().contains("#")) {
            hashNodes.add(predicate);
        }
        final RDFNode object = stmnt.getObject();
        if (object.isURIResource()) {
            final Resource objResource = object.asResource();
            if (idTranslator.inDomain(objResource) && objResource.getURI().contains("#")) {
                hashNodes.add(objResource);
            }
        }
        return stmnt;
    }

    @Override
    public Set<Resource> get() {
        return hashNodes;
    }

}
